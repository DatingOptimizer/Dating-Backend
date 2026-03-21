# Quick Start Guide

## Prerequisites

- Java 21 (recommended) or Java 17+
- Maven 3.9+
- Supabase account + project (free tier works)
- Claude API Key from Anthropic

## Setup

### 1. Clone and Configure

```bash
git clone <repo-url>
cd dating
```

Create a `.env` file in the project root:

```bash
# Claude API
CLAUDE_API_KEY=sk-ant-your-api-key-here

# CORS (for React frontend)
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Supabase PostgreSQL (JDBC format)
SUPABASE_DB_JDBC_URL=jdbc:postgresql://db.your-project-id.supabase.co:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=your-db-password

# Supabase Auth (Settings -> API -> JWT Secret)
SUPABASE_JWT_SECRET=your-jwt-secret
```

### 2. Create the `saved_items` Table

Run in **Supabase SQL Editor**:

```sql
CREATE TABLE IF NOT EXISTS saved_items (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    TEXT NOT NULL,
  type       TEXT NOT NULL CHECK (type IN ('BIO', 'STARTER')),
  content    TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saved_items_user_type
  ON saved_items (user_id, type, created_at DESC);
```

The `profile_requests` table is created automatically by Hibernate on first run.

### 3. Run the Application

```bash
# Clean and compile
./mvnw clean compile

# Run
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### 4. Verify

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "Dating Profile Optimizer API",
  "timestamp": "2026-03-20T10:00:00"
}
```

## Project Structure

```
src/main/java/com/groupf/dating/
├── common/              # Constants, enums (ToneType, ApiConstants)
├── config/              # CORS, Security, RestClient, Jackson, Retry
├── controller/          # REST API endpoints
├── dto/                 # Request/Response DTOs
├── exception/           # ErrorCode, ClaudeApiException, GlobalExceptionHandler
├── model/               # JPA entities (ProfileOptimizationRequest, SavedItem)
├── repository/          # JPA repositories
├── service/             # Service interfaces
│   └── impl/            # Service implementations
├── util/                # PromptBuilder, ImageUtil, StringListConverter
└── DatingApplication.java
```

## API Endpoints

All protected endpoints require: `Authorization: Bearer <supabase-jwt-token>`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check (public) |
| POST | `/api/profile/rewrite-bio` | Rewrite dating bio with AI |
| POST | `/api/profile/generate-openers` | Generate conversation starters |
| POST | `/api/profile/rank-photos` | Rank photos using Claude Vision |
| GET | `/api/profile/history` | Get saved bios and starters |
| POST | `/api/profile/history/bio` | Save a bio |
| DELETE | `/api/profile/history/bio/{id}` | Delete a saved bio |
| POST | `/api/profile/history/starter` | Save a starter |
| DELETE | `/api/profile/history/starter/{id}` | Delete a saved starter |

## Testing

```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BioServiceImplTest

# View coverage report
open target/site/jacoco/index.html
```

## Common Issues

### Supabase Connection Error
- Ensure JDBC URL format: `jdbc:postgresql://...` (not `postgresql://...`)
- Append `?sslmode=require`
- Check DB password for unescaped special characters
- Verify Supabase project is not paused

### Port 8080 Already in Use
```bash
lsof -i :8080
kill -9 <PID>
```

### Claude API Key Missing
Add `CLAUDE_API_KEY` to your `.env` file or set it as an environment variable.

## Deployment

The backend is deployed on Render (Docker): https://dating-optimizer-backend.onrender.com

Frontend is on Netlify: https://dating-optimizer.netlify.app

See [README.md](README.md) for full deployment instructions.
