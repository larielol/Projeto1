package com.vitral.service;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.MensagemResponse;
import com.vitral.dto.SeboRequest;
import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.StatusVerificacaoSebo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.mapper.SeboMapper;
import com.vitral.repository.SeboRepository;

import lombok.RequiredArgsConstructor;
import com.vitral.util.DocumentoFiscalUtils;

@Service
@RequiredArgsConstructor
public class SeboService {

    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP",
            "SE", "TO");

    private final SeboRepository seboRepository;
    private final SeboMapper seboMapper;
    private final AnonimizacaoContaService anonimizacaoContaService;
    private final CnpjConsultaService cnpjConsultaService;
    private final SeboGeocodingService seboGeocodingService;

    @Value("${app.sebo-verificacao.mock-auto-aprovar:true}")
    private boolean mockAutoAprovarVerificacao;

    @Transactional
    public SeboResponse criar(Account account, SeboRequest request) {
        if (account.getType() != AccountType.SEBO) {
            throw new BusinessException("Apenas contas do tipo SEBO podem criar perfil de sebo", HttpStatus.FORBIDDEN);
        }
        if (seboRepository.existsByAccountId(account.getId())) {
            throw new ConflictException("Sebo ja cadastrado para esta conta");
        }
        String cnpj = validarCnpj(request.cnpj());
        if (seboRepository.existsByCnpj(cnpj)) {
            throw new ConflictException("CNPJ ja cadastrado");
        }
        // Fluxo real: novo cadastro comeca PENDENTE ate um ADMIN revisar
        // (ver VerificacaoSeboService.revisar). Isso ja e o default da entidade Sebo.
        // .statusVerificacao(StatusVerificacaoSebo.PENDENTE)
        Sebo sebo = Sebo.builder()
                .account(account)
                .descricao(request.descricao())
                .telefone(request.telefone())
                .cnpj(cnpj)
                .fotoUrl(request.fotoUrl())
                .build();
        aplicarEndereco(sebo, request);
        seboGeocodingService.geocodificar(sebo);
        aprovarAutomaticamenteEnquantoNaoHaAdmin(sebo);
        OffsetDateTime agora = OffsetDateTime.now();
        sebo.setDataCriacao(agora);
        sebo.setUltimaAtividade(agora);
        Sebo salvo = seboRepository.save(sebo);
        cnpjConsultaService.consultar(salvo);
        return seboMapper.toResponse(salvo);
    }

    @Transactional
    public SeboResponse atualizarMeuSebo(Account account, SeboRequest request) {
        Sebo sebo = seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
        sebo.setDescricao(request.descricao());
        sebo.setTelefone(request.telefone());
        String cnpj = validarCnpj(request.cnpj());
        if (!java.util.Objects.equals(sebo.getCnpj(), cnpj)) {
            if (seboRepository.existsByCnpjAndIdNot(cnpj, sebo.getId())) {
                throw new ConflictException("CNPJ ja cadastrado");
            }
            sebo.setCnpj(cnpj);
            // Fluxo real: alterar o CNPJ reabre a verificacao (volta pra PENDENTE ate um ADMIN revisar).
            sebo.setStatusVerificacao(StatusVerificacaoSebo.PENDENTE);
            sebo.setVerificadoEm(null);
            sebo.setConfirmado(false);
            aprovarAutomaticamenteEnquantoNaoHaAdmin(sebo);
            cnpjConsultaService.consultar(sebo);
        }
        sebo.setFotoUrl(request.fotoUrl());
        String cepAntigo = sebo.getCep();
        aplicarEndereco(sebo, request);
        boolean enderecoMudou = !java.util.Objects.equals(cepAntigo, sebo.getCep());
        // Tenta de novo quando o sebo ainda esta sem coordenadas (ex.: tentativa anterior
        // falhou ou o CEP nao mudou desta vez).
        boolean aindaSemCoordenadas = sebo.getLatitude() == null || sebo.getLongitude() == null;
        if (enderecoMudou || aindaSemCoordenadas) {
            seboGeocodingService.geocodificar(sebo);
        }
        // Cobre o caso de um sebo que ficou PENDENTE antes do mock existir (ou por qualquer
        // outro motivo) e so esta salvando o perfil sem trocar CNPJ/documento: com o mock
        // ativo, qualquer atualizacao de perfil tambem aprova automaticamente.
        if (sebo.getStatusVerificacao() != StatusVerificacaoSebo.VERIFICADO) {
            aprovarAutomaticamenteEnquantoNaoHaAdmin(sebo);
        }
        registrarAtividade(sebo);
        return seboMapper.toResponse(sebo);
    }

    @Transactional(readOnly = true)
    public SeboResponse buscarPorId(Long id) {
        Sebo sebo = seboRepository.findByIdAndAccountAtivoTrueAndStatusVerificacao(
                id, StatusVerificacaoSebo.VERIFICADO)
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado"));
        return seboMapper.toPublicResponse(sebo);
    }

    @Transactional(readOnly = true)
    public SeboResponse buscarMeuSebo(Account account) {
        return seboMapper.toResponse(buscarEntidadePorAccount(account.getId()));
    }

    @Transactional(readOnly = true)
    public Sebo buscarEntidadePorAccount(Long accountId) {
        return seboRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
    }

    @Transactional(readOnly = true)
    public Page<SeboResponse> listarPendentes(Pageable pageable) {
        return seboRepository.findByStatusVerificacao(StatusVerificacaoSebo.PENDENTE, pageable)
                .map(seboMapper::toResponse);
    }

    @Transactional
    public MensagemResponse excluirConta(Account account) {
        anonimizacaoContaService.anonimizarSebo(account);
        return new MensagemResponse("Conta do sebo excluida com sucesso");
    }

    public void registrarAtividade(Sebo sebo) {
        sebo.setUltimaAtividade(OffsetDateTime.now());
    }

    private void aplicarEndereco(Sebo sebo, SeboRequest request) {
        String cep = apenasDigitos(request.cep());
        String logradouro = trimToNull(request.logradouro());
        String cidade = trimToNull(request.cidade());
        String uf = trimToNull(request.uf());
        if (cep == null || logradouro == null || cidade == null || uf == null) {
            throw new BusinessException("CEP, logradouro, cidade e UF sao obrigatorios",
                    HttpStatus.BAD_REQUEST);
        }
        if (!cep.matches("\\d{8}")) {
            throw new BusinessException("CEP deve conter 8 numeros", HttpStatus.BAD_REQUEST);
        }
        if (!UFS.contains(uf.toUpperCase())) {
            throw new BusinessException("UF invalida", HttpStatus.BAD_REQUEST);
        }
        sebo.setCep(cep);
        sebo.setLogradouro(logradouro);
        sebo.setCidade(cidade);
        sebo.setUf(uf.toUpperCase());
        sebo.setHorarioFuncionamento(trimToNull(request.horarioFuncionamento()));
    }

    private String apenasDigitos(String valor) {
        String texto = trimToNull(valor);
        if (texto == null) {
            return null;
        }
        String digitos = texto.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }

    private String validarCnpj(String valor) {
        String cnpj = DocumentoFiscalUtils.somenteDigitos(valor);
        if (!DocumentoFiscalUtils.cnpjValido(cnpj)) {
            throw new BusinessException("CNPJ invalido", HttpStatus.BAD_REQUEST);
        }
        return cnpj;
    }

    /**
     * MOCK TEMPORARIO (ainda nao existe conta ADMIN para revisar cadastros):
     * aprova o sebo automaticamente, pulando a analise manual normalmente feita
     * em VerificacaoSeboService.revisar(). Defina
     * app.sebo-verificacao.mock-auto-aprovar=false (ou remova este metodo e as
     * chamadas a ele) quando houver um ADMIN operando, para que o sebo volte a
     * ficar PENDENTE ate a revisao manual.
     */
    private void aprovarAutomaticamenteEnquantoNaoHaAdmin(Sebo sebo) {
        if (!mockAutoAprovarVerificacao) {
            return;
        }
        sebo.setStatusVerificacao(StatusVerificacaoSebo.VERIFICADO);
        sebo.setVerificadoEm(OffsetDateTime.now());
        sebo.setMotivoRejeicao(null);
        sebo.setConfirmado(Boolean.TRUE);
    }

    private String trimToNull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
