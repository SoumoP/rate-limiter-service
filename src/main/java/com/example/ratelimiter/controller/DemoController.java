package com.example.ratelimiter.controller;

import com.example.ratelimiter.aspect.RateLimit;
import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.model.RateLimiterType;
import com.example.ratelimiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoController {

  private final RateLimiterService rateLimiterService;

  /**
   * Token Bucket example - allows bursts up to capacity
   */
  @GetMapping("/token-bucket")
  @RateLimit(type = RateLimiterType.TOKEN_BUCKET, capacity = 5, refillRate = 1.0) // 1 token per second
  public ResponseEntity<Map<String, Object>> tokenBucketExample() {
    return createSuccessResponse("Token Bucket - Allows bursts, refills at constant rate");
  }

  /**
   * Fixed Window Counter example - simple but can allow bursts at boundaries
   */
  @GetMapping("/fixed-window")
  @RateLimit(type = RateLimiterType.FIXED_WINDOW_COUNTER, limit = 5, windowSeconds = 60)
  public ResponseEntity<Map<String, Object>> fixedWindowExample() {
    return createSuccessResponse("Fixed Window Counter - Simple, resets at window boundaries");
  }

  /**
   * Sliding Window Log example - most accurate but memory intensive
   */
  @GetMapping("/sliding-log")
  @RateLimit(type = RateLimiterType.SLIDING_WINDOW_LOG, limit = 5, windowSeconds = 60)
  public ResponseEntity<Map<String, Object>> slidingLogExample() {
    return createSuccessResponse("Sliding Window Log - Most accurate, stores all timestamps");
  }

  /**
   * Sliding Window Counter example - balanced approach
   */
  @GetMapping("/sliding-counter")
  @RateLimit(type = RateLimiterType.SLIDING_WINDOW_COUNTER, limit = 5, windowSeconds = 60)
  public ResponseEntity<Map<String, Object>> slidingCounterExample() {
    return createSuccessResponse("Sliding Window Counter - Balanced accuracy and memory");
  }

  /**
   * Leaky Bucket example - smooth output rate
   */
  @GetMapping("/leaky-bucket")
  @RateLimit(type = RateLimiterType.LEAKY_BUCKET, capacity = 5, refillRate = 1.0  // Processes 1 request per second
  )
  public ResponseEntity<Map<String, Object>> leakyBucketExample() {
    return createSuccessResponse("Leaky Bucket - Smooth output, constant processing rate");
  }

  /**
   * Custom key example - rate limit per user ID
   */
  @GetMapping("/user/{userId}")
  @RateLimit(type = RateLimiterType.TOKEN_BUCKET, limit = 10, windowSeconds = 60, key = "#userId"  // Rate limit per user
  )
  public ResponseEntity<Map<String, Object>> perUserExample(@PathVariable String userId) {
    return createSuccessResponse("Rate limited per user: " + userId);
  }

  /**
   * Programmatic rate limiting example
   */
  @PostMapping("/check-limit")
  public ResponseEntity<Map<String, Object>> checkLimit(@RequestBody RateLimitConfig config, @RequestParam String key) {
    RateLimitResult result = rateLimiterService.tryAcquire(key, config);

    Map<String, Object> response = new HashMap<>();
    response.put("allowed", result.isAllowed());
    response.put("remainingTokens", result.getRemainingTokens());
    response.put("retryAfterSeconds", result.getRetryAfterSeconds());
    response.put("message", result.getMessage());
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.ok(response);
  }

  /**
   * Reset rate limiter for a specific key
   */
  @DeleteMapping("/reset/{type}/{key}")
  public ResponseEntity<Map<String, Object>> resetRateLimit(@PathVariable RateLimiterType type,
      @PathVariable String key) {
    rateLimiterService.reset(key, type);

    Map<String, Object> response = new HashMap<>();
    response.put("message", "Rate limiter reset successfully");
    response.put("type", type);
    response.put("key", key);
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.ok(response);
  }

  /**
   * Health check endpoint - no rate limiting
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", LocalDateTime.now());
    return ResponseEntity.ok(response);
  }

  private ResponseEntity<Map<String, Object>> createSuccessResponse(String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", message);
    response.put("timestamp", LocalDateTime.now());
    return ResponseEntity.ok(response);
  }
}
