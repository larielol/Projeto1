package com.vitral.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.vitral.dto.AuditoriaVerificacaoSeboResponse;
import com.vitral.dto.DocumentoVerificacaoSeboRequest;
import com.vitral.dto.DocumentoVerificacaoSeboResponse;
import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.AuditoriaVerificacaoSebo;
import com.vitral.entity.DocumentoUpload;
import com.vitral.entity.DocumentoVerificacaoSebo;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.StatusDocumentoSebo;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.enumerations.TipoDocumentoSebo;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.mapper.SeboMapper;
import com.vitral.repository.AuditoriaVerificacaoSeboRepository;
import com.vitral.repository.DocumentoVerificacaoSeboRepository;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.SeboRepository;
import com.vitral.repository.DocumentoUploadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificacaoSeboService {
    private final SeboRepository seboRepository;
    private final DocumentoVerificacaoSeboRepository documentoRepository;
    private final AuditoriaVerificacaoSeboRepository auditoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final SeboMapper seboMapper;
    private final DocumentoUploadRepository uploadRepository;
    private final DocumentoUploadService documentoUploadService;

    private static final String PREFIXO_DOCUMENTO = "/api/v1/uploads/documents/";

    @Value("${app.sebo-verificacao.mock-auto-aprovar:true}")
    private boolean mockAutoAprovarVerificacao;

    @Transactional
    public DocumentoVerificacaoSeboResponse enviarDocumento(Account account, DocumentoVerificacaoSeboRequest request) {
        Sebo sebo = buscarPorAccount(account);
        String nomeInterno = extrairNomeInterno(request.arquivoUrl());
        if (!uploadRepository.existsByNomeInternoAndSeboAccountId(nomeInterno, account.getId())) {
            throw new BusinessException("O arquivo informado nao pertence ao sebo autenticado", HttpStatus.FORBIDDEN);
        }
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(request.tipoDocumento()).arquivoUrl(request.arquivoUrl())
                .status(StatusDocumentoSebo.PENDENTE).enviadoEm(OffsetDateTime.now()).build();
        // Fluxo real: reenviar documento reabre a verificacao (volta pra PENDENTE ate um ADMIN revisar).
        sebo.setStatusVerificacao(StatusVerificacaoSebo.PENDENTE);
        sebo.setVerificadoEm(null);
        sebo.setMotivoRejeicao(null);
        sebo.setConfirmado(false);
        DocumentoVerificacaoSebo salvo = documentoRepository.save(documento);
        aprovarAutomaticamenteEnquantoNaoHaAdmin(sebo);
        return toResponse(salvo, metadataPorNome(nomeInterno));
    }

    @Transactional
    public DocumentoVerificacaoSeboResponse enviarDocumento(Account account, TipoDocumentoSebo tipo, MultipartFile arquivo) {
        if (tipo == null) {
            throw new BusinessException("Informe o tipo do documento", HttpStatus.BAD_REQUEST);
        }
        Sebo sebo = buscarPorAccount(account);
        DocumentoUpload metadata = documentoUploadService.armazenarComMetadata(account, arquivo);
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(tipo).arquivoUrl(documentoUploadService.arquivoUrl(metadata))
                .status(StatusDocumentoSebo.PENDENTE).enviadoEm(OffsetDateTime.now()).build();
        // Fluxo real: reenviar documento reabre a verificacao (volta pra PENDENTE ate um ADMIN revisar).
        sebo.setStatusVerificacao(StatusVerificacaoSebo.PENDENTE);
        sebo.setVerificadoEm(null);
        sebo.setMotivoRejeicao(null);
        sebo.setConfirmado(false);
        DocumentoVerificacaoSebo salvo = documentoRepository.save(documento);
        aprovarAutomaticamenteEnquantoNaoHaAdmin(sebo);
        return toResponse(salvo, metadata);
    }

    @Transactional(readOnly = true)
    public List<DocumentoVerificacaoSeboResponse> listarMeusDocumentos(Account account) {
        return listarDocumentos(buscarPorAccount(account).getId());
    }

    @Transactional(readOnly = true)
    public List<DocumentoVerificacaoSeboResponse> listarDocumentos(Long seboId) {
        validarSebo(seboId);
        return documentoRepository.findBySeboIdOrderByEnviadoEmDesc(seboId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaVerificacaoSeboResponse> listarAuditoria(Long seboId) {
        validarSebo(seboId);
        return auditoriaRepository.findBySeboIdOrderByCreatedAtDesc(seboId).stream()
                .map(a -> new AuditoriaVerificacaoSeboResponse(a.getId(), a.getAnalisadoPor().getId(),
                        a.getStatusAnterior(), a.getNovoStatus(), a.getMotivo(), a.getCreatedAt()))
                .toList();
    }

    @Transactional
    public SeboResponse revisar(Account admin, Long seboId, StatusVerificacaoSebo status, String motivo) {
        if (admin.getType() != AccountType.ADMIN) {
            throw new BusinessException("Apenas administradores podem revisar sebos", HttpStatus.FORBIDDEN);
        }
        if (status == StatusVerificacaoSebo.PENDENTE) {
            throw new BusinessException("Use VERIFICADO ou REJEITADO", HttpStatus.BAD_REQUEST);
        }
        if (status == StatusVerificacaoSebo.REJEITADO && (motivo == null || motivo.isBlank())) {
            throw new BusinessException("Informe o motivo da rejeicao", HttpStatus.BAD_REQUEST);
        }
        Sebo sebo = validarSebo(seboId);
        List<DocumentoVerificacaoSebo> documentos = documentoRepository.findBySeboIdOrderByEnviadoEmDesc(seboId);
        if (status == StatusVerificacaoSebo.VERIFICADO && documentos.isEmpty()) {
            throw new BusinessException("Envie ao menos um documento antes da aprovacao", HttpStatus.BAD_REQUEST);
        }
        if (status == StatusVerificacaoSebo.VERIFICADO && sebo.getStatusConsultaCnpj() != StatusConsultaCnpj.ATIVA) {
            throw new BusinessException("O CNPJ precisa estar ativo na consulta oficial antes da aprovacao", HttpStatus.BAD_REQUEST);
        }
        StatusVerificacaoSebo anterior = sebo.getStatusVerificacao();
        OffsetDateTime agora = OffsetDateTime.now();
        sebo.setStatusVerificacao(status);
        sebo.setVerificadoEm(status == StatusVerificacaoSebo.VERIFICADO ? agora : null);
        sebo.setMotivoRejeicao(status == StatusVerificacaoSebo.REJEITADO ? motivo.trim() : null);
        sebo.setConfirmado(status == StatusVerificacaoSebo.VERIFICADO);
        documentos.stream().filter(d -> d.getStatus() == StatusDocumentoSebo.PENDENTE).forEach(d -> {
            d.setStatus(status == StatusVerificacaoSebo.VERIFICADO ? StatusDocumentoSebo.APROVADO : StatusDocumentoSebo.REJEITADO);
            d.setAnalisadoEm(agora);
            d.setAnalisadoPor(admin);
            d.setMotivoRejeicao(status == StatusVerificacaoSebo.REJEITADO ? motivo.trim() : null);
        });
        auditoriaRepository.save(AuditoriaVerificacaoSebo.builder().sebo(sebo).analisadoPor(admin)
                .statusAnterior(anterior).novoStatus(status).motivo(status == StatusVerificacaoSebo.REJEITADO ? motivo.trim() : null).build());
        if (status == StatusVerificacaoSebo.REJEITADO) produtoRepository.desativarCatalogoDoSebo(seboId);
        return seboMapper.toResponse(sebo);
    }

    /**
     * MOCK TEMPORARIO (ainda nao existe conta ADMIN para revisar cadastros):
     * aprova o sebo e os documentos pendentes automaticamente, pulando a analise
     * manual normalmente feita em revisar(). Defina app.sebo-verificacao.mock-auto-aprovar=false
     * (ou remova este metodo e as chamadas a ele) quando houver um ADMIN operando.
     */
    private void aprovarAutomaticamenteEnquantoNaoHaAdmin(Sebo sebo) {
        if (!mockAutoAprovarVerificacao) {
            return;
        }
        sebo.setStatusVerificacao(StatusVerificacaoSebo.VERIFICADO);
        sebo.setVerificadoEm(OffsetDateTime.now());
        sebo.setMotivoRejeicao(null);
        sebo.setConfirmado(Boolean.TRUE);
        OffsetDateTime agora = OffsetDateTime.now();
        documentoRepository.findBySeboIdOrderByEnviadoEmDesc(sebo.getId()).stream()
                .filter(d -> d.getStatus() == StatusDocumentoSebo.PENDENTE)
                .forEach(d -> {
                    d.setStatus(StatusDocumentoSebo.APROVADO);
                    d.setAnalisadoEm(agora);
                    d.setMotivoRejeicao(null);
                });
    }

    private Sebo buscarPorAccount(Account account) {
        return seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
    }

    private Sebo validarSebo(Long id) {
        return seboRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado"));
    }

    private DocumentoVerificacaoSeboResponse toResponse(DocumentoVerificacaoSebo d) {
        return toResponse(d, metadataPorUrl(d.getArquivoUrl()));
    }

    private DocumentoVerificacaoSeboResponse toResponse(DocumentoVerificacaoSebo d, DocumentoUpload metadata) {
        return new DocumentoVerificacaoSeboResponse(d.getId(), d.getTipoDocumento(), d.getArquivoUrl(),
                metadata == null ? null : metadata.getNomeOriginal(),
                metadata == null ? null : metadata.getMimeType(),
                metadata == null ? null : metadata.getTamanhoBytes(),
                d.getStatus(), d.getEnviadoEm(), d.getAnalisadoEm(),
                d.getAnalisadoPor() == null ? null : d.getAnalisadoPor().getId(), d.getMotivoRejeicao());
    }

    private DocumentoUpload metadataPorUrl(String url) {
        try {
            return metadataPorNome(extrairNomeInterno(url));
        } catch (BusinessException exception) {
            return null;
        }
    }

    private DocumentoUpload metadataPorNome(String nomeInterno) {
        return uploadRepository.findByNomeInterno(nomeInterno).orElse(null);
    }

    private String extrairNomeInterno(String url) {
        if (url == null || !url.startsWith(PREFIXO_DOCUMENTO)) {
            throw new BusinessException("Use uma URL de documento gerada pelo backend", HttpStatus.BAD_REQUEST);
        }
        String nome = url.substring(PREFIXO_DOCUMENTO.length());
        if (nome.isBlank() || nome.contains("/") || nome.contains("\\") || nome.contains("..")) {
            throw new BusinessException("URL de documento invalida", HttpStatus.BAD_REQUEST);
        }
        return nome;
    }
}
