package com.vitral.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.vitral.exception.BusinessException;

class RateLimitServiceTest {

    private final RateLimitService service = new RateLimitService();

    @Test
    void bloqueiaAposAtingirOLimite() {
        service.check("login:email:teste", 2, Duration.ofMinutes(1));
        service.check("login:email:teste", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> service.check("login:email:teste", 2, Duration.ofMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void resetLiberaNovasTentativas() {
        service.check("login:email:teste", 1, Duration.ofMinutes(1));
        service.reset("login:email:teste");
        service.check("login:email:teste", 1, Duration.ofMinutes(1));
    }
}
