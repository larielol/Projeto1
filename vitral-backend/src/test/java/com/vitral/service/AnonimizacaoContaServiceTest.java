package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.repository.ProdutoRepository;
import com.vitral.repository.SeboRepository;

@ExtendWith(MockitoExtension.class)
class AnonimizacaoContaServiceTest {

    @Mock com.vitral.repository.RecomendacaoEventoRepository recomendacaoEventoRepository;
    @Mock SeboRepository seboRepository;
    @Mock ProdutoRepository produtoRepository;
    @InjectMocks AnonimizacaoContaService service;

    @Test
    void anonimizaUsuarioEExcluiHistoricoDeRecomendacao() {
        Account account = Account.builder().name("Maria").email("maria@email.com").passwordHash("hash").authVersion(3).build();
        service.anonimizarUsuario(account);
        assertThat(account.getAtivo()).isFalse();
        assertThat(account.getEmail()).endsWith("@invalid.local");
        assertThat(account.getAuthVersion()).isEqualTo(4);
        verify(recomendacaoEventoRepository).deleteByAccountId(account.getId());
    }

    @Test
    void anonimizaSeboEDesativaCatalogo() {
        Account account = Account.builder().name("Sebo").email("sebo@email.com").passwordHash("hash").build();
        Sebo sebo = Sebo.builder().account(account).cnpj("12345678000199").telefone("9999")
                .statusVerificacao(StatusVerificacaoSebo.VERIFICADO).build();
        setId(account, 5L); setId(sebo, 8L);
        when(seboRepository.findByAccountId(5L)).thenReturn(Optional.of(sebo));
        service.anonimizarSebo(account);
        verify(produtoRepository).desativarCatalogoDoSebo(8L);
        assertThat(sebo.getCnpj()).isNull();
        assertThat(sebo.getStatusVerificacao()).isEqualTo(StatusVerificacaoSebo.REJEITADO);
        assertThat(account.getAtivo()).isFalse();
    }

    private void setId(Object entity, Long id) {
        try {
            var field = com.vitral.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true); field.set(entity, id);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
