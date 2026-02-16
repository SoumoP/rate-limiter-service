# Rate Limiter Service - Spring Boot

A comprehensive Java Spring Boot service implementing multiple rate limiting strategies with annotation-based and programmatic APIs.

## Features

- **Multiple Rate Limiting Algorithms**
  - Token Bucket
  - Fixed Window Counter
  - Sliding Window Log
  - Sliding Window Counter
  - Leaky Bucket

- **Flexible Usage**
  - Annotation-based rate limiting with AOP
  - Programmatic API
  - SpEL expression support for dynamic keys
  - Per-user, per-IP, or custom key rate limiting

- **Production Ready**
  - Thread-safe implementations
  - Exception handling with proper HTTP status codes
  - Configurable limits and windows
  - Retry-After headers

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/rate-limiter-service-1.0.0.jar
```

The service will start on `http://localhost:8080`

## Rate Limiting Strategies

### 1. Token Bucket

**How it works:** Tokens are added to a bucket at a constant rate. Each request consumes one token. If no tokens are available, the request is rejected.

**Pros:**
- Handles bursts well
- Smooth rate limiting
- Allows temporary spikes

**Cons:**
- More complex than fixed window
- Requires token refill tracking

**Use case:** APIs that need to handle occasional bursts while maintaining average rate

```java
@GetMapping("/api/resource")
@RateLimit(
    type = RateLimiterType.TOKEN_BUCKET,
    capacity = 10,
    refillRate = 2.0  // 2 tokens per second
)
public ResponseEntity<?> getResource() {
    return ResponseEntity.ok("Success");
}
```

### 2. Fixed Window Counter

**How it works:** Time is divided into fixed windows. Counts requests per window and resets at window boundaries.

**Pros:**
- Simple to implement
- Memory efficient
- Easy to understand

**Cons:**
- Can allow bursts at window boundaries (up to 2x limit)
- Not perfectly accurate

**Use case:** Simple rate limiting where burst allowance is acceptable

```java
@GetMapping("/api/resource")
@RateLimit(
    type = RateLimiterType.FIXED_WINDOW_COUNTER,
    limit = 100,
    windowSeconds = 60
)
public ResponseEntity<?> getResource() {
    return ResponseEntity.ok("Success");
}
```

### 3. Sliding Window Log

**How it works:** Maintains a log of all request timestamps. Removes old timestamps outside the current window and counts remaining ones.

**Pros:**
- Most accurate
- No burst issues
- Precise rate limiting

**Cons:**
- Higher memory usage (stores all timestamps)
- More processing overhead

**Use case:** Critical APIs requiring precise rate limiting

```java
@GetMapping("/api/resource")
@RateLimit(
    type = RateLimiterType.SLIDING_WINDOW_LOG,
    limit = 100,
    windowSeconds = 60
)
public ResponseEntity<?> getResource() {
    return ResponseEntity.ok("Success");
}
```

### 4. Sliding Window Counter

**How it works:** Hybrid approach combining fixed window counters with weighted counting based on time position in window.

**Pros:**
- More accurate than fixed window
- Less memory than sliding log
- Good balance

**Cons:**
- Slightly more complex calculations
- Approximation (not 100% accurate)

**Use case:** General purpose rate limiting with good accuracy

```java
@GetMapping("/api/resource")
@RateLimit(
    type = RateLimiterType.SLIDING_WINDOW_COUNTER,
    limit = 100,
    windowSeconds = 60
)
public ResponseEntity<?> getResource() {
    return ResponseEntity.ok("Success");
}
```

### 5. Leaky Bucket

**How it works:** Requests are added to a queue (bucket). Requests "leak" from the bucket at a constant rate.

**Pros:**
- Smooth output rate
- Prevents traffic spikes
- Constant processing rate

**Cons:**
- Can reject requests even when system has capacity
- Queue management overhead

**Use case:** Protecting downstream services with strict rate requirements

```java
@GetMapping("/api/resource")
@RateLimit(
    type = RateLimiterType.LEAKY_BUCKET,
    capacity = 10,
    refillRate = 1.0  // Process 1 request per second
)
public ResponseEntity<?> getResource() {
    return ResponseEntity.ok("Success");
}
```

