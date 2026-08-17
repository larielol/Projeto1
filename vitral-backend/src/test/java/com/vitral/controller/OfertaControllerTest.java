package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.vitral.dto.OfertaRequest;
import com.vitral.dto.OfertaResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.OfertaService;

@ExtendWith(MockitoExtension.class)
class OfertaControllerTest {

    @Mock
    private OfertaService ofertaService;

    @InjectMocks
    private OfertaController controller;

    @Test
    void listarAtivasEMinhasConvertemPaginacao() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        var pageable = PageRequest.of(0, 10);
        OfertaResponse oferta = oferta();
        when(ofertaService.listarAtivas(pageable)).thenReturn(new PageImpl<>(List.of(oferta), pageable, 1));
        when(ofertaService.listarDoSebo(account, pageable)).thenReturn(new PageImpl<>(List.of(oferta), pageable, 1));

        assertThat(controller.listarAtivas(pageable).getBody().content()).containsExactly(oferta);
        assertThat(controller.listarMinhas(principal, pageable).getBody().content()).containsExactly(oferta);
        verify(ofertaService).listarAtivas(pageable);
        verify(ofertaService).listarDoSebo(account, pageable);
    }

    @Test
    void criarAtualizarERemoverOfertaDoSebo() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        OfertaRequest request = new OfertaRequest(30L, BigDecimal.valueOf(39), "Promocao",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(5), true);
        OfertaResponse response = oferta();
        when(ofertaService.criar(account, request)).thenReturn(response);
        when(ofertaService.atualizar(account, 50L, request)).thenReturn(response);

        assertThat(controller.criar(principal, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.atualizar(principal, 50L, request).getBody()).isSameAs(response);
        assertThat(controller.remover(principal, 50L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(ofertaService).criar(account, request);
        verify(ofertaService).atualizar(account, 50L, request);
        verify(ofertaService).remover(account, 50L);
    }

    private Account account() {
        return Account.builder().type(AccountType.SEBO).email("sebo@vitral.com").passwordHash("hash").ativo(true).build();
    }

    private OfertaResponse oferta() {
        return new OfertaResponse(50L, 30L, 10L, "Dom Casmurro", BigDecimal.valueOf(45),
                BigDecimal.valueOf(39), "Promocao", OffsetDateTime.now(), OffsetDateTime.now().plusDays(5), true);
    }
}
