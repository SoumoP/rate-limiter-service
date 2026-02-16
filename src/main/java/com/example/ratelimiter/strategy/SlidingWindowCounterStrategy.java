package com.example.ratelimiter.strategy;

import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Counter Algorithm
 * 
 * Hybrid approach that combines fixed window counters with weighted counting.
 * Uses previous and current window counts with time-based weighting.
 * 
 * Pros: More accurate than fixed window, less memory than sliding log
 * Cons: Slightly more complex calculations
 */
@Component
public class SlidingWindowCounterStrategy implements RateLimiterStrategy {
    
    private final ConcurrentHashMap<String, SlidingWindowData> windows = new ConcurrentHashMap<>();
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        long currentTime = System.currentTimeMillis();
        long windowSize = config.getWindowSizeInSeconds() * 1000;
        long currentWindowId = currentTime / windowSize;
        
        SlidingWindowData data = windows.compute(key, (k, existing) -> {
            if (existing == null) {
                return new SlidingWindowData(currentWindowId, 0, 0);
            }
            
            if (existing.getCurrentWindowId() < currentWindowId - 1) {
                // More than 2 windows old, reset
                return new SlidingWindowData(currentWindowId, 0, 0);
            } else if (existing.getCurrentWindowId() == currentWindowId - 1) {
                // Move current to previous, reset current
                return new SlidingWindowData(currentWindowId, 0, existing.getCurrentCount());
            }
            
            return existing;
        });
        
        return data.tryIncrement(config.getLimit(), windowSize, currentTime, currentWindowId);
    }
    
    @Override
    public void reset(String key) {
        windows.remove(key);
    }
    
    @Data
    @AllArgsConstructor
    private static class SlidingWindowData {
        private long currentWindowId;
        private int currentCount;
        private int previousCount;
        
        public synchronized RateLimitResult tryIncrement(int limit, long windowSize, 
                                                         long currentTime, long currentWindowId) {
            // Calculate weighted count
            long windowStart = currentWindowId * windowSize;
            double percentageInCurrentWindow = (double) (currentTime - windowStart) / windowSize;
            double weightedCount = (previousCount * (1 - percentageInCurrentWindow)) + currentCount;
            
            if (weightedCount < limit) {
                currentCount++;
                long remaining = limit - (long) Math.ceil(weightedCount) - 1;
                
                return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens(Math.max(0, remaining))
                    .retryAfterSeconds(0)
                    .message("Request allowed")
                    .build();
            }
            
            // Calculate retry time
            long windowEnd = windowStart + windowSize;
            long retryAfter = (windowEnd - currentTime) / 1000;
            
            return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(Math.max(1, retryAfter))
                .message("Rate limit exceeded - Sliding window counter limit reached")
                .build();
        }
    }
}
