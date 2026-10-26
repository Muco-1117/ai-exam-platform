package com.aiexam.common;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil configured(String secret) {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "expireHours", 1L);
        return jwt;
    }

    @Test void rejectsMissingAndShortSigningSecretsBeforeServingRequests() {
        for (String secret : new String[]{"", " ", "0123456789012345678901234567890"}) {
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> configured(secret).validateSecret());
            assertTrue(error.getMessage().contains("JWT_SECRET"));
        }
    }

    @Test void acceptsValidSecretAndRejectsTokenSignedWithAnotherKey() {
        JwtUtil first = configured("test-key-one-0123456789abcdef0123456789");
        JwtUtil second = configured("test-key-two-0123456789abcdef0123456789");
        first.validateSecret();
        second.validateSecret();
        String token = first.generate(42L);
        assertEquals(42L, first.parse(token));
        assertNull(second.parse(token));
        assertNull(first.parse("invalid"));
    }
}
