package com.vitral.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.vitral.dto.MovimentacaoEstoqueResponse;
import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.security.AccountUserDetails;
import com.vitral.service.MovimentacaoEstoqueService;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueControllerTest {

    @Mock
    private MovimentacaoEstoqueService service;

    @InjectMocks
    private MovimentacaoEstoqueController controller;

    @Test
    @DisplayName("Deve converter a pagina de movimentacoes para o contrato estavel da API")
    void listarConvertePageParaContratoEstavel() {
        Account account = account();
        AccountUserDetails principal = new AccountUserDetails(account);
        PageRequest pageable = PageRequest.of(0, 20);
        MovimentacaoEstoqueResponse response = movimentacao(TipoMovimentacaoEstoque.ENTRADA);
        when(service.listar(account, pageable)).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        var result = controller.listar(principal, pageable);

        assertThat(result.getBody().content()).containsExactly(response);
        assertThat(result.getBody().totalElements()).isEqualTo(1);
        verify(service).listar(account, pageable);
    }

    private Account account() {
        return Account.builder().type(AccountType.SEBO).email("sebo@vitral.com").passwordHash("hash").ativo(true).build();
    }

    private MovimentacaoEstoqueResponse movimentacao(TipoMovimentacaoEstoque tipo) {
        return new MovimentacaoEstoqueResponse(40L, 30L, 10L, 1L, null, tipo, 2, 5, 3,
                BigDecimal.valueOf(45), BigDecimal.valueOf(90), "entrada", OffsetDateTime.now());
    }
}
