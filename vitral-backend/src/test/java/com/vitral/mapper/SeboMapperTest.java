package com.vitral.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;

class SeboMapperTest {

    private final SeboMapper mapper = Mappers.getMapper(SeboMapper.class);

    @Test
    void toResponse_retornaDataCriacaoEUltimaAtividade() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        ReflectionTestUtils.setField(account, "id", 7L);
        OffsetDateTime dataCriacao = OffsetDateTime.now().minusDays(10);
        OffsetDateTime ultimaAtividade = OffsetDateTime.now().minusHours(1);
        Sebo sebo = Sebo.builder().account(account).build();
        ReflectionTestUtils.setField(sebo, "id", 3L);
        sebo.setDataCriacao(dataCriacao);
        sebo.setUltimaAtividade(ultimaAtividade);

        SeboResponse response = mapper.toResponse(sebo);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.accountId()).isEqualTo(7L);
        assertThat(response.dataCriacao()).isEqualTo(dataCriacao);
        assertThat(response.ultimaAtividade()).isEqualTo(ultimaAtividade);
    }

    @Test
    void toResponse_retornaCnpjCompletoSemMascaraParaODonoDoSebo() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        Sebo sebo = Sebo.builder().account(account).cnpj("83877580000127").build();

        SeboResponse response = mapper.toResponse(sebo);

        assertThat(response.cnpj()).isEqualTo("83877580000127");
    }

    @Test
    void toResponseComDistancia_retornaCnpjCompletoSemMascaraParaODonoDoSebo() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        Sebo sebo = Sebo.builder().account(account).cnpj("83877580000127").build();

        SeboResponse response = mapper.toResponse(sebo, 12.3);

        assertThat(response.cnpj()).isEqualTo("83877580000127");
    }

    @Test
    void toPublicResponse_mascaraOCnpjParaVisitantes() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        Sebo sebo = Sebo.builder().account(account).cnpj("83877580000127").build();

        SeboResponse response = mapper.toPublicResponse(sebo);

        assertThat(response.cnpj()).isEqualTo("**.***.***/****-27");
    }

    @Test
    void toPublicResponseComDistancia_mascaraOCnpjParaVisitantes() {
        Account account = Account.builder().name("Sebo").email("sebo@vitral.com").type(AccountType.SEBO).build();
        Sebo sebo = Sebo.builder().account(account).cnpj("83877580000127").build();

        SeboResponse response = mapper.toPublicResponse(sebo, 5.0);

        assertThat(response.cnpj()).isEqualTo("**.***.***/****-27");
    }
}
