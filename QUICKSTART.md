# Quick Start Guide

Get up and running with the Rate Limiter Service in 5 minutes!

## Step 1: Prerequisites Check

```bash
# Check Java version (need Java 17+)
java -version

# Check Maven version
mvn -version
```

## Step 2: Build the Project

```bash
cd rate-limiter-service
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

## Step 3: Run the Service

```bash
mvn spring-boot:run
```

Wait for:
```
Started RateLimiterApplication in X.XXX seconds
```

The service is now running on `http://localhost:8080`

## Step 4: Test the Service

### Option A: Use the Test Script (Recommended)

```bash
./test-rate-limiter.sh
```

### Option B: Manual Testing with cURL

#### Test 1: Token Bucket (allows 5 requests, then rate limits)

```bash
# Send 8 requests quickly
for i in {1..8}; do
  curl http://localhost:8080/api/token-bucket
  echo ""
done
```

Expected: First 5 succeed, then rate limited

#### Test 2: Per-User Rate Limiting

```bash
# Different users get separate quotas
curl http://localhost:8080/api/user/alice
curl http://localhost:8080/api/user/bob
curl http://localhost:8080/api/user/alice
```

#### Test 3: Check Rate Limit Programmatically

```bash
curl -X POST "http://localhost:8080/api/check-limit?key=user123" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TOKEN_BUCKET",
    "limit": 10,
    "windowSizeInSeconds": 60,
    "refillRate": 1.0,
    "capacity": 10
  }'
```

## Step 5: Add to Your Own Code

### Example 1: Add Rate Limiting to Your Controller

```java
import aspect.com.demo.ratelimiter.RateLimit;
import model.com.demo.ratelimiter.RateLimiterType;

@RestController
public class MyController {

  @GetMapping("/api/my-endpoint")
  @RateLimit(type = RateLimiterType.TOKEN_BUCKET, limit = 100, windowSeconds = 3600)
  public String myEndpoint() {
    return "Success!";
  }
}
```

### Example 2: Rate Limit per User

```java
@GetMapping("/api/user/{userId}/resource")
@RateLimit(
    type = RateLimiterType.SLIDING_WINDOW_COUNTER,
    limit = 50,
    windowSeconds = 60,
    key = "#userId"  // Different limit per user
)
public String getUserResource(@PathVariable String userId) {
    return "User resource for " + userId;
}
```

### Example 3: Use Programmatically

```java
@Autowired
private RateLimiterService rateLimiterService;

public void myMethod(String userId) {
    RateLimitConfig config = RateLimitConfig.builder()
        .type(RateLimiterType.TOKEN_BUCKET)
        .limit(10)
        .windowSizeInSeconds(60)
        .refillRate(1.0)
        .capacity(10)
        .build();
    
    RateLimitResult result = rateLimiterService.tryAcquire(userId, config);
    
    if (!result.isAllowed()) {
        throw new RuntimeException("Rate limit exceeded!");
    }
    
    // Process request
}
```

## Common Use Cases

### Use Case 1: Protect Public API

```java
@RestController
public class PublicApiController {
    
    @GetMapping("/api/public/data")
    @RateLimit(
        type = RateLimiterType.FIXED_WINDOW_COUNTER,
        limit = 1000,
        windowSeconds = 3600  // 1000 requests per hour
    )
    public ResponseEntity<?> getPublicData() {
        // Your logic here
    }
}
```

### Use Case 2: Different Limits for Different API Keys

```java
@GetMapping("/api/premium/feature")
@RateLimit(
    type = RateLimiterType.TOKEN_BUCKET,
    limit = 10000,
    windowSeconds = 3600,
    key = "#request.getHeader('X-API-Key')"
)
public ResponseEntity<?> premiumFeature() {
    // Premium feature logic
}
```

### Use Case 3: Protect Expensive Operations

```java
@PostMapping("/api/process/heavy-task")
@RateLimit(
    type = RateLimiterType.LEAKY_BUCKET,
    capacity = 5,
    refillRate = 0.1,  // Only 1 request every 10 seconds
    key = "'global'"   // Global limit across all users
)
public ResponseEntity<?> heavyTask() {
    // Expensive operation
}
```

## Understanding Rate Limit Responses

### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Request allowed",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Rate Limited Response (429 Too Many Requests)

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded - Token bucket depleted",
  "retryAfterSeconds": 5
}
```

**Important Headers:**
- `Retry-After`: Number of seconds to wait before retrying

## Choosing the Right Strategy

| Scenario | Recommended Strategy | Reason |
|----------|---------------------|---------|
| General API | Token Bucket | Best balance |
| Simple quotas | Fixed Window | Easy to understand |
| Critical operations | Sliding Window Log | Most accurate |
| Multi-tenant SaaS | Sliding Window Counter | Fair, efficient |
| Protect downstream | Leaky Bucket | Smooth output |

## Next Steps

1. **Read the full README**: `README.md` - Complete documentation
2. **Study strategies**: `STRATEGIES.md` - Visual comparisons
3. **Explore examples**: Check the `DemoController.java` for more examples
4. **Customize**: Modify rate limits based on your needs

## Troubleshooting

### Service won't start

**Problem:** Port 8080 already in use

**Solution:** Change port in `application.properties`:
```properties
server.port=8081
```

### Rate limits too strict/loose

**Problem:** Getting rate limited too quickly or not at all

**Solution:** Adjust annotation parameters:
```java
@RateLimit(
    limit = 100,        // Increase/decrease
    windowSeconds = 60  // Make window longer/shorter
)
```

### Need to reset during testing

**Solution:** Call reset endpoint:
```bash
curl -X DELETE http://localhost:8080/api/reset/TOKEN_BUCKET/your-key
```

## Support

For questions or issues:
1. Check `README.md` for detailed documentation
2. Review `STRATEGIES.md` for algorithm explanations
3. Look at test examples in `test-rate-limiter.sh`

Happy rate limiting! 🚀
