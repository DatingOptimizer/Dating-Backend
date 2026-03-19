# AI Dating Profile Optimizer - Backend

CS 5500 Group 4: Yihan Wang, Kaichen Qu, Xujing Hui

## Overview

Spring Boot REST API backend for the AI Dating Profile Optimizer application. This service uses Claude AI API to optimize dating profiles by rewriting bios, ranking photos, and generating conversation starters.

## Tech Stack

- **Backend Framework**: Spring Boot 4.0.2
- **Language**: Java 21
- **Database**: PostgreSQL (Supabase)
- **Auth**: Supabase Auth + Spring Security (JWT/HS256)
- **AI**: Claude API (Anthropic)
- **Build Tool**: Maven

## Project Structure

```
src/main/java/com/groupf/dating/
├── config/              # Configuration classes (CORS, Security, DataSource)
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects (request/response)
├── model/               # JPA entities
├── repository/          # JPA repositories
├── service/             # Business logic
├── util/                # Helpers (PromptBuilder, StringListConverter, ImageUtil)
└── DatingApplication.java
```

## Prerequisites

- Java 21 (recommended) or Java 17+
- Maven 3.9+
- Supabase account + project (free tier works)
- Claude API Key from Anthropic

## Setup Instructions

### 1. Create a Supabase Project

