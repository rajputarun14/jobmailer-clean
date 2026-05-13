package com.arun.jobmailer.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void createsAndVerifiesToken() {
        JwtService service = new JwtService("test-secret-with-enough-length", 3600);

        String token = service.createToken("arun", List.of("USER", "ADMIN"));

        JwtService.JwtClaims claims = service.verify(token);
        assertThat(claims).isNotNull();
        assertThat(claims.username()).isEqualTo("arun");
        assertThat(claims.roles()).containsExactly("USER", "ADMIN");
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = new JwtService("test-secret-with-enough-length", 3600);
        String token = service.createToken("arun", List.of("USER"));

        assertThat(service.verify(token + "x")).isNull();
    }
}
