package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.vitral.dto.DocumentoVerificacaoSeboRequest;
import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.DocumentoUpload;
import com.vitral.entity.DocumentoVerificacaoSebo;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.enumerations.StatusDocumentoSebo;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.TipoDocumentoSebo;
import com.vitral.exception.BusinessException;
import com.vitral.mapper.SeboMapper;
import com.vitral.repository.AuditoriaVerificacaoSeboRepository;
import com.vitral.repository.DocumentoUploadRepository;
import com.vitral.repository.DocumentoVerificacaoSeboRepository;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.SeboRepository;

@ExtendWith(MockitoExtension.class)
class VerificacaoSeboDocumentoServiceTest {
    @Mock SeboRepository seboRepository;
    @Mock DocumentoVerificacaoSeboRepository documentoRepository;
    @Mock AuditoriaVerificacaoSeboRepository auditoriaRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock SeboMapper seboMapper;
    @Mock DocumentoUploadRepository uploadRepository;
    @Mock DocumentoUploadService documentoUploadService;
    @InjectMocks VerificacaoSeboService service;
    private Account account;
    private Sebo sebo;

    @BeforeEach
    void setup() {
        account = Account.builder().build(); setId(account, 1L);
        sebo = Sebo.builder().account(account).confirmado(true).statusVerificacao(StatusVerificacaoSebo.VERIFICADO).build();
        setId(sebo, 10L);
        lenient().when(seboRepository.findByAccountId(1L)).thenReturn(Optional.of(sebo));
    }

    @Test
    void rejeitaUrlExterna() {
        var request = new DocumentoVerificacaoSeboRequest(TipoDocumentoSebo.CONTRATO_SOCIAL, "https://externo/arquivo.pdf");
        assertThatThrownBy(() -> service.enviarDocumento(account, request)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("URL de documento gerada pelo backend");
    }

    @Test
    void rejeitaArquivoDeOutroSebo() {
        String url = "/api/v1/uploads/documents/arquivo.pdf";
        when(uploadRepository.existsByNomeInternoAndSeboAccountId("arquivo.pdf", 1L)).thenReturn(false);
        var request = new DocumentoVerificacaoSeboRequest(TipoDocumentoSebo.CONTRATO_SOCIAL, url);
        assertThatThrownBy(() -> service.enviarDocumento(account, request)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao pertence");
    }

    @Test
    void enviarDocumentoReabreVerificacaoERemoveConfirmacao() {
        String url = "/api/v1/uploads/documents/arquivo.pdf";
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(TipoDocumentoSebo.CONTRATO_SOCIAL).arquivoUrl(url)
                .status(StatusDocumentoSebo.PENDENTE).build();

        when(uploadRepository.existsByNomeInternoAndSeboAccountId("arquivo.pdf", 1L)).thenReturn(true);
        when(documentoRepository.save(org.mockito.ArgumentMatchers.any(DocumentoVerificacaoSebo.class)))
                .thenReturn(documento);

        service.enviarDocumento(account, new DocumentoVerificacaoSeboRequest(TipoDocumentoSebo.CONTRATO_SOCIAL, url));

        assertThat(sebo.getStatusVerificacao()).isEqualTo(StatusVerificacaoSebo.PENDENTE);
        assertThat(sebo.getVerificadoEm()).isNull();
        assertThat(sebo.getConfirmado()).isFalse();
    }

    @Test
    void enviarDocumentoComMockAtivo_aprovaAutomaticamenteAoInvesDeReabrirPendente() {
        ReflectionTestUtils.setField(service, "mockAutoAprovarVerificacao", true);
        String url = "/api/v1/uploads/documents/arquivo.pdf";
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(TipoDocumentoSebo.CONTRATO_SOCIAL).arquivoUrl(url)
                .status(StatusDocumentoSebo.PENDENTE).build();

        when(uploadRepository.existsByNomeInternoAndSeboAccountId("arquivo.pdf", 1L)).thenReturn(true);
        when(documentoRepository.save(org.mockito.ArgumentMatchers.any(DocumentoVerificacaoSebo.class)))
                .thenReturn(documento);
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L)).thenReturn(List.of(documento));

        service.enviarDocumento(account, new DocumentoVerificacaoSeboRequest(TipoDocumentoSebo.CONTRATO_SOCIAL, url));

        assertThat(sebo.getStatusVerificacao()).isEqualTo(StatusVerificacaoSebo.VERIFICADO);
        assertThat(sebo.getVerificadoEm()).isNotNull();
        assertThat(sebo.getConfirmado()).isTrue();
        assertThat(documento.getStatus()).isEqualTo(StatusDocumentoSebo.APROVADO);
        assertThat(documento.getAnalisadoEm()).isNotNull();
    }