1. Sign up at [supabase.com](https://supabase.com) and create a new project
2. Go to **Settings → Database** to find your connection string
3. Go to **Settings → API** to find your JWT secret and anon key
4. The `profile_requests` table is created automatically by Hibernate on first run

### 2. Get Claude API Key

1. Sign up at [console.anthropic.com](https://console.anthropic.com/)
2. Navigate to API Keys section
3. Create a new API key
4. Copy the key (starts with `sk-ant-...`)

### 3. Configure Environment Variables

Create a `.env` file in the project root:

```bash
# Claude API
CLAUDE_API_KEY=sk-ant-your-api-key-here

# CORS (for React frontend)
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Supabase PostgreSQL — original URI (for reference)
SUPABASE_DB_URL=postgresql://postgres:[password]@db.your-project-id.supabase.co:5432/postgres

# Spring Boot JDBC format (derived from above)
SUPABASE_DB_JDBC_URL=jdbc:postgresql://db.your-project-id.supabase.co:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=your-db-password

# Supabase Auth (Settings → API → JWT Secret)
SUPABASE_JWT_SECRET=your-jwt-secret
```

### 4. Run the Application

```bash
# Clean and compile
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Or package and run
./mvnw clean package
java -jar target/dating-0.0.1-SNAPSHOT.jar
```

The API will start on `http://localhost:8080`

## API Endpoints

### Health Check
```http
GET /api/health
```

Response:
```json
{
  "status": "UP",
  "service": "Dating Profile Optimizer API",
  "timestamp": "2026-02-10T18:30:00"
}
```

### Bio Rewriting
```http
POST /api/profile/rewrite-bio
Content-Type: application/json

{
  "bio": "I like traveling and pizza. Looking for someone fun.",
  "tone": "casual"
}
```

Valid tone values: `casual`, `bold`, `polite`, `humorous`, `warm`

All protected endpoints require: `Authorization: Bearer <supabase-jwt-token>`

Response:
```json
{
  "originalBio": "I like traveling and pizza. Looking for someone fun.",
  "rewrittenBios": [
    "Adventure seeker with a passport full of stamps...",
    "Globe-trotter who believes the best conversations...",
    "Travel enthusiast and pizza connoisseur..."
  ],
  "tone": "casual"
}
```

### Generate Conversation Starters
```http
POST /api/profile/generate-openers
Content-Type: application/json

{
  "bio": "I love hiking and photography",
  "tone": "polite"
}
```

Valid tone values: `casual`, `bold`, `polite`, `humorous`, `warm`

Response:
```json
{
  "bio": "I love hiking and photography",
  "starters": [
    "I noticed you're into hiking too, what's been your favorite trail?",
    "Your photography looks amazing! What camera do you use?",
    "Fellow outdoor enthusiast here! Any hidden gem trails to recommend?"
  ],
  "tone": "polite"
}
```

### Photo Ranking
```http
POST /api/profile/rank-photos
Content-Type: multipart/form-data

photos: <file1>, <file2>, ...
```

## Development Workflow

### Testing Endpoints

**Using curl:**
```bash
# Health check
curl http://localhost:8080/api/health

# Bio rewrite
curl -X POST http://localhost:8080/api/profile/rewrite-bio \
  -H "Content-Type: application/json" \
  -d '{"bio":"I like pizza","tone":"casual"}'
```

**Using Postman:**
1. Import the API endpoints
2. Set `Content-Type: application/json`
3. Test each endpoint

### Running Tests
```bash
./mvnw test
```

### Hot Reload
The application supports hot reload during development. Just rebuild:
```bash
./mvnw compile
```

## Database Schema (PostgreSQL / Supabase)

### Table: `profile_requests`

Auto-created by Hibernate on first startup.

| Column | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Auto-generated |
| `user_id` | VARCHAR | Supabase auth user UUID |
| `original_bio` | TEXT | User's original bio |
| `tone_preference` | VARCHAR | casual / bold / polite / humorous / warm |
| `rewritten_bios` | TEXT | JSON array of rewritten bios |
| `conversation_starters` | TEXT | JSON array of starters |
| `ranked_photos` | TEXT | JSON array of photo rankings |
| `created_at` | TIMESTAMP | Record creation time |

Registered users are managed automatically by Supabase in the built-in `auth.users` table. View them in **Supabase Dashboard → Authentication → Users**.

## Next Steps (Week by Week)

### ✅ Week 1-2: Project Setup (DONE)
- [x] Fix Spring Boot configuration
- [x] Set up Supabase PostgreSQL connection
- [x] Create basic REST API structure
- [x] Add CORS configuration for React frontend

### ✅ Week 3-4: Core Features (DONE)
- [x] Implement Claude API integration service
- [x] Complete bio rewriting with multiple tones (casual, bold, polite, humorous, warm)
- [x] Add conversation starter generation
- [ ] Implement basic content moderation

### ✅ Week 5-6: Photo Ranking & Integration (DONE)
- [x] Implement photo ranking with Claude Vision API
- [x] Add file upload handling
- [x] Connect with React frontend
- [x] Add error handling and validation

### 📝 Week 7-8: Testing & Deployment
- [ ] Write unit and integration tests
- [ ] Deploy to Heroku
- [ ] Configure Supabase production environment
- [ ] Performance testing

## Deployment to Heroku

```bash
# Login to Heroku
heroku login

# Create Heroku app
heroku create dating-optimizer-api

# Set environment variables
heroku config:set CLAUDE_API_KEY=sk-ant-your-key
heroku config:set ALLOWED_ORIGINS=https://your-frontend.netlify.app
heroku config:set SUPABASE_DB_JDBC_URL=jdbc:postgresql://db.your-project-id.supabase.co:5432/postgres?sslmode=require
heroku config:set SUPABASE_DB_USERNAME=postgres
heroku config:set SUPABASE_DB_PASSWORD=your-db-password
heroku config:set SUPABASE_JWT_SECRET=your-jwt-secret

# Deploy
git push heroku main
```

## Troubleshooting

### Supabase Connection Issues
- Ensure `SUPABASE_DB_JDBC_URL` uses JDBC format: `jdbc:postgresql://...` not `postgresql://...`
- Ensure `?sslmode=require` is appended to the JDBC URL
- Check your DB password doesn't have unescaped special characters
- Verify the Supabase project is not paused (free tier pauses after inactivity)

### Port Already in Use
```bash
# Find process on port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in application.yaml
server.port=8081
```

### Java Version Issues
```bash
# Check Java version
java -version

# Should be Java 21 or 17+
# Install if needed
brew install openjdk@21
```

## Team Resources

- **API Documentation**: Update as you add endpoints
- **Postman Collection**: Create shared collection for testing (add `Authorization: Bearer <token>` header)
- **Supabase Dashboard**: View users (Authentication → Users) and data (Table Editor)
- **Heroku Dashboard**: Monitor deployment and logs

## License

MIT License - CS 5500 Academic Project
