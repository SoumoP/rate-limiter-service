package com.example.ratelimiter.strategy;

import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Sliding Window Log Algorithm
 * 
 * Maintains a log of request timestamps.
 * Removes timestamps outside the current window.
 * Counts requests in the sliding window.
 * 
 * Pros: Very accurate, no burst issues
 * Cons: Higher memory usage (stores all request timestamps)
 */
@Component
public class SlidingWindowLogStrategy implements RateLimiterStrategy {
    
    private final ConcurrentHashMap<String, Queue<Long>> requestLogs = new ConcurrentHashMap<>();
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        long currentTime = System.currentTimeMillis();
        long windowStartTime = currentTime - (config.getWindowSizeInSeconds() * 1000);
        
        Queue<Long> log = requestLogs.computeIfAbsent(key, k -> new LinkedList<>());
        
        synchronized (log) {
            // Remove timestamps outside the window
            while (!log.isEmpty() && log.peek() < windowStartTime) {
                log.poll();
            }
            
            if (log.size() < config.getLimit()) {
                log.offer(currentTime);
                return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens(config.getLimit() - log.size())
                    .retryAfterSeconds(0)
                    .message("Request allowed")
                    .build();
            }
            
            // Calculate retry time based on oldest request in window
            long oldestRequestTime = log.peek();
            long retryAfter = ((oldestRequestTime + (config.getWindowSizeInSeconds() * 1000)) - currentTime) / 1000;
            
            return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(Math.max(1, retryAfter))
                .message("Rate limit exceeded - Sliding window limit reached")
                .build();
        }
    }
    
    @Override
    public void reset(String key) {
        requestLogs.remove(key);
    }
}
