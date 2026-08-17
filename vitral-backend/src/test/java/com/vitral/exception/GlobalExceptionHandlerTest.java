package com.vitral.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void erroInesperadoRetornaMensagemSegura() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/teste");

        var response = handler.handleUnexpected(
                new IllegalStateException("detalhe interno sensivel"),
                new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("sensivel");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/teste");
    }
}
