package com.arun.jobmailer.rateLimiter.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.rateLimiter.model.TokenBucket;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillRatePerSecond;

    public RateLimiterService(@Value("${security.rate-limit.capacity:10}") int capacity,
                              @Value("${security.rate-limit.refill-rate-per-second:1}") int refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public boolean allowRequest(String userId) {
        TokenBucket tokenBucket = buckets.computeIfAbsent(userId,
                ignored -> new TokenBucket(capacity, refillRatePerSecond));
        return tokenBucket.allowRequest();
    }

}
