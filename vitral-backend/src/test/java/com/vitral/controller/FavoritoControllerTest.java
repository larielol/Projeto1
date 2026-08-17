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

import com.vitral.dto.FavoritoResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.FavoritoService;

@ExtendWith(MockitoExtension.class)
class FavoritoControllerTest {

    @Mock
    private FavoritoService favoritoService;

    @InjectMocks
    private FavoritoController controller;

    @Test
    void favoritarRetornaCreated() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        MensagemResponse response = new MensagemResponse("Produto favoritado");
        when(favoritoService.favoritar(account, 30L)).thenReturn(response);

        var result = controller.favoritar(principal, 30L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(favoritoService).favoritar(account, 30L);
    }

    @Test
    void listarERemoverFavoritos() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        List<FavoritoResponse> favoritos = List.of(new FavoritoResponse(1L, 30L, "Dom Casmurro",
                "Machado de Assis", BigDecimal.valueOf(45), null, CondicaoProduto.USADO, null));
        when(favoritoService.listarFavoritos(account)).thenReturn(favoritos);

        assertThat(controller.listarFavoritos(principal).getBody()).containsExactlyElementsOf(favoritos);
        assertThat(controller.removerFavorito(principal, 30L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(favoritoService).listarFavoritos(account);
        verify(favoritoService).removerFavorito(account, 30L);
    }

    private Account account() {
        return Account.builder().type(AccountType.USUARIO).email("user@vitral.com").passwordHash("hash").ativo(true).build();
    }
}
