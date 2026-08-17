package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.vitral.entity.Account;
import com.vitral.entity.BaseEntity;
import com.vitral.repository.RecomendacaoEventoRepository;

@ExtendWith(MockitoExtension.class)
class RecomendacaoHistoricoServiceTest {
    @Mock RecomendacaoEventoRepository repository;
    @InjectMocks RecomendacaoHistoricoService service;

    @Test void usuarioLimpaSomenteOProprioHistorico() throws Exception {
        Account account = Account.builder().build();
        Field id = BaseEntity.class.getDeclaredField("id"); id.setAccessible(true); id.set(account, 42L);
        assertThat(service.limpar(account).mensagem()).contains("removido");
        verify(repository).deleteByAccountId(42L);
    }
}
