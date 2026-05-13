package com.arun.jobmailer.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RateLimitFilterTest {

    @Test
    void rejectsRequestsAfterLimitWithinWindow() {
        RateLimitFilter filter = new RateLimitFilter(2, 60, Clock.fixed(Instant.parse("2026-05-12T00:00:00Z"), ZoneId.of("UTC")));

        assertThat(filter.allow("alice")).isTrue();
        assertThat(filter.allow("alice")).isTrue();
        assertThat(filter.allow("alice")).isFalse();
    }
}
