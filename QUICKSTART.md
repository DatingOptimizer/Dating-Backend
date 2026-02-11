# Quick Start Guide

## What Was Fixed

### ❌ Original Issues:
1. **Java 25** (early access) → Changed to **Java 21 LTS**
2. **JPA dependency** (needs SQL database) → **Removed** (you're using MongoDB)
3. **Both WebFlux and WebMVC** (conflict) → **Kept WebMVC** for REST APIs, WebFlux for HTTP client
4. **Invalid test dependencies** → **Fixed** to use `spring-boot-starter-test`
5. **Missing datasource** (caused startup failure) → **Configured MongoDB**

### ✅ What's Now Working:
- ✅ Project compiles successfully
- ✅ MongoDB configuration ready
- ✅ CORS configured for React frontend
- ✅ Basic REST API structure
- ✅ Placeholder endpoints for your features

## Run the Application

### Option 1: Without MongoDB (Testing Mode)

To test if the application starts (will fail when trying to connect to MongoDB, but validates the setup):

```bash
./mvnw spring-boot:run
```

You'll see MongoDB connection errors, but the REST API structure is ready.

### Option 2: With MongoDB (Full Mode)

1. **Install and start MongoDB:**
```bash
# macOS
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Verify it's running
mongosh
```

2. **Run the application:**
```bash
./mvnw spring-boot:run
```

3. **Test the health endpoint:**
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "Dating Profile Optimizer API",
  "timestamp": "2026-02-10T18:30:00"
}
```

## Project Structure

```
dating/
├── src/main/java/com/groupf/dating/
│   ├── DatingApplication.java          # Main application
│   ├── config/
│   │   └── CorsConfig.java            # CORS for React frontend
│   ├── controller/
│   │   ├── HealthController.java       # Health check endpoint
│   │   └── ProfileOptimizationController.java  # Main API endpoints
│   ├── dto/                           # Request/Response objects
│   │   ├── BioRewriteRequest.java
│   │   └── BioRewriteResponse.java
│   ├── model/
│   │   └── ProfileOptimizationRequest.java  # MongoDB entity
│   ├── repository/
│   │   └── ProfileRequestRepository.java    # MongoDB repository
│   └── service/                       # Business logic (TODO)
├── src/main/resources/
│   └── application.yaml               # Configuration
└── pom.xml                            # Dependencies
```

## Available API Endpoints

### 1. Health Check
```bash
GET http://localhost:8080/api/health
```

### 2. Bio Rewriting (Placeholder)
```bash
POST http://localhost:8080/api/profile/rewrite-bio
Content-Type: application/json

{
  "bio": "I like traveling and pizza. Looking for someone fun.",
  "tone": "casual"
}
```

### 3. Generate Openers (Placeholder)
```bash
POST http://localhost:8080/api/profile/generate-openers
Content-Type: application/json

"I love hiking and photography"
```

### 4. Photo Ranking (Placeholder)
```bash
POST http://localhost:8080/api/profile/rank-photos?photos=url1&photos=url2
```

## Next Steps

### Week 3-4: Implement Claude API Integration

You need to create a `ClaudeService` class to integrate with the Claude API:

```java
// src/main/java/com/groupf/dating/service/ClaudeService.java
@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public ClaudeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<String> rewriteBio(String bio, String tone) {
        // TODO: Implement Claude API call
        // See: https://docs.anthropic.com/claude/reference/messages_post
    }

    public List<String> generateOpeners(String bio) {
        // TODO: Implement Claude API call
    }

    public List<PhotoRanking> rankPhotos(List<String> photoUrls) {
        // TODO: Implement Claude Vision API call
    }
}
```

### Testing Your API

Use **Postman** or **curl** to test:

```bash
# Test health endpoint
curl http://localhost:8080/api/health

# Test bio rewrite (with placeholder)
curl -X POST http://localhost:8080/api/profile/rewrite-bio \
  -H "Content-Type: application/json" \
  -d '{"bio":"I like pizza and dogs","tone":"casual"}'
```

## Environment Variables

Create a `.env` file (already in .gitignore):

```bash
MONGODB_URI=mongodb://localhost:27017/dating-optimizer
CLAUDE_API_KEY=sk-ant-your-key-here
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Common Issues

### MongoDB Connection Error
**Error:** `Failed to connect to MongoDB`

**Solution:** Make sure MongoDB is running:
```bash
brew services start mongodb-community
mongosh  # Test connection
```

### Port 8080 Already in Use
**Error:** `Port 8080 is already in use`

**Solution:**
```bash
# Find process
lsof -i :8080

# Kill it
kill -9 <PID>
```

### Claude API Key Missing
**Error:** `CLAUDE_API_KEY not configured`

**Solution:** Add your API key to `application.yaml`:
```yaml
claude:
  api:
    key: sk-ant-your-actual-key-here
```

## Resources

- **Claude API Docs:** https://docs.anthropic.com/
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **MongoDB Docs:** https://www.mongodb.com/docs/
- **README.md:** Comprehensive documentation

## Team Workflow

1. **Week 1-2:** ✅ Setup complete!
2. **Week 3-4:** Implement `ClaudeService` and integrate API
3. **Week 5-6:** Add file upload, connect React frontend
4. **Week 7-8:** Testing, deployment to Heroku
5. **Week 9:** Presentation and demo

Good luck with your project! 🚀
