package com.demo.ratelimiter.aspect;

import com.demo.ratelimiter.model.RateLimiterType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Rate limiter strategy type
     */
    RateLimiterType type() default RateLimiterType.TOKEN_BUCKET;
    
    /**
     * Maximum number of requests allowed
     */
    int limit() default 10;
    
    /**
     * Time window in seconds
     */
    long windowSeconds() default 60;
    
    /**
     * Refill rate for Token Bucket and Leaky Bucket (tokens/requests per second)
     */
    double refillRate() default 1.0;
    
    /**
     * Capacity for Token Bucket and Leaky Bucket
     */
    int capacity() default 10;
    
    /**
     * Key to use for rate limiting. Supports SpEL expressions.
     * Default is the client IP address.
     * Examples: 
     *   - "#request.getHeader('X-API-Key')"
     *   - "#userId"
     *   - "'global'"
     */
    String key() default "#{T(org.springframework.web.context.request.RequestContextHolder).currentRequestAttributes().getRequest().getRemoteAddr()}";
}
