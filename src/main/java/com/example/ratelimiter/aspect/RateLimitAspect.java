package com.example.ratelimiter.aspect;

import com.example.ratelimiter.exception.RateLimitExceededException;
import com.example.ratelimiter.model.RateLimitConfig;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimiterService rateLimiterService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Build rate limit config
        RateLimitConfig config = RateLimitConfig.builder()
            .type(rateLimit.type())
            .limit(rateLimit.limit())
            .windowSizeInSeconds(rateLimit.windowSeconds())
            .refillRate(rateLimit.refillRate())
            .capacity(rateLimit.capacity())
            .build();
        
        // Evaluate the key using SpEL
        String key = evaluateKey(rateLimit.key(), joinPoint);
        
        log.debug("Rate limiting request with key: {}, type: {}", key, rateLimit.type());
        
        // Try to acquire permission
        RateLimitResult result = rateLimiterService.tryAcquire(key, config);
        
        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for key: {}. Retry after: {} seconds", 
                     key, result.getRetryAfterSeconds());
            throw new RateLimitExceededException(
                result.getMessage(), 
                result.getRetryAfterSeconds()
            );
        }
        
        log.debug("Request allowed. Remaining tokens: {}", result.getRemainingTokens());
        
        // Proceed with the method execution
        return joinPoint.proceed();
    }
    
    private String evaluateKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            // Add method parameters to context
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();
            
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], paramValues[i]);
            }
            
            // Add request to context
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                context.setVariable("request", attributes.getRequest());
            }
            
            return parser.parseExpression(keyExpression).getValue(context, String.class);
        } catch (Exception e) {
            log.error("Error evaluating key expression: {}", keyExpression, e);
            // Fallback to IP address
            return getClientIp();
        }
    }
    
    private String getClientIp() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            return request.getRemoteAddr();
        }
        
        return "unknown";
    }
}
