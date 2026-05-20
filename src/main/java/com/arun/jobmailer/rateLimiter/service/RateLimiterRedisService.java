package com.arun.jobmailer.rateLimiter.service;

import java.util.Collections;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterRedisService {
    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> redisScript;

    public RateLimiterRedisService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;

        redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(
                new ClassPathResource(
                        "scripts/tokenBucket.lua"));

        redisScript.setResultType(Long.class);
    }

    public boolean allowRequest(
        String userId,
        int capacity,
        int refillRate) {

    String key = "rate_limit:" + userId;

    Long result = redisTemplate.execute(
            redisScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(
                    System.currentTimeMillis() / 1000),
            "1"
    );

    return result != null && result == 1;
}
}