    @Test
    void enviarDocumentoComMockAtivo_aprovaTambemDocumentosAntigosAindaPendentes() {
        ReflectionTestUtils.setField(service, "mockAutoAprovarVerificacao", true);
        String urlNovo = "/api/v1/uploads/documents/novo.pdf";
        DocumentoVerificacaoSebo documentoAntigo1 = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(TipoDocumentoSebo.CARTAO_CNPJ)
                .arquivoUrl("/api/v1/uploads/documents/antigo1.pdf").status(StatusDocumentoSebo.PENDENTE).build();
        DocumentoVerificacaoSebo documentoAntigo2 = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(TipoDocumentoSebo.CARTAO_CNPJ)
                .arquivoUrl("/api/v1/uploads/documents/antigo2.pdf").status(StatusDocumentoSebo.PENDENTE).build();
        DocumentoVerificacaoSebo documentoNovo = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).tipoDocumento(TipoDocumentoSebo.CARTAO_CNPJ).arquivoUrl(urlNovo)
                .status(StatusDocumentoSebo.PENDENTE).build();

        when(uploadRepository.existsByNomeInternoAndSeboAccountId("novo.pdf", 1L)).thenReturn(true);
        when(documentoRepository.save(org.mockito.ArgumentMatchers.any(DocumentoVerificacaoSebo.class)))
                .thenReturn(documentoNovo);
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L))
                .thenReturn(List.of(documentoNovo, documentoAntigo1, documentoAntigo2));

        service.enviarDocumento(account, new DocumentoVerificacaoSeboRequest(TipoDocumentoSebo.CARTAO_CNPJ, urlNovo));

        assertThat(documentoNovo.getStatus()).isEqualTo(StatusDocumentoSebo.APROVADO);
        assertThat(documentoAntigo1.getStatus()).isEqualTo(StatusDocumentoSebo.APROVADO);
        assertThat(documentoAntigo2.getStatus()).isEqualTo(StatusDocumentoSebo.APROVADO);
    }

    @Test
    void enviarDocumentoMultipartSalvaArquivoEMetadados() {
        var arquivo = new MockMultipartFile("arquivo", "cartao-cnpj.pdf", "application/pdf", "%PDF-1.7".getBytes());
        DocumentoUpload metadata = DocumentoUpload.builder().sebo(sebo).nomeInterno("interno.pdf")
                .nomeOriginal("cartao-cnpj.pdf").mimeType("application/pdf").tamanhoBytes(8L).build();
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder().sebo(sebo)
                .tipoDocumento(TipoDocumentoSebo.CARTAO_CNPJ)
                .arquivoUrl("/api/v1/uploads/documents/interno.pdf")
                .status(StatusDocumentoSebo.PENDENTE).build();

        when(documentoUploadService.armazenarComMetadata(account, arquivo)).thenReturn(metadata);
        when(documentoUploadService.arquivoUrl(metadata)).thenReturn("/api/v1/uploads/documents/interno.pdf");
        when(documentoRepository.save(any(DocumentoVerificacaoSebo.class))).thenReturn(documento);

        var response = service.enviarDocumento(account, TipoDocumentoSebo.CARTAO_CNPJ, arquivo);

        assertThat(response.tipoDocumento()).isEqualTo(TipoDocumentoSebo.CARTAO_CNPJ);
        assertThat(response.nomeArquivo()).isEqualTo("cartao-cnpj.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.tamanhoBytes()).isEqualTo(8L);
        assertThat(sebo.getStatusVerificacao()).isEqualTo(StatusVerificacaoSebo.PENDENTE);
        assertThat(sebo.getConfirmado()).isFalse();
    }

    @Test
    void aprovarVerificacaoMarcaSeboComoConfirmado() {
        Account admin = Account.builder().type(AccountType.ADMIN).build();
        setId(admin, 99L);
        Sebo sebo = Sebo.builder().account(account).statusVerificacao(StatusVerificacaoSebo.PENDENTE).statusConsultaCnpj(StatusConsultaCnpj.ATIVA).build();
        setId(sebo, 10L);
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder().sebo(sebo).status(StatusDocumentoSebo.PENDENTE).build();
        setId(documento, 5L);

        when(seboRepository.findById(10L)).thenReturn(Optional.of(sebo));
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L)).thenReturn(List.of(documento));
        when(seboMapper.toResponse(sebo)).thenReturn(new SeboResponse(10L, 1L, "Sebo", "sebo@vitral.com", null, null, null,
                StatusVerificacaoSebo.VERIFICADO, null, null, StatusConsultaCnpj.ATIVA, null, null, null, true));

        service.revisar(admin, 10L, StatusVerificacaoSebo.VERIFICADO, null);

        assertThat(sebo.getConfirmado()).isTrue();
    }

    @Test
    void rejeitarVerificacaoExigeMotivo() {
        Account admin = Account.builder().type(AccountType.ADMIN).build();
        setId(admin, 99L);

        assertThatThrownBy(() -> service.revisar(admin, 10L, StatusVerificacaoSebo.REJEITADO, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void aprovarVerificacaoExigeDocumentoEConsultaCnpjAtiva() {
        Account admin = Account.builder().type(AccountType.ADMIN).build();
        setId(admin, 99L);
        Sebo sebo = Sebo.builder().account(account).statusVerificacao(StatusVerificacaoSebo.PENDENTE)
                .statusConsultaCnpj(StatusConsultaCnpj.INATIVA).build();
        setId(sebo, 10L);

        when(seboRepository.findById(10L)).thenReturn(Optional.of(sebo));
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.revisar(admin, 10L, StatusVerificacaoSebo.VERIFICADO, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ao menos um documento");

        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L))
                .thenReturn(List.of(DocumentoVerificacaoSebo.builder().sebo(sebo).status(StatusDocumentoSebo.PENDENTE).build()));
        assertThatThrownBy(() -> service.revisar(admin, 10L, StatusVerificacaoSebo.VERIFICADO, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CNPJ precisa estar ativo");
    }

    @Test
    void rejeitarVerificacaoMarcaDocumentosEDesativaCatalogo() {
        Account admin = Account.builder().type(AccountType.ADMIN).build();
        setId(admin, 99L);
        Sebo sebo = Sebo.builder().account(account).statusVerificacao(StatusVerificacaoSebo.PENDENTE)
                .statusConsultaCnpj(StatusConsultaCnpj.ATIVA).build();
        setId(sebo, 10L);
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo).status(StatusDocumentoSebo.PENDENTE).build();
        setId(documento, 5L);
        SeboResponse response = new SeboResponse(10L, 1L, "Sebo", "sebo@vitral.com", null, null, null,
                StatusVerificacaoSebo.REJEITADO, "Documento vencido", null, StatusConsultaCnpj.ATIVA, null, null, null, false);

        when(seboRepository.findById(10L)).thenReturn(Optional.of(sebo));
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L)).thenReturn(List.of(documento));
        when(seboMapper.toResponse(sebo)).thenReturn(response);

        var result = service.revisar(admin, 10L, StatusVerificacaoSebo.REJEITADO, " Documento vencido ");

        assertThat(result).isSameAs(response);
        assertThat(sebo.getStatusVerificacao()).isEqualTo(StatusVerificacaoSebo.REJEITADO);
        assertThat(sebo.getMotivoRejeicao()).isEqualTo("Documento vencido");
        assertThat(sebo.getConfirmado()).isFalse();
        assertThat(documento.getStatus()).isEqualTo(StatusDocumentoSebo.REJEITADO);
        assertThat(documento.getMotivoRejeicao()).isEqualTo("Documento vencido");
        verify(produtoRepository).desativarCatalogoDoSebo(10L);
    }

    @Test
    void listarDocumentosIncluiMetadadosDoUpload() {
        DocumentoVerificacaoSebo documento = DocumentoVerificacaoSebo.builder()
                .sebo(sebo)
                .tipoDocumento(TipoDocumentoSebo.CONTRATO_SOCIAL)
                .arquivoUrl("/api/v1/uploads/documents/contrato.pdf")
                .status(StatusDocumentoSebo.APROVADO)
                .enviadoEm(java.time.OffsetDateTime.now())
                .build();
        setId(documento, 7L);
        DocumentoUpload metadata = DocumentoUpload.builder().sebo(sebo).nomeInterno("contrato.pdf")
                .nomeOriginal("Contrato Social.pdf").mimeType("application/pdf").tamanhoBytes(123L).build();

        when(seboRepository.findById(10L)).thenReturn(Optional.of(sebo));
        when(documentoRepository.findBySeboIdOrderByEnviadoEmDesc(10L)).thenReturn(List.of(documento));
        when(uploadRepository.findByNomeInterno("contrato.pdf")).thenReturn(Optional.of(metadata));

        var result = service.listarDocumentos(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nomeArquivo()).isEqualTo("Contrato Social.pdf");
        assertThat(result.get(0).contentType()).isEqualTo("application/pdf");
        assertThat(result.get(0).tamanhoBytes()).isEqualTo(123L);
    }

    private void setId(Object entity, Long id) {
        try {
            var field = com.vitral.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
