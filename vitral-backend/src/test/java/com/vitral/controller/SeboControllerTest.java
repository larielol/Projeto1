package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.vitral.dto.ConsultaCnpjResponse;
import com.vitral.dto.DocumentoVerificacaoSeboResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.dto.RevisaoVerificacaoSeboRequest;
import com.vitral.dto.SeboRequest;
import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.enumerations.StatusDocumentoSebo;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.TipoDocumentoSebo;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.CnpjConsultaService;
import com.vitral.service.SeboService;
import com.vitral.service.VerificacaoSeboService;

@ExtendWith(MockitoExtension.class)
class SeboControllerTest {

    @Mock
    private SeboService seboService;

    @Mock
    private VerificacaoSeboService verificacaoSeboService;

    @Mock
    private CnpjConsultaService cnpjConsultaService;

    @InjectMocks
    private SeboController controller;

    @Test
    void criarRetornaCreatedComEnderecoObrigatorio() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        SeboRequest request = new SeboRequest("Sebo online", "85999999999", "12345678000195", null, "58000000", "Rua das Letras", "Joao Pessoa", "PB");
        SeboResponse response = seboResponse(false);
        when(seboService.criar(account, request)).thenReturn(response);

        var result = controller.criar(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(seboService).criar(account, request);
    }

    @Test
    void revisarVerificacaoUsaMotivoDoCorpoQuandoInformado() {
        Account admin = account(AccountType.ADMIN);
        AccountUserDetails principal = new AccountUserDetails(admin);
        SeboResponse response = seboResponse(true);
        when(verificacaoSeboService.revisar(admin, 10L, StatusVerificacaoSebo.REJEITADO, "Documento ilegivel"))
                .thenReturn(response);

        var result = controller.revisarVerificacao(10L, StatusVerificacaoSebo.REJEITADO, "motivo antigo",
                new RevisaoVerificacaoSeboRequest("Documento ilegivel"), principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(verificacaoSeboService).revisar(admin, 10L, StatusVerificacaoSebo.REJEITADO, "Documento ilegivel");
    }

    @Test
    void enviarDocumentoMultipartRetornaCreatedComMetadados() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "cnpj.pdf", "application/pdf", "%PDF-1.7".getBytes());
        DocumentoVerificacaoSeboResponse response = documentoResponse();
        when(verificacaoSeboService.enviarDocumento(account, TipoDocumentoSebo.CARTAO_CNPJ, arquivo)).thenReturn(response);

        var result = controller.enviarDocumentoMultipart(principal, TipoDocumentoSebo.CARTAO_CNPJ, arquivo);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(verificacaoSeboService).enviarDocumento(account, TipoDocumentoSebo.CARTAO_CNPJ, arquivo);
    }

    @Test
    void listarDocumentosConsultaMeusEPorSebo() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        List<DocumentoVerificacaoSeboResponse> documentos = List.of(documentoResponse());
        when(verificacaoSeboService.listarMeusDocumentos(account)).thenReturn(documentos);
        when(verificacaoSeboService.listarDocumentos(10L)).thenReturn(documentos);

        assertThat(controller.listarMeusDocumentos(principal).getBody()).isSameAs(documentos);
        assertThat(controller.listarDocumentos(10L).getBody()).isSameAs(documentos);
        verify(verificacaoSeboService).listarMeusDocumentos(account);
        verify(verificacaoSeboService).listarDocumentos(10L);
    }

    @Test
    void consultarCnpjEExcluirContaDelegamParaServicos() {
        Account account = account(AccountType.SEBO);
        AccountUserDetails principal = new AccountUserDetails(account);
        ConsultaCnpjResponse consulta = new ConsultaCnpjResponse("12345678000195", "Vitral LTDA",
                StatusConsultaCnpj.ATIVA, OffsetDateTime.now(), "CNPJ ativo");
        MensagemResponse mensagem = new MensagemResponse("Conta excluida com sucesso");
        when(cnpjConsultaService.consultarMeuSebo(account)).thenReturn(consulta);
        when(seboService.excluirConta(account)).thenReturn(mensagem);

        assertThat(controller.consultarCnpj(principal).getBody()).isSameAs(consulta);
        assertThat(controller.excluirConta(principal).getBody()).isSameAs(mensagem);
        verify(cnpjConsultaService).consultarMeuSebo(account);
        verify(seboService).excluirConta(account);
    }

    @Test
    void listarPendentesConvertePageParaContratoEstavel() {
        var pageable = PageRequest.of(0, 5);
        SeboResponse sebo = seboResponse(false);
        when(seboService.listarPendentes(pageable)).thenReturn(new PageImpl<>(List.of(sebo), pageable, 1));

        var result = controller.listarPendentes(pageable);

        assertThat(result.getBody().content()).containsExactly(sebo);
        assertThat(result.getBody().totalElements()).isEqualTo(1);
    }

    private Account account(AccountType type) {
        return Account.builder().type(type).email("user@vitral.com").passwordHash("hash").ativo(true).build();
    }

    private SeboResponse seboResponse(boolean confirmado) {
        return new SeboResponse(10L, 1L, "Sebo Vitral", "sebo@vitral.com", "Descricao", "85999999999",
                "12345678000195", StatusVerificacaoSebo.PENDENTE, null, "Vitral LTDA",
                StatusConsultaCnpj.ATIVA, OffsetDateTime.now(), "CNPJ ativo", null, confirmado);
    }

    private DocumentoVerificacaoSeboResponse documentoResponse() {
        return new DocumentoVerificacaoSeboResponse(5L, TipoDocumentoSebo.CARTAO_CNPJ,
                "/api/v1/uploads/documents/cnpj.pdf", "cnpj.pdf", "application/pdf", 8L,
                StatusDocumentoSebo.PENDENTE, OffsetDateTime.now(), null, null, null);
    }
}
