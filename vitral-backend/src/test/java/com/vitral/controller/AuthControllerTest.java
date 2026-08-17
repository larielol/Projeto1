package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import com.vitral.dto.AccountResponse;
import com.vitral.dto.AuthResponse;
import com.vitral.dto.LoginRequest;
import com.vitral.dto.MensagemResponse;
import com.vitral.dto.RecuperarSenhaRequest;
import com.vitral.dto.RedefinirSenhaRequest;
import com.vitral.dto.ReenviarConfirmacaoRequest;
import com.vitral.dto.RegisterRequest;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.mapper.AccountMapper;
import com.vitral.security.AccountUserDetails;
import com.vitral.security.JwtService;
import com.vitral.security.RateLimitService;
import com.vitral.security.TokenBlacklistService;
import com.vitral.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private AuthController controller;

    @Test
    void registerRetornaCreated() {
        RegisterRequest request = new RegisterRequest("Ana", "ana.silva", "ana@vitral.com", "password123", AccountType.USUARIO);
        MensagemResponse response = new MensagemResponse("Cadastro criado");
        when(authService.register(request)).thenReturn(response);

        var result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void loginNormalizaChaveELimpaRateLimitAposSucesso() {
        LoginRequest request = new LoginRequest(" ANA@VITRAL.COM ", "senha");
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr("10.0.0.1");
        AuthResponse response = new AuthResponse("jwt", 3600, null);
        when(authService.login(request)).thenReturn(response);

        var result = controller.login(request, http);

        assertThat(result.getBody()).isSameAs(response);
        verify(rateLimitService).check("login:ip:10.0.0.1", 30, Duration.ofMinutes(15));
        verify(rateLimitService).check("login:id:ana@vitral.com", 5, Duration.ofMinutes(15));
        verify(rateLimitService).reset("login:id:ana@vitral.com");
    }

    @Test
    void confirmarRecuperarRedefinirEReenviarDelegamServicos() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setRemoteAddr("127.0.0.1");
        var recuperar = new RecuperarSenhaRequest("USER@VITRAL.COM");
        var redefinir = new RedefinirSenhaRequest("token", "password123");
        var reenviar = new ReenviarConfirmacaoRequest("USER@VITRAL.COM");
        MensagemResponse mensagem = new MensagemResponse("ok");
        when(authService.confirmarEmail("abc")).thenReturn(mensagem);
        when(authService.solicitarRecuperacaoSenha(recuperar)).thenReturn(mensagem);
        when(authService.redefinirSenha(redefinir)).thenReturn(mensagem);
        when(authService.reenviarConfirmacao(reenviar)).thenReturn(mensagem);

        assertThat(controller.confirmar("abc").getBody()).isSameAs(mensagem);
        assertThat(controller.recuperarSenha(recuperar, http).getBody()).isSameAs(mensagem);
        assertThat(controller.redefinirSenha(redefinir, http).getBody()).isSameAs(mensagem);
        assertThat(controller.reenviarConfirmacao(reenviar, http).getBody()).isSameAs(mensagem);
        verify(rateLimitService).check("recovery:email:user@vitral.com", 3, Duration.ofHours(1));
        verify(rateLimitService).check("confirmation:email:user@vitral.com", 3, Duration.ofHours(1));
    }

    @Test
    void meMapeiaContaAutenticada() {
        Account account = Account.builder().type(AccountType.USUARIO).email("user@vitral.com").ativo(true).build();
        AccountResponse response = new AccountResponse(1L, "User", "user", "user@vitral.com", AccountType.USUARIO, null);
        when(accountMapper.toResponse(account)).thenReturn(response);

        var result = controller.me(new AccountUserDetails(account));

        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void logoutSemBearerOuTokenInvalidoNaoRevoga() {
        MockHttpServletRequest semHeader = new MockHttpServletRequest();
        MockHttpServletRequest basico = new MockHttpServletRequest();
        basico.addHeader("Authorization", "Basic abc");
        MockHttpServletRequest invalido = new MockHttpServletRequest();
        invalido.addHeader("Authorization", "Bearer invalido");
        when(jwtService.isValid("invalido")).thenReturn(false);

        assertThat(controller.logout(semHeader).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.logout(basico).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.logout(invalido).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tokenBlacklistService, never()).revoke(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void logoutComBearerValidoRevogaToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valido");
        Instant expiration = Instant.now().plusSeconds(60);
        when(jwtService.isValid("valido")).thenReturn(true);
        when(jwtService.extractExpiration("valido")).thenReturn(expiration);

        var result = controller.logout(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tokenBlacklistService).revoke("valido", expiration);
    }
}
