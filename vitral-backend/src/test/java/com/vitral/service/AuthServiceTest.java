package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vitral.dto.AccountResponse;
import com.vitral.dto.AuthResponse;
import com.vitral.dto.LoginRequest;
import com.vitral.dto.MensagemResponse;
import com.vitral.dto.RegisterRequest;
import com.vitral.dto.RecuperarSenhaRequest;
import com.vitral.dto.RedefinirSenhaRequest;
import com.vitral.dto.ReenviarConfirmacaoRequest;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.entity.TokenRecuperacaoSenha;
import com.vitral.entity.TokenVerificacaoEmail;
import com.vitral.enumerations.AccountType;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.mapper.AccountMapper;
import com.vitral.repository.AccountRepository;
import com.vitral.repository.SeboRepository;
import com.vitral.repository.TokenVerificacaoEmailRepository;
import com.vitral.repository.TokenRecuperacaoSenhaRepository;
import com.vitral.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TokenVerificacaoEmailRepository tokenRepository;

    @Mock
    private TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private SeboRepository seboRepository;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Captor
    private ArgumentCaptor<TokenVerificacaoEmail> tokenCaptor;

    @Test
    @DisplayName("Deve registrar nova conta, salvar token e enviar e-mail quando o e-mail ainda nao existe")
    void shouldRegisterAccountAndSendConfirmationEmailWhenEmailIsAvailable() {
        RegisterRequest request = new RegisterRequest("Maria", "maria.silva", "maria@vitral.com", "senha1234", AccountType.USUARIO);
        Account savedAccount = Account.builder()
                .name("Maria")
                .username("maria.silva")
                .email("maria@vitral.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(false)
                .build();

        when(accountRepository.existsByEmail("maria@vitral.com")).thenReturn(false);
        when(accountRepository.existsByUsername("maria.silva")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash");
        when(accountRepository.save(accountCaptor.capture())).thenReturn(savedAccount);
        when(tokenRepository.save(tokenCaptor.capture())).thenReturn(new TokenVerificacaoEmail());
        when(emailService.isMailEnabled()).thenReturn(true);

        MensagemResponse result = authService.register(request);

        assertThat(result.mensagem()).contains("Verifique seu e-mail");

        Account persisted = accountCaptor.getValue();
        assertThat(persisted.getName()).isEqualTo("Maria");
        assertThat(persisted.getEmail()).isEqualTo("maria@vitral.com");
        assertThat(persisted.getPasswordHash()).isEqualTo("hash");
        assertThat(persisted.getType()).isEqualTo(AccountType.USUARIO);
        assertThat(persisted.isEmailVerificado()).isFalse();

        TokenVerificacaoEmail tokenSalvo = tokenCaptor.getValue();
        assertThat(tokenSalvo.getToken()).isNotBlank();
        assertThat(tokenSalvo.isUsado()).isFalse();
        assertThat(tokenSalvo.getExpiraEm()).isAfter(OffsetDateTime.now());

        verify(emailService).enviarConfirmacaoCadastro(savedAccount.getEmail(), tokenSalvo.getToken());
    }

    @Test
    @DisplayName("Deve lancar ConflictException e nao salvar quando o e-mail ja esta cadastrado")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Maria", "maria.silva", "maria@vitral.com", "senha1234", AccountType.USUARIO);
        when(accountRepository.existsByEmail("maria@vitral.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email ja cadastrado");

        verify(accountRepository, never()).save(any(Account.class));
        verify(tokenRepository, never()).save(any(TokenVerificacaoEmail.class));
        verify(emailService, never()).enviarConfirmacaoCadastro(any(), any());
    }

    @Test
    @DisplayName("Deve lancar ConflictException quando o nome de usuario ja esta cadastrado")
    void shouldThrowConflictWhenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Maria", "maria.silva", "maria@vitral.com", "senha1234", AccountType.USUARIO);
        when(accountRepository.existsByEmail("maria@vitral.com")).thenReturn(false);
        when(accountRepository.existsByUsername("maria.silva")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Usuario ja cadastrado");

        verify(accountRepository, never()).save(any(Account.class));
        verify(emailService, never()).enviarConfirmacaoCadastro(any(), any());
    }

    @Test
    @DisplayName("Deve autenticar por nome de usuario quando o identificador nao for e-mail")
    void shouldLoginWithUsernameWhenIdentifierIsNotEmail() {
        LoginRequest request = new LoginRequest("maria", "senha1234");
        Account account = Account.builder()
                .name("Maria")
                .username("maria")
                .email("maria@vitral.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build();
        AccountResponse accountResponse = new AccountResponse(1L, "Maria", "maria", "maria@vitral.com", AccountType.USUARIO, null);

        when(accountRepository.findByUsernameAndAtivoTrue("maria")).thenReturn(Optional.of(account));
        when(jwtService.generateToken("maria@vitral.com", "USUARIO", 0)).thenReturn("token-jwt");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("token-jwt");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("maria@vitral.com", "senha1234"));
    }

    @Test
    @DisplayName("Deve autenticar e retornar token quando e-mail estiver confirmado")
    void shouldLoginSuccessfullyWhenEmailIsVerified() {
        LoginRequest request = new LoginRequest("maria@vitral.com", "senha1234");
        Account account = Account.builder()
                .name("Maria")
                .email("maria@vitral.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build();
        AccountResponse accountResponse = new AccountResponse(1L, "Maria", "maria", "maria@vitral.com", AccountType.USUARIO, null);

        when(accountRepository.findByEmailAndAtivoTrue("maria@vitral.com")).thenReturn(Optional.of(account));
        when(jwtService.generateToken("maria@vitral.com", "USUARIO", 0)).thenReturn("token-jwt");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("token-jwt");
        assertThat(result.expiresIn()).isEqualTo(86400000L);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("maria@vitral.com", "senha1234"));
    }

    @Test
    @DisplayName("Deve atualizar ultima atividade do sebo quando vendedor autenticar com sucesso")
    void shouldUpdateSeboLastActivityWhenSellerLogsInSuccessfully() {
        LoginRequest request = new LoginRequest("sebo@vitral.com", "senha1234");
        Account account = Account.builder()
                .name("Sebo")
                .email("sebo@vitral.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .emailVerificado(true)
                .build();
        setId(account, 55L);
        Sebo sebo = Sebo.builder().account(account).build();
        AccountResponse accountResponse = new AccountResponse(55L, "Sebo", "sebo", "sebo@vitral.com", AccountType.SEBO, null);

        when(accountRepository.findByEmailAndAtivoTrue("sebo@vitral.com")).thenReturn(Optional.of(account));
        when(seboRepository.findByAccountId(55L)).thenReturn(Optional.of(sebo));
        when(jwtService.generateToken("sebo@vitral.com", "SEBO", 0)).thenReturn("token-jwt");
        when(jwtService.getExpiration()).thenReturn(86400000L);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("token-jwt");
        assertThat(sebo.getUltimaAtividade()).isNotNull();
        assertThat(sebo.getUltimaAtividade()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Deve lancar BusinessException 401 ao tentar login com e-mail nao confirmado")
    void shouldBlockLoginWhenEmailIsNotVerified() {
        LoginRequest request = new LoginRequest("maria@vitral.com", "senha1234");
        Account account = Account.builder()
                .email("maria@vitral.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(false)
                .build();

        when(accountRepository.findByEmailAndAtivoTrue("maria@vitral.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("E-mail nao confirmado")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Deve confirmar e-mail e marcar token como usado quando o token for valido")
    void shouldConfirmEmailAndMarkTokenAsUsedWhenTokenIsValid() {
        Account account = Account.builder()
                .email("maria@vitral.com")
                .emailVerificado(false)
                .build();
        TokenVerificacaoEmail token = TokenVerificacaoEmail.builder()
                .account(account)
                .token("uuid-valido")
                .expiraEm(OffsetDateTime.now().plusHours(24))
                .usado(false)
                .build();

        when(tokenRepository.findByToken("uuid-valido")).thenReturn(Optional.of(token));

        MensagemResponse result = authService.confirmarEmail("uuid-valido");

        assertThat(result.mensagem()).contains("confirmado com sucesso");
        assertThat(account.isEmailVerificado()).isTrue();
        assertThat(token.isUsado()).isTrue();
    }

    private void setId(Account account, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", id);
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando o token ja foi utilizado")
    void shouldThrowWhenConfirmTokenAlreadyUsed() {
        TokenVerificacaoEmail token = TokenVerificacaoEmail.builder()
                .token("uuid-usado")
                .expiraEm(OffsetDateTime.now().plusHours(24))
                .usado(true)
                .build();

        when(tokenRepository.findByToken("uuid-usado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.confirmarEmail("uuid-usado"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token ja utilizado");
    }

    @Test
    @DisplayName("Deve lancar BusinessException quando o token estiver expirado")
    void shouldThrowWhenConfirmTokenIsExpired() {
        TokenVerificacaoEmail token = TokenVerificacaoEmail.builder()
                .token("uuid-expirado")
                .expiraEm(OffsetDateTime.now().minusHours(1))
                .usado(false)
                .build();

        when(tokenRepository.findByToken("uuid-expirado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.confirmarEmail("uuid-expirado"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token expirado");
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando o token nao existir")
    void shouldThrowWhenConfirmTokenNotFound() {
        when(tokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.confirmarEmail("token-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Token de confirmacao invalido");
    }

    @Test
    @DisplayName("Deve responder de forma neutra quando o e-mail de recuperacao nao existir")
    void shouldNotRevealWhetherRecoveryEmailExists() {
        when(accountRepository.findByEmailAndAtivoTrue("ausente@vitral.com")).thenReturn(Optional.empty());

        MensagemResponse result = authService.solicitarRecuperacaoSenha(
                new RecuperarSenhaRequest("ausente@vitral.com"));

        assertThat(result.mensagem()).contains("Se o e-mail estiver cadastrado");
        verify(tokenRecuperacaoSenhaRepository, never()).save(any());
        verify(emailService, never()).enviarRecuperacaoSenha(any(), any());
    }

    @Test
    @DisplayName("Deve redefinir a senha e consumir o token de recuperacao")
    void shouldResetPasswordAndConsumeToken() {
        Account account = Account.builder().email("maria@vitral.com").passwordHash("antiga").build();
        TokenRecuperacaoSenha token = TokenRecuperacaoSenha.builder()
                .account(account)
                .token("token-recuperacao")
                .expiraEm(OffsetDateTime.now().plusHours(1))
                .usado(false)
                .build();
        when(tokenRecuperacaoSenhaRepository.findByToken("token-recuperacao")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("nova-hash");

        MensagemResponse result = authService.redefinirSenha(
                new RedefinirSenhaRequest("token-recuperacao", "novaSenha123"));

        assertThat(result.mensagem()).contains("sucesso");
        assertThat(account.getPasswordHash()).isEqualTo("nova-hash");
        assertThat(account.getAuthVersion()).isEqualTo(1);
        assertThat(token.isUsado()).isTrue();
    }

    @Test
    @DisplayName("Deve substituir o token e reenviar confirmacao para conta pendente")
    void shouldResendConfirmationForPendingAccount() {
        Account account = Account.builder()
                .email("maria@vitral.com")
                .emailVerificado(false)
                .build();
        when(accountRepository.findByEmailAndAtivoTrue("maria@vitral.com")).thenReturn(Optional.of(account));

        MensagemResponse result = authService.reenviarConfirmacao(
                new ReenviarConfirmacaoRequest("maria@vitral.com"));

        assertThat(result.mensagem()).contains("conta estiver pendente");
        verify(tokenRepository).deleteByAccountId(account.getId());
        verify(tokenRepository).save(any(TokenVerificacaoEmail.class));
        verify(emailService).enviarConfirmacaoCadastro(any(), any());
    }

    @Test
    @DisplayName("Nao deve reenviar confirmacao para conta ja verificada")
    void shouldNotResendConfirmationForVerifiedAccount() {
        Account account = Account.builder()
                .email("maria@vitral.com")
                .emailVerificado(true)
                .build();
        when(accountRepository.findByEmailAndAtivoTrue("maria@vitral.com")).thenReturn(Optional.of(account));

        authService.reenviarConfirmacao(new ReenviarConfirmacaoRequest("maria@vitral.com"));

        verify(tokenRepository, never()).save(any(TokenVerificacaoEmail.class));
        verify(emailService, never()).enviarConfirmacaoCadastro(any(), any());
    }
}
