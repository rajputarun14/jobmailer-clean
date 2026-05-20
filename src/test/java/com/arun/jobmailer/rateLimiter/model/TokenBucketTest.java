package com.arun.jobmailer.rateLimiter.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void blocksWhenBucketIsEmpty() {
        TokenBucket bucket = new TokenBucket(2, 1);

        assertThat(bucket.allowRequest()).isTrue();
        assertThat(bucket.allowRequest()).isTrue();
        assertThat(bucket.allowRequest()).isFalse();
    }

    @Test
    void refillsTokensOverTime() throws Exception {
        TokenBucket bucket = new TokenBucket(1, 1);

        assertThat(bucket.allowRequest()).isTrue();
        assertThat(bucket.allowRequest()).isFalse();

        Thread.sleep(1100);

        assertThat(bucket.allowRequest()).isTrue();
    }
}
