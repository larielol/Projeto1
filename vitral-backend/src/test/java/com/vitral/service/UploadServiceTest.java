package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve salvar imagem e retornar URL publica")
    void storeImage_sucesso() throws Exception {
        UploadService service = new UploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "capa.png", "image/png", "imagem".getBytes());

        String url = service.storeImage(file);

        assertThat(url).startsWith("/api/v1/uploads/images/").endsWith(".png");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
        assertThat(service.loadImage(filename).exists()).isTrue();
        assertThat(service.contentType(filename)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo vazio")
    void storeImage_vazio_lancaErro() {
        UploadService service = new UploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "vazio.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storeImage(file))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Deve rejeitar formato diferente de imagem")
    void storeImage_formatoInvalido_lancaErro() {
        UploadService service = new UploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "nota.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> service.storeImage(file))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).contains("Formato de imagem invalido"));
    }

    @Test
    @DisplayName("Deve rejeitar imagem maior que 5MB")
    void storeImage_tamanhoExcedido_lancaErro() {
        UploadService service = new UploadService(tempDir.toString());
        byte[] content = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "grande.jpg", "image/jpeg", content);

        assertThatThrownBy(() -> service.storeImage(file))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).contains("maximo 5MB");
                });
    }

    @Test
    @DisplayName("Deve rejeitar consulta de imagem inexistente")
    void loadImage_arquivoInexistente_lancaErro() {
        UploadService service = new UploadService(tempDir.toString());

        assertThatThrownBy(() -> service.loadImage("inexistente.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Imagem nao encontrada");
    }

    @Test
    @DisplayName("Deve impedir acesso a arquivo fora do diretorio de imagens")
    void loadImage_pathTraversal_lancaErro() {
        UploadService service = new UploadService(tempDir.toString());

        assertThatThrownBy(() -> service.loadImage("../segredo.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve identificar os tipos de imagem suportados")
    void contentType_retornaMimeTypeCorreto() {
        UploadService service = new UploadService(tempDir.toString());

        assertThat(service.contentType("imagem.jpg")).isEqualTo("image/jpeg");
        assertThat(service.contentType("imagem.JPEG")).isEqualTo("image/jpeg");
        assertThat(service.contentType("imagem.png")).isEqualTo("image/png");
        assertThat(service.contentType("imagem.webp")).isEqualTo("image/webp");
        assertThat(service.contentType("imagem.bin")).isEqualTo("application/octet-stream");
    }
}
