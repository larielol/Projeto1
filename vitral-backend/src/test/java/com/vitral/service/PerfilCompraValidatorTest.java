package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.vitral.entity.Account;
import com.vitral.exception.BusinessException;

class PerfilCompraValidatorTest {
    private final PerfilCompraValidator validator = new PerfilCompraValidator();

    @Test
    void rejeitaPerfilVazioComCodigoEListaCompleta() {
        assertThatThrownBy(() -> validator.validar(Account.builder().build()))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("PROFILE_INCOMPLETE_FOR_PURCHASE");
                    assertThat(error.getFields()).containsExactly("cpf", "cep", "logradouro", "numero", "bairro",
                            "cidade", "estado");
                });
    }

    @Test
    void rejeitaCpfInvalidoMesmoComEnderecoCompleto() {
        Account account = completo();
        account.setCpf("11111111111");
        assertThatThrownBy(() -> validator.validar(account))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getFields()).containsExactly("cpf"));
    }

    @Test
    void rejeitaUfQueNaoExiste() {
        Account account = completo();
        account.setEstado("XX");
        assertThatThrownBy(() -> validator.validar(account))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getFields()).containsExactly("estado"));
    }

    @Test
    void aceitaPerfilCompletoSemComplemento() {
        assertThatCode(() -> validator.validar(completo())).doesNotThrowAnyException();
    }

    private Account completo() {
        return Account.builder().cpf("52998224725").cep("60160120").logradouro("Rua Silva Jatahy")
                .numero("100").bairro("Meireles").cidade("Fortaleza").estado("CE").build();
    }
}
