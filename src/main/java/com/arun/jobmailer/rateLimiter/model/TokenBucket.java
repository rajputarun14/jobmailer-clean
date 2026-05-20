package com.arun.jobmailer.rateLimiter.model;

public class TokenBucket {
    private final int capacity;
    private int tokens;
    private long lastRefillTime;
    private final int refillRate;

    public TokenBucket(int capacity, int refillRate){
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillRate = refillRate;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest(){
        refillTokens();

        if(tokens > 0){
            tokens --;
            return true;
        }
        return false;
    }
    private void refillTokens(){
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRefillTime;
        int tokensToAdd = (int) (elapsedTime / 1000) * refillRate;

        if(tokensToAdd > 0){
            tokens = Math.min(tokens + tokensToAdd, capacity);
            lastRefillTime = now;
        }
    }
}
