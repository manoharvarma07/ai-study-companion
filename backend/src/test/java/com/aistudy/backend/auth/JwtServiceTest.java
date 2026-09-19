package com.aistudy.backend.auth;

import com.aistudy.backend.common.config.AppProperties;
import com.aistudy.backend.user.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService service() {
        return new JwtService(new AppProperties(
                new AppProperties.Jwt("another-test-secret-that-is-long-enough-32!", 3600000),
                new AppProperties.Ai("", "https://api.openai.com/v1", "m", "e", 1536, 1000, true),
                new AppProperties.Rag(6),
                new AppProperties.Storage("./uploads")));
    }

    @Test
    void tokenRoundTripsUserId() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@b.com");
        u.setRole(User.Role.USER);
        String token = service().generate(u);
        assertThat(service().userId(token)).isEqualTo(u.getId());
    }
}
