package com.demo.ratelimiter.strategy;

import com.demo.ratelimiter.model.RateLimitConfig;
import com.demo.ratelimiter.model.RateLimitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Algorithm
 * 
 * Tokens are added to the bucket at a fixed rate (refill rate).
 * Each request consumes one token.
 * If no tokens are available, the request is rejected.
 * 
 * Pros: Handles bursts well, smooth rate limiting
 * Cons: More complex than fixed window
 */
@Component
public class TokenBucketStrategy implements RateLimiterStrategy {
    
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> 
            new TokenBucket(config.getCapacity(), config.getRefillRate(), System.currentTimeMillis())
        );
        
        return bucket.tryConsume();
    }
    
    @Override
    public void reset(String key) {
        buckets.remove(key);
    }
    
    @Data
    @AllArgsConstructor
    private static class TokenBucket {
        private double tokens;
        private final double refillRate;
        private long lastRefillTimestamp;
        
        public TokenBucket(int capacity, double refillRate, long currentTime) {
            this.tokens = capacity;
            this.refillRate = refillRate;
            this.lastRefillTimestamp = currentTime;
        }
        
        public synchronized RateLimitResult tryConsume() {
            refill();
            
            if (tokens >= 1) {
                tokens -= 1;
                return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens((long) tokens)
                    .retryAfterSeconds(0)
                    .message("Request allowed")
                    .build();
            }
            
            long retryAfter = (long) Math.ceil((1 - tokens) / refillRate);
            return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(retryAfter)
                .message("Rate limit exceeded - Token bucket depleted")
                .build();
        }
        
        private void refill() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastRefillTimestamp;
            double tokensToAdd = (elapsedTime / 1000.0) * refillRate;
            
            if (tokensToAdd > 0) {
                tokens = Math.min(tokens + tokensToAdd, refillRate * 60); // Cap at 1 minute worth
                lastRefillTimestamp = currentTime;
            }
        }
    }
}
