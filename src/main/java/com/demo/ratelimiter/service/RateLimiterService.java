package com.demo.ratelimiter.service;

import com.demo.ratelimiter.model.RateLimitConfig;
import com.demo.ratelimiter.model.RateLimitResult;
import com.demo.ratelimiter.model.RateLimiterType;
import com.demo.ratelimiter.strategy.FixedWindowCounterStrategy;
import com.demo.ratelimiter.strategy.LeakyBucketStrategy;
import com.demo.ratelimiter.strategy.RateLimiterStrategy;
import com.demo.ratelimiter.strategy.SlidingWindowCounterStrategy;
import com.demo.ratelimiter.strategy.SlidingWindowLogStrategy;
import com.demo.ratelimiter.strategy.TokenBucketStrategy;
import com.example.ratelimiter.strategy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private final TokenBucketStrategy tokenBucketStrategy;
    private final FixedWindowCounterStrategy fixedWindowCounterStrategy;
    private final SlidingWindowLogStrategy slidingWindowLogStrategy;
    private final SlidingWindowCounterStrategy slidingWindowCounterStrategy;
    private final LeakyBucketStrategy leakyBucketStrategy;
    
    private final Map<RateLimiterType, RateLimiterStrategy> strategies = new EnumMap<>(RateLimiterType.class);
    
    public void init() {
        strategies.put(RateLimiterType.TOKEN_BUCKET, tokenBucketStrategy);
        strategies.put(RateLimiterType.FIXED_WINDOW_COUNTER, fixedWindowCounterStrategy);
        strategies.put(RateLimiterType.SLIDING_WINDOW_LOG, slidingWindowLogStrategy);
        strategies.put(RateLimiterType.SLIDING_WINDOW_COUNTER, slidingWindowCounterStrategy);
        strategies.put(RateLimiterType.LEAKY_BUCKET, leakyBucketStrategy);
    }
    
    /**
     * Attempt to acquire permission for a request
     */
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        if (strategies.isEmpty()) {
            init();
        }
        
        RateLimiterStrategy strategy = strategies.get(config.getType());
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown rate limiter type: " + config.getType());
        }
        
        return strategy.tryAcquire(key, config);
    }
    
    /**
     * Reset rate limiter for a specific key and strategy
     */
    public void reset(String key, RateLimiterType type) {
        if (strategies.isEmpty()) {
            init();
        }
        
        RateLimiterStrategy strategy = strategies.get(type);
        if (strategy != null) {
            strategy.reset(key);
        }
    }
    
    /**
     * Reset rate limiter for a key across all strategies
     */
    public void resetAll(String key) {
        if (strategies.isEmpty()) {
            init();
        }
        
        strategies.values().forEach(strategy -> strategy.reset(key));
    }
}