## Usage Examples

### Annotation-Based Rate Limiting

#### Rate Limit by IP Address (Default)

```java
@GetMapping("/api/public")
@RateLimit(
    type = RateLimiterType.TOKEN_BUCKET,
    limit = 10,
    windowSeconds = 60
)
public ResponseEntity<?> publicEndpoint() {
    return ResponseEntity.ok("Success");
}
```

#### Rate Limit by User ID

```java
@GetMapping("/api/user/{userId}/data")
@RateLimit(
    type = RateLimiterType.SLIDING_WINDOW_COUNTER,
    limit = 50,
    windowSeconds = 3600,
    key = "#userId"  // SpEL expression
)
public ResponseEntity<?> getUserData(@PathVariable String userId) {
    return ResponseEntity.ok("User data");
}
```

#### Rate Limit by API Key

```java
@GetMapping("/api/premium")
@RateLimit(
    type = RateLimiterType.TOKEN_BUCKET,
    limit = 1000,
    windowSeconds = 3600,
    key = "#request.getHeader('X-API-Key')"
)
public ResponseEntity<?> premiumEndpoint() {
    return ResponseEntity.ok("Premium feature");
}
```

#### Global Rate Limit

```java
@PostMapping("/api/expensive-operation")
@RateLimit(
    type = RateLimiterType.LEAKY_BUCKET,
    capacity = 5,
    refillRate = 0.1,  // Very slow: 1 request per 10 seconds
    key = "'global'"
)
public ResponseEntity<?> expensiveOperation() {
    return ResponseEntity.ok("Operation completed");
}
```

### Programmatic API

```java
@RestController
@RequiredArgsConstructor
public class CustomController {
    
    private final RateLimiterService rateLimiterService;
    
    @GetMapping("/api/custom")
    public ResponseEntity<?> customRateLimit(@RequestParam String userId) {
        // Build configuration
        RateLimitConfig config = RateLimitConfig.builder()
            .type(RateLimiterType.TOKEN_BUCKET)
            .limit(10)
            .windowSizeInSeconds(60)
            .refillRate(1.0)
            .capacity(10)
            .build();
        
        // Check rate limit
        RateLimitResult result = rateLimiterService.tryAcquire(userId, config);
        
        if (!result.isAllowed()) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(result.getRetryAfterSeconds()))
                .body("Rate limit exceeded");
        }
        
        // Process request
        return ResponseEntity.ok("Success");
    }
}
```

## API Endpoints

### Demo Endpoints

| Endpoint | Strategy | Limit | Window |
|----------|----------|-------|--------|
| `GET /api/token-bucket` | Token Bucket | 5 | 1s refill |
| `GET /api/fixed-window` | Fixed Window Counter | 5 | 60s |
| `GET /api/sliding-log` | Sliding Window Log | 5 | 60s |
| `GET /api/sliding-counter` | Sliding Window Counter | 5 | 60s |
| `GET /api/leaky-bucket` | Leaky Bucket | 5 | 1s leak |
| `GET /api/user/{userId}` | Token Bucket (per user) | 10 | 60s |

### Management Endpoints

#### Check Rate Limit (Programmatic)

```bash
POST /api/check-limit?key=user123
Content-Type: application/json

{
  "type": "TOKEN_BUCKET",
  "limit": 10,
  "windowSizeInSeconds": 60,
  "refillRate": 1.0,
  "capacity": 10
}
```

#### Reset Rate Limiter

```bash
DELETE /api/reset/{type}/{key}

# Example
DELETE /api/reset/TOKEN_BUCKET/user123
```

#### Health Check

```bash
GET /api/health
```

## Testing the Service

### Using cURL

```bash
# Test token bucket endpoint
for i in {1..10}; do
  curl http://localhost:8080/api/token-bucket
  echo ""
done

# Test with user ID
curl http://localhost:8080/api/user/alice

# Reset rate limiter
curl -X DELETE http://localhost:8080/api/reset/TOKEN_BUCKET/127.0.0.1
```

### Using HTTPie

