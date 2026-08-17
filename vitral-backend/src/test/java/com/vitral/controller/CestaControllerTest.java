package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.vitral.dto.CestaItemResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.CestaService;

@ExtendWith(MockitoExtension.class)
class CestaControllerTest {

    @Mock
    private CestaService cestaService;

    @InjectMocks
    private CestaController controller;

    @Test
    void adicionarItemRetornaCreated() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        MensagemResponse response = new MensagemResponse("Item adicionado");
        when(cestaService.adicionarItem(account, 30L, 2)).thenReturn(response);

        var result = controller.adicionarItem(principal, 30L, 2);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(cestaService).adicionarItem(account, 30L, 2);
    }

    @Test
    void atualizarListarERemoverItens() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        MensagemResponse mensagem = new MensagemResponse("Quantidade atualizada");
        List<CestaItemResponse> itens = List.of(item());
        when(cestaService.atualizarQuantidade(account, 30L, 3)).thenReturn(mensagem);
        when(cestaService.listarCesta(account)).thenReturn(itens);

        assertThat(controller.atualizarQuantidade(principal, 30L, 3).getBody()).isSameAs(mensagem);
        assertThat(controller.listarCesta(principal).getBody()).containsExactlyElementsOf(itens);
        assertThat(controller.removerItem(principal, 30L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cestaService).atualizarQuantidade(account, 30L, 3);
        verify(cestaService).listarCesta(account);
        verify(cestaService).removerItem(account, 30L);
    }

    private Account account() {
        return Account.builder().type(AccountType.USUARIO).email("user@vitral.com").passwordHash("hash").ativo(true).build();
    }

    private CestaItemResponse item() {
        return new CestaItemResponse(1L, 30L, "Dom Casmurro", "Machado de Assis", BigDecimal.valueOf(45),
                BigDecimal.valueOf(50), 2, BigDecimal.valueOf(90), 5, CondicaoProduto.USADO, null);
    }
}
