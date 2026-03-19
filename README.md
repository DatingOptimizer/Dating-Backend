# AI Dating Profile Optimizer - Backend

CS 5500 Group 4: Yihan Wang, Kaichen Qu, Xujing Hui

## Overview

Spring Boot REST API backend for the AI Dating Profile Optimizer application. This service uses Claude AI API to optimize dating profiles by rewriting bios, ranking photos, and generating conversation starters.

## Tech Stack

- **Backend Framework**: Spring Boot 4.0.2
- **Language**: Java 21
- **Database**: MongoDB
- **AI**: Claude API (Anthropic)
- **Build Tool**: Maven

## Project Structure

```
src/main/java/com/groupf/dating/
├── config/              # Configuration classes (CORS, etc.)
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects (request/response)
├── model/               # MongoDB entities
├── repository/          # MongoDB repositories
├── service/             # Business logic
└── DatingApplication.java
```

## Prerequisites

- Java 21 (recommended) or Java 17+
- Maven 3.9+
- MongoDB (local or MongoDB Atlas)
- Claude API Key from Anthropic

## Setup Instructions

### 1. Install MongoDB

**Option A: Local MongoDB**
```bash
# macOS
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Verify MongoDB is running
mongosh
```

**Option B: MongoDB Atlas (Cloud)**
- Sign up at [mongodb.com/atlas](https://www.mongodb.com/atlas)
- Create a free cluster
- Get your connection string

### 2. Get Claude API Key

1. Sign up at [console.anthropic.com](https://console.anthropic.com/)
2. Navigate to API Keys section
3. Create a new API key
4. Copy the key (starts with `sk-ant-...`)

### 3. Configure Environment Variables

Create a `.env` file in the project root (or set environment variables):

```bash
# MongoDB Connection
MONGODB_URI=mongodb://localhost:27017/dating-optimizer

# Claude API
CLAUDE_API_KEY=sk-ant-your-api-key-here

# CORS (for React frontend)
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

**For local development**, you can also edit `src/main/resources/application.yaml` directly.

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

```json
```

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

## MongoDB Database Schema

### Collection: `profile_requests`

```javascript
{
  "_id": "ObjectId",
  "userId": "string",
  "originalBio": "string",
  "photoUrls": ["string"],
  "tonePreference": "casual|bold|polite|humorous|warm",
  "createdAt": "ISODate",
  "updatedAt": "ISODate",
  "rewrittenBios": ["string"],
  "rankedPhotos": [
    {
      "photoUrl": "string",
      "rank": "number",
      "score": "number",
      "reasoning": "string"
    }
  ],
  "conversationStarters": ["string"]
}
```

## Next Steps (Week by Week)

### ✅ Week 1-2: Project Setup (DONE)
- [x] Fix Spring Boot configuration
- [x] Set up MongoDB connection
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
- [ ] Configure MongoDB Atlas for production
- [ ] Performance testing

## Deployment to Heroku

```bash
# Login to Heroku
heroku login

# Create Heroku app
heroku create dating-optimizer-api

# Add MongoDB addon (or use MongoDB Atlas)
heroku addons:create mongocloud:free

# Set environment variables
heroku config:set CLAUDE_API_KEY=sk-ant-your-key
heroku config:set ALLOWED_ORIGINS=https://your-frontend.netlify.app

# Deploy
git push heroku main
```

## Troubleshooting

### MongoDB Connection Issues
```bash
# Check if MongoDB is running
brew services list | grep mongodb

# Start MongoDB
brew services start mongodb-community

# Check connection
mongosh mongodb://localhost:27017/dating-optimizer
```

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
- **Postman Collection**: Create shared collection for testing
- **MongoDB Compass**: Use for database visualization
- **Heroku Dashboard**: Monitor deployment and logs

## License

MIT License - CS 5500 Academic Project
