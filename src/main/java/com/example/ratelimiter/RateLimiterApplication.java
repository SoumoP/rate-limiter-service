package com.example.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RateLimiterApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }
}
