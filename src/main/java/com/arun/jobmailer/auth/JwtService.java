package com.arun.jobmailer.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] secret;
    private final long ttlSeconds;

    public JwtService(@Value("${security.jwt.secret:${APP_PASSWORD:change-me-in-production}}") String secret,
                      @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(String username, List<String> roles) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = Map.of(
                    "sub", username,
                    "roles", roles,
                    "iat", now,
                    "exp", now + ttlSeconds
            );
            String unsigned = encodeJson(header) + "." + encodeJson(payload);
            return unsigned + "." + sign(unsigned);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token", e);
        }
    }

    public JwtClaims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) return null;

            Map<String, Object> payload = mapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            Object exp = payload.get("exp");
            if (!(exp instanceof Number) || ((Number) exp).longValue() < Instant.now().getEpochSecond()) {
                return null;
            }

            Object sub = payload.get("sub");
            Object roles = payload.get("roles");
            if (!(sub instanceof String) || !(roles instanceof List<?> roleList)) return null;
            List<String> roleNames = roleList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            return new JwtClaims((String) sub, roleNames);
        } catch (Exception e) {
            return null;
        }
    }

    private String encodeJson(Object value) throws Exception {
        return URL_ENCODER.encodeToString(mapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public record JwtClaims(String username, List<String> roles) {}
}
