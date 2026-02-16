package com.example.ratelimiter.model;

public enum RateLimiterType {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER,
    FIXED_WINDOW_COUNTER,
    LEAKY_BUCKET
}
