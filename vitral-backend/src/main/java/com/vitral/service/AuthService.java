package com.vitral.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.AuthResponse;
import com.vitral.dto.LoginRequest;
import com.vitral.dto.MensagemResponse;
import com.vitral.dto.RegisterRequest;
import com.vitral.dto.RecuperarSenhaRequest;
import com.vitral.dto.RedefinirSenhaRequest;
import com.vitral.dto.ReenviarConfirmacaoRequest;
import com.vitral.entity.Account;
import com.vitral.entity.TokenVerificacaoEmail;
import com.vitral.entity.TokenRecuperacaoSenha;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final TokenVerificacaoEmailRepository tokenRepository;
    private final TokenRecuperacaoSenhaRepository tokenRecuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AccountMapper accountMapper;
    private final EmailService emailService;
    private final SeboRepository seboRepository;

    @Transactional
    public MensagemResponse register(RegisterRequest request) {
        if (request.type() == AccountType.ADMIN) {
            throw new BusinessException("Tipo de conta invalido para cadastro publico", HttpStatus.BAD_REQUEST);
        }
        if (accountRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email ja cadastrado");
        }
        if (accountRepository.existsByUsername(request.username())) {
            throw new ConflictException("Usuario ja cadastrado");
        }

        Account account = Account.builder()
                .name(request.name())
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .type(request.type())
                .emailVerificado(!emailService.isMailEnabled())
                .build();

        Account saved = accountRepository.save(account);

        if (emailService.isMailEnabled()) {
            enviarTokenConfirmacao(saved);
            return new MensagemResponse("Cadastro realizado. Verifique seu e-mail para confirmar a conta.");
        }

        return new MensagemResponse("Cadastro realizado. Voce ja pode fazer login.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identificador = request.login().trim();
        Account account = (identificador.contains("@")
                ? accountRepository.findByEmailAndAtivoTrue(identificador)
                : accountRepository.findByUsernameAndAtivoTrue(identificador.toLowerCase(java.util.Locale.ROOT)))
                .orElseThrow(() -> new BusinessException("Credenciais invalidas", HttpStatus.UNAUTHORIZED));

        if (!account.isEmailVerificado()) {
            throw new BusinessException("E-mail nao confirmado. Verifique sua caixa de entrada.", HttpStatus.UNAUTHORIZED);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account.getEmail(), request.password()));

        registrarAtividadeSebo(account);

        return buildAuthResponse(account);
    }

    @Transactional
    public MensagemResponse confirmarEmail(String tokenValor) {
        TokenVerificacaoEmail token = tokenRepository.findByToken(tokenValor)
                .orElseThrow(() -> new ResourceNotFoundException("Token de confirmacao invalido"));

        if (token.isUsado()) {
            throw new BusinessException("Token ja utilizado", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (OffsetDateTime.now().isAfter(token.getExpiraEm())) {
            throw new BusinessException("Token expirado. Solicite um novo link de confirmacao.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        token.getAccount().setEmailVerificado(true);
        token.setUsado(true);

        return new MensagemResponse("E-mail confirmado com sucesso. Voce ja pode fazer login.");
    }

    @Transactional
    public MensagemResponse reenviarConfirmacao(ReenviarConfirmacaoRequest request) {
        accountRepository.findByEmailAndAtivoTrue(request.email()).ifPresent(account -> {
            if (!account.isEmailVerificado()) {
                tokenRepository.deleteByAccountId(account.getId());
                enviarTokenConfirmacao(account);
            }
        });
        return new MensagemResponse(
                "Se a conta estiver pendente, enviaremos um novo link de confirmação.");
    }

    @Transactional
    public MensagemResponse solicitarRecuperacaoSenha(RecuperarSenhaRequest request) {
        accountRepository.findByEmailAndAtivoTrue(request.email()).ifPresent(account -> {
            tokenRecuperacaoSenhaRepository.deleteByAccountId(account.getId());
            String tokenValor = UUID.randomUUID().toString();
            tokenRecuperacaoSenhaRepository.save(TokenRecuperacaoSenha.builder()
                    .account(account)
                    .token(tokenValor)
                    .expiraEm(OffsetDateTime.now().plusHours(1))
                    .usado(false)
                    .build());
            emailService.enviarRecuperacaoSenha(account.getEmail(), tokenValor);
        });
        return new MensagemResponse("Se o e-mail estiver cadastrado, enviaremos um link para redefinir sua senha.");
    }

    @Transactional
    public MensagemResponse redefinirSenha(RedefinirSenhaRequest request) {
        TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository.findByToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException("Link de recuperacao invalido"));
        if (token.isUsado()) {
            throw new BusinessException("Link de recuperacao ja utilizado", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (OffsetDateTime.now().isAfter(token.getExpiraEm())) {
            throw new BusinessException("Link de recuperacao expirado. Solicite um novo.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        token.getAccount().setPasswordHash(passwordEncoder.encode(request.password()));
        token.getAccount().setAuthVersion(token.getAccount().getAuthVersion() + 1);
        token.setUsado(true);
        return new MensagemResponse("Senha redefinida com sucesso.");
    }

    private AuthResponse buildAuthResponse(Account account) {
        String token = jwtService.generateToken(
                account.getEmail(), account.getType().name(), account.getAuthVersion());
        return new AuthResponse(token, jwtService.getExpiration(), accountMapper.toResponse(account));
    }

    private void registrarAtividadeSebo(Account account) {
        if (account.getType() != AccountType.SEBO) {
            return;
        }
        seboRepository.findByAccountId(account.getId())
                .ifPresent(sebo -> sebo.setUltimaAtividade(OffsetDateTime.now()));
    }

    private void enviarTokenConfirmacao(Account account) {
        String tokenValor = UUID.randomUUID().toString();
        tokenRepository.save(TokenVerificacaoEmail.builder()
                .account(account)
                .token(tokenValor)
                .expiraEm(OffsetDateTime.now().plusHours(24))
                .usado(false)
                .build());
        emailService.enviarConfirmacaoCadastro(account.getEmail(), tokenValor);
    }
}
