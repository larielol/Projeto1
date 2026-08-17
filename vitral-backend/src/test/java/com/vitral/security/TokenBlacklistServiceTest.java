package com.vitral.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    @DisplayName("Token recém-emitido não deve estar na blacklist")
    void tokenNotRevokedByDefault() {
        assertThat(tokenBlacklistService.isRevoked("qualquer-token")).isFalse();
    }

    @Test
    @DisplayName("Token revogado deve constar na blacklist")
    void revokedTokenIsDetected() {
        String token = "token-valido";
        Instant expiration = Instant.now().plusSeconds(3600);

        tokenBlacklistService.revoke(token, expiration);

        assertThat(tokenBlacklistService.isRevoked(token)).isTrue();
    }

    @Test
    @DisplayName("Purge deve remover apenas tokens com expiração no passado")
    void purgeRemovesOnlyExpiredTokens() {
        String expired = "token-expirado";
        String valid = "token-valido";

        tokenBlacklistService.revoke(expired, Instant.now().minusSeconds(1));
        tokenBlacklistService.revoke(valid, Instant.now().plusSeconds(3600));

        tokenBlacklistService.purgeExpired();

        assertThat(tokenBlacklistService.isRevoked(expired)).isFalse();
        assertThat(tokenBlacklistService.isRevoked(valid)).isTrue();
    }

    @Test
    @DisplayName("Token de outro usuário não deve ser afetado pela revogação")
    void revokingOneTokenDoesNotAffectOthers() {
        String tokenA = "token-usuario-a";
        String tokenB = "token-usuario-b";

        tokenBlacklistService.revoke(tokenA, Instant.now().plusSeconds(3600));

        assertThat(tokenBlacklistService.isRevoked(tokenA)).isTrue();
        assertThat(tokenBlacklistService.isRevoked(tokenB)).isFalse();
    }
}
