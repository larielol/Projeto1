package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.entity.Oferta;
import com.vitral.entity.Produto;
import com.vitral.repository.OfertaRepository;

@ExtendWith(MockitoExtension.class)
class PrecoServiceTest {

    @Mock
    private OfertaRepository ofertaRepository;

    @InjectMocks
    private PrecoService service;

    @Test
    void precoPromocionalVigenteRetornaOfertaQuandoExiste() {
        Produto produto = produto(10L, "50.00");
        Oferta oferta = Oferta.builder().produto(produto).precoPromocional(new BigDecimal("39.90")).build();
        when(ofertaRepository.findVigenteByProdutoId(org.mockito.ArgumentMatchers.eq(10L), any()))
                .thenReturn(Optional.of(oferta));

        assertThat(service.precoPromocionalVigente(produto)).contains(new BigDecimal("39.90"));
        assertThat(service.precoEfetivo(produto)).isEqualByComparingTo("39.90");
    }

    @Test
    void precoEfetivoUsaPrecoOriginalSemOferta() {
        Produto produto = produto(10L, "50.00");
        when(ofertaRepository.findVigenteByProdutoId(org.mockito.ArgumentMatchers.eq(10L), any()))
                .thenReturn(Optional.empty());

        assertThat(service.precoEfetivo(produto)).isEqualByComparingTo("50.00");
    }

    @Test
    void precosPromocionaisRetornaMapaVazioSemIds() {
        assertThat(service.precosPromocionaisVigentes(List.of())).isEmpty();
        verify(ofertaRepository, never()).findVigentesByProdutoIds(any(), any());
    }

    @Test
    void precosPromocionaisMantemPrimeiraOfertaQuandoHaDuplicidade() {
        Produto produto = produto(10L, "50.00");
        Oferta primeira = Oferta.builder().produto(produto).precoPromocional(new BigDecimal("39.90")).build();
        Oferta segunda = Oferta.builder().produto(produto).precoPromocional(new BigDecimal("35.00")).build();
        when(ofertaRepository.findVigentesByProdutoIds(org.mockito.ArgumentMatchers.eq(List.of(10L)), any()))
                .thenReturn(List.of(primeira, segunda));

        var result = service.precosPromocionaisVigentes(List.of(10L));

        assertThat(result).containsEntry(10L, new BigDecimal("39.90"));
    }

    private Produto produto(Long id, String preco) {
        Produto produto = Produto.builder().preco(new BigDecimal(preco)).build();
        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }
}
