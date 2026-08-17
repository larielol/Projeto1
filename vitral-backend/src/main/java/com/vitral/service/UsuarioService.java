package com.vitral.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.AccountResponse;
import com.vitral.dto.AtualizarPerfilRequest;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.mapper.AccountMapper;
import com.vitral.repository.AccountRepository;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ConflictException;
import org.springframework.http.HttpStatus;
import java.util.Locale;
import com.vitral.util.DocumentoFiscalUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AnonimizacaoContaService anonimizacaoContaService;

    @Transactional
    public AccountResponse atualizarPerfil(Account account, AtualizarPerfilRequest request) {
        account.setName(request.name());
        account.setFotoUrl(request.fotoUrl());
        if (request.cpf() != null) atualizarCpf(account, request.cpf());
        if (request.cep() != null) atualizarCep(account, request.cep());
        if (request.logradouro() != null) account.setLogradouro(normalizarOpcional(request.logradouro()));
        if (request.numero() != null) account.setNumero(normalizarOpcional(request.numero()));
        if (request.complemento() != null) account.setComplemento(normalizarOpcional(request.complemento()));
        if (request.bairro() != null) account.setBairro(normalizarOpcional(request.bairro()));
        if (request.cidade() != null) account.setCidade(normalizarOpcional(request.cidade()));
        if (request.estado() != null) account.setEstado(normalizarOpcional(request.estado()) == null
                ? null : request.estado().trim().toUpperCase(Locale.ROOT));
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    private void atualizarCpf(Account account, String valor) {
        if (valor.isBlank()) {
            account.setCpf(null);
            return;
        }
        String cpf = DocumentoFiscalUtils.somenteDigitos(valor);
        if (!DocumentoFiscalUtils.cpfValido(cpf)) {
            throw new BusinessException("CPF invalido", HttpStatus.BAD_REQUEST);
        }
        if (accountRepository.existsByCpfAndIdNot(cpf, account.getId())) {
            throw new ConflictException("CPF ja cadastrado");
        }
        account.setCpf(cpf);
    }

    private void atualizarCep(Account account, String valor) {
        if (valor.isBlank()) {
            account.setCep(null);
            return;
        }
        String cep = DocumentoFiscalUtils.somenteDigitos(valor);
        if (cep == null || cep.length() != 8) {
            throw new BusinessException("CEP deve conter 8 numeros", HttpStatus.BAD_REQUEST);
        }
        account.setCep(cep);
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @Transactional
    public MensagemResponse excluirConta(Account account) {
        anonimizacaoContaService.anonimizarUsuario(account);
        return new MensagemResponse("Conta excluida com sucesso");
    }
}
