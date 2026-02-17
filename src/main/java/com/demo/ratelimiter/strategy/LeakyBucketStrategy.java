package com.demo.ratelimiter.strategy;

import com.demo.ratelimiter.model.RateLimitConfig;
import com.demo.ratelimiter.model.RateLimitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaky Bucket Algorithm
 * 
 * Requests are added to a queue (bucket) and processed at a constant rate.
 * If the bucket is full, requests are rejected.
 * Water (requests) leaks from the bucket at a constant rate.
 * 
 * Pros: Smooth output rate, prevents bursts
 * Cons: Can reject requests even when system has capacity
 */
@Component
public class LeakyBucketStrategy implements RateLimiterStrategy {
    
    private final ConcurrentHashMap<String, LeakyBucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        LeakyBucket bucket = buckets.computeIfAbsent(key, k -> 
            new LeakyBucket(config.getCapacity(), config.getRefillRate(), System.currentTimeMillis())
        );
        
        return bucket.tryAdd();
    }
    
    @Override
    public void reset(String key) {
        buckets.remove(key);
    }
    
    @Data
    @AllArgsConstructor
    private static class LeakyBucket {
        private double waterLevel;
        private final double leakRate;
        private final int capacity;
        private long lastLeakTimestamp;
        
        public LeakyBucket(int capacity, double leakRate, long currentTime) {
            this.waterLevel = 0;
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.lastLeakTimestamp = currentTime;
        }
        
        public synchronized RateLimitResult tryAdd() {
            leak();
            
            if (waterLevel < capacity) {
                waterLevel += 1;
                return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens((long) (capacity - waterLevel))
                    .retryAfterSeconds(0)
                    .message("Request allowed")
                    .build();
            }
            
            long retryAfter = (long) Math.ceil(1 / leakRate);
            return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .retryAfterSeconds(retryAfter)
                .message("Rate limit exceeded - Leaky bucket full")
                .build();
        }
        
        private void leak() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastLeakTimestamp;
            double leaked = (elapsedTime / 1000.0) * leakRate;
            
            if (leaked > 0) {
                waterLevel = Math.max(0, waterLevel - leaked);
                lastLeakTimestamp = currentTime;
            }
        }
    }
}
