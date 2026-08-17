package com.vitral.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;

class ProdutoMapperTest {

    private final ProdutoMapper mapper = Mappers.getMapper(ProdutoMapper.class);

    @Test
    void toResponse_retornaDataPublicacaoAPartirDoCreatedAt() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        Sebo sebo = Sebo.builder().account(account).build();
        ReflectionTestUtils.setField(sebo, "id", 10L);
        OffsetDateTime dataPublicacao = OffsetDateTime.now().minusDays(2);
        Produto produto = Produto.builder()
                .sebo(sebo)
                .titulo("Livro")
                .preco(BigDecimal.TEN)
                .estoque(1)
                .condicao(CondicaoProduto.USADO)
                .ativo(true)
                .build();
        ReflectionTestUtils.setField(produto, "id", 99L);
        ReflectionTestUtils.setField(produto, "createdAt", dataPublicacao);

        ProdutoResponse response = mapper.toResponse(produto, null);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.seboId()).isEqualTo(10L);
        assertThat(response.dataPublicacao()).isEqualTo(dataPublicacao);
    }
}
