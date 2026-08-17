package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.vitral.dto.UploadResponse;
import com.vitral.service.UploadService;
import com.vitral.service.DocumentoUploadService;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.security.AccountUserDetails;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock
    private UploadService uploadService;

    @Mock
    private DocumentoUploadService documentoUploadService;

    @InjectMocks
    private UploadController uploadController;

    @Test
    @DisplayName("Deve retornar a URL publica ao enviar uma imagem")
    void uploadImage_retornaUrlPublica() {
        MockMultipartFile file = new MockMultipartFile("file", "capa.png", "image/png", "imagem".getBytes());
        String url = "/api/v1/uploads/images/arquivo.png";
        when(uploadService.storeImage(file)).thenReturn(url);

        var response = uploadController.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new UploadResponse(url));
        verify(uploadService).storeImage(file);
    }

    @Test
    @DisplayName("Deve retornar imagem publica com cache e tipo de conteudo")
    void getImage_retornaImagemComHeaders() {
        String filename = "arquivo.webp";
        ByteArrayResource image = new ByteArrayResource("imagem".getBytes());
        when(uploadService.loadImage(filename)).thenReturn(image);
        when(uploadService.contentType(filename)).thenReturn("image/webp");

        var response = uploadController.getImage(filename);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/webp");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("public, max-age=31536000, immutable");
        assertThat(response.getBody()).isSameAs(image);
        verify(uploadService).loadImage(filename);
        verify(uploadService).contentType(filename);
    }

    @Test
    void uploadDocument_retornaUrlPrivada() {
        Account account = Account.builder().type(AccountType.SEBO).build();
        AccountUserDetails principal = new AccountUserDetails(account);
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "%PDF-1.7".getBytes());
        String url = "/api/v1/uploads/documents/documento.pdf";
        when(documentoUploadService.armazenar(account, file)).thenReturn(url);

        var response = uploadController.uploadDocument(principal, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new UploadResponse(url));
        verify(documentoUploadService).armazenar(account, file);
    }

    @Test
    void getDocument_impedeCachePublico() {
        Account account = Account.builder().type(AccountType.ADMIN).build();
        AccountUserDetails principal = new AccountUserDetails(account);
        String filename = "documento.pdf";
        ByteArrayResource document = new ByteArrayResource("%PDF-1.7".getBytes());
        when(documentoUploadService.carregar(account, filename)).thenReturn(document);
        when(documentoUploadService.contentType(filename)).thenReturn("application/pdf");

        var response = uploadController.getDocument(principal, filename);

        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("private, no-store");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("inline");
    }
}
