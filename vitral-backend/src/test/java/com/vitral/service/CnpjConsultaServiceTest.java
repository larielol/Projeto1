package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.repository.SeboRepository;

@ExtendWith(MockitoExtension.class)
class CnpjConsultaServiceTest {
    @Mock CnpjConsultaClient client;
    @Mock SeboRepository seboRepository;
    @InjectMocks CnpjConsultaService service;

    @Test
    void persisteResultadoAtivoDaConsulta() {
        Account account = Account.builder().build();
        Sebo sebo = Sebo.builder().account(account).cnpj("11222333000181").build();
        setId(account, 1L);
        when(seboRepository.findByAccountId(1L)).thenReturn(Optional.of(sebo));
        when(client.consultar("11222333000181")).thenReturn(
                new CnpjConsultaClient.Resultado("SEBO EXEMPLO LTDA", StatusConsultaCnpj.ATIVA, "CNPJ ativo"));

        var resposta = service.consultarMeuSebo(account);

        assertThat(resposta.status()).isEqualTo(StatusConsultaCnpj.ATIVA);
        assertThat(sebo.getRazaoSocialReceita()).isEqualTo("SEBO EXEMPLO LTDA");
        assertThat(sebo.getCnpjConsultadoEm()).isNotNull();
    }

    private void setId(Object entity, Long id) {
        try {
            var field = com.vitral.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
