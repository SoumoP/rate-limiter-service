package com.example.ratelimiter.strategy;

import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed Window Counter Algorithm
 * 
 * Divides time into fixed windows and counts requests per window.
 * Resets the counter at the start of each new window.
 * 
 * Pros: Simple, memory efficient
 * Cons: Can allow bursts at window boundaries (2x limit in short time)
 */
@Component
public class FixedWindowCounterStrategy implements RateLimiterStrategy {
    
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        long currentTime = System.currentTimeMillis();
        long windowSize = config.getWindowSizeInSeconds() * 1000;
        long currentWindow = currentTime / windowSize;
        
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.getWindowId() != currentWindow) {
                return new WindowCounter(currentWindow, 0);
            }
            return existing;
        });
        
        return counter.tryIncrement(config.getLimit(), windowSize, currentTime);
    }
    
    @Override
    public void reset(String key) {
        counters.remove(key);
    }
    
    @Data
    @AllArgsConstructor
    private static class WindowCounter {
        private final long windowId;
        private int count;
        
        public synchronized RateLimitResult tryIncrement(int limit, long windowSize, long currentTime) {
            if (count < limit) {
                count++;
                return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens(limit - count)
                    .retryAfterSeconds(0)
                    .message("Request allowed")
                    .build();
            }
            
            long windowStart = windowId * windowSize;
            long windowEnd = windowStart + windowSize;
            long retryAfter = (windowEnd - currentTime) / 1000;
            
            return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(retryAfter)
                .message("Rate limit exceeded - Fixed window limit reached")
                .build();
        }
    }
}
