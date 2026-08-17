package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.vitral.dto.CategoriaRequest;
import com.vitral.dto.CategoriaResponse;
import com.vitral.service.CategoriaService;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController controller;

    @Test
    void listarConvertePageParaContratoEstavel() {
        var pageable = PageRequest.of(0, 10);
        CategoriaResponse categoria = categoria();
        when(categoriaService.listar(pageable)).thenReturn(new PageImpl<>(List.of(categoria), pageable, 1));

        var result = controller.listar(pageable);

        assertThat(result.getBody().content()).containsExactly(categoria);
        assertThat(result.getBody().totalElements()).isEqualTo(1);
        verify(categoriaService).listar(pageable);
    }

    @Test
    void criarRetornaCreated() {
        CategoriaRequest request = new CategoriaRequest("Literatura", "Livros literarios");
        CategoriaResponse response = categoria();
        when(categoriaService.criar(request)).thenReturn(response);

        var result = controller.criar(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(categoriaService).criar(request);
    }

    private CategoriaResponse categoria() {
        return new CategoriaResponse(5L, "Literatura", "literatura", "Livros literarios");
    }
}
