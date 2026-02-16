package com.example.ratelimiter.strategy;

import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;

public interface RateLimiterStrategy {
    /**
     * Attempts to acquire permission for a request
     * 
     * @param key Unique identifier for the rate limit (e.g., user ID, IP address)
     * @param config Rate limit configuration
     * @return Result indicating if the request is allowed
     */
    RateLimitResult tryAcquire(String key, RateLimitConfig config);
    
    /**
     * Resets the rate limiter for a specific key
     * 
     * @param key Unique identifier to reset
     */
    void reset(String key);
}