```bash
# Test fixed window
http GET http://localhost:8080/api/fixed-window

# Test programmatic API
http POST http://localhost:8080/api/check-limit?key=user123 \
  type=TOKEN_BUCKET \
  limit=10 \
  windowSizeInSeconds=60 \
  refillRate=1.0 \
  capacity=10
```

## Response Format

### Success Response

```json
{
  "success": true,
  "message": "Request processed successfully",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Rate Limit Exceeded Response

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded - Token bucket depleted",
  "retryAfterSeconds": 5
}
```

**Headers:**
```
HTTP/1.1 429 Too Many Requests
Retry-After: 5
```

## Configuration

### Application Properties

```properties
# Server Configuration
server.port=8080

# Logging
logging.level.com.example.ratelimiter=DEBUG

# Redis (for distributed rate limiting - optional)
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Custom Configuration

You can customize rate limiting behavior by modifying the annotation parameters:

```java
@RateLimit(
    type = RateLimiterType.TOKEN_BUCKET,  // Algorithm
    limit = 100,                           // Max requests
    windowSeconds = 3600,                  // Time window
    refillRate = 2.0,                      // Refill rate (tokens/sec)
    capacity = 100,                        // Bucket capacity
    key = "#userId"                        // Rate limit key
)
```

## Algorithm Comparison

| Algorithm | Accuracy | Memory | Complexity | Bursts | Use Case |
|-----------|----------|--------|------------|--------|----------|
| Token Bucket | High | Low | Medium | ✅ Yes | General purpose, bursty traffic |
| Fixed Window | Medium | Low | Low | ⚠️ At boundaries | Simple scenarios |
| Sliding Window Log | Very High | High | Medium | ❌ No | Critical APIs |
| Sliding Window Counter | High | Medium | Medium | ⚠️ Limited | Balanced approach |
| Leaky Bucket | High | Low | Medium | ❌ No | Smooth output required |

## Architecture

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (Endpoints with @RateLimit annotation) │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│         RateLimitAspect (AOP)           │
│  - Intercepts annotated methods         │
│  - Evaluates SpEL expressions           │
│  - Throws RateLimitExceededException    │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│      RateLimiterService                 │
│  - Strategy selection                   │
│  - Delegates to specific strategy       │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│      Strategy Implementations           │
│  - TokenBucketStrategy                  │
│  - FixedWindowCounterStrategy           │
│  - SlidingWindowLogStrategy             │
│  - SlidingWindowCounterStrategy         │
│  - LeakyBucketStrategy                  │
└─────────────────────────────────────────┘
```

## Extension Points

### Adding a New Strategy

1. Implement `RateLimiterStrategy` interface
2. Add the strategy to `RateLimiterType` enum
3. Register in `RateLimiterService.init()`

Example:

```java
@Component
public class CustomStrategy implements RateLimiterStrategy {
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        // Your implementation
    }
    
    @Override
    public void reset(String key) {
        // Reset logic
    }
}
```

### Using Redis for Distributed Rate Limiting

For distributed systems, you can extend the strategies to use Redis:

```java
@Component
public class RedisTokenBucketStrategy implements RateLimiterStrategy {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        // Use Redis for distributed state
        String redisKey = "rate_limit:" + key;
        // Implementation using Redis Lua scripts
    }
}
```

## Best Practices

1. **Choose the Right Algorithm**
   - Token Bucket: Best for most APIs
   - Sliding Window Log: When accuracy is critical
   - Fixed Window: When simplicity is key
   - Leaky Bucket: When smooth output is required

2. **Set Appropriate Limits**
   - Consider your system capacity
   - Account for peak traffic
   - Leave room for bursts

3. **Use Meaningful Keys**
   - User ID for per-user limits
   - API key for different tiers
   - IP address for anonymous access
   - Endpoint name for per-endpoint limits

4. **Monitor and Adjust**
   - Track rate limit hits
   - Adjust limits based on usage patterns
   - Log exceeded requests

5. **Provide Clear Feedback**
   - Return appropriate HTTP status codes
   - Include Retry-After headers
   - Provide helpful error messages

## License

MIT License

## Author

Built with Spring Boot and Java
