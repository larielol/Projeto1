package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.CategoriaRequest;
import com.vitral.entity.Categoria;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    @DisplayName("Deve criar categoria normalizando nome acentuado para slug")
    void criar_sucesso_geraSlugNormalizado() {
        CategoriaRequest request = new CategoriaRequest(" Livros ", "Livros");
        when(categoriaRepository.existsBySlug("livros")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            ReflectionTestUtils.setField(categoria, "id", 1L);
            return categoria;
        });

        var response = categoriaService.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Livros");
        assertThat(response.slug()).isEqualTo("livros");
    }

    @Test
    @DisplayName("Deve rejeitar categoria com slug ja cadastrado")
    void criar_slugDuplicado_lancaConflictException() {
        when(categoriaRepository.existsBySlug("livros")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaRequest("Livros", null)))
                .isInstanceOf(ConflictException.class);

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar categorias paginadas")
    void listar_retornaCategorias() {
        Categoria categoria = Categoria.builder().nome("Livros").slug("livros").build();
        ReflectionTestUtils.setField(categoria, "id", 2L);
        var pageable = PageRequest.of(0, 10);
        when(categoriaRepository.findPermitidas(CategoriaService.SLUGS_PERMITIDOS, pageable))
                .thenReturn(new PageImpl<>(List.of(categoria), pageable, 1));

        var resultado = categoriaService.listar(pageable);

        assertThat(resultado.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(2L);
            assertThat(item.slug()).isEqualTo("livros");
        });
    }

    @Test
    @DisplayName("Deve informar categoria inexistente")
    void buscarEntidade_inexistente_lancaNotFound() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarEntidade(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
