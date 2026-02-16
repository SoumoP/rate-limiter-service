package com.example.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {
    private int limit;              // Maximum number of requests
    private long windowSizeInSeconds; // Time window in seconds
    private RateLimiterType type;   // Strategy type
    private double refillRate;      // For Token Bucket and Leaky Bucket (tokens/second)
    private int capacity;           // For Token Bucket and Leaky Bucket
}
