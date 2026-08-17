package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.RecomendacaoHistoricoService;
import com.vitral.service.RecomendacaoService;

@ExtendWith(MockitoExtension.class)
class RecomendacaoControllerTest {
    @Mock RecomendacaoService recomendacaoService;
    @Mock RecomendacaoHistoricoService historicoService;
    @InjectMocks RecomendacaoController controller;

    @Test void listaSempreUsaUsuarioAutenticadoComoFonteDeVerdade() {
        Account account = Account.builder().type(AccountType.USUARIO).build();
        AccountUserDetails principal = new AccountUserDetails(account);
        PageRequest pageable = PageRequest.of(0, 20);
        when(recomendacaoService.recomendar(account, pageable, Set.of()))
                .thenReturn(new PageImpl<com.vitral.dto.ProdutoResponse>(List.of()));
        assertThat(controller.listar(principal, pageable).getBody().content()).isEmpty();
        verify(recomendacaoService).recomendar(account, pageable, Set.of());
    }

    @Test void limpaHistoricoDoUsuarioAutenticado() {
        Account account = Account.builder().type(AccountType.USUARIO).build();
        AccountUserDetails principal = new AccountUserDetails(account);
        when(historicoService.limpar(account)).thenReturn(new MensagemResponse("ok"));
        assertThat(controller.limparHistorico(principal).getBody().mensagem()).isEqualTo("ok");
        verify(historicoService).limpar(account);
    }
}
