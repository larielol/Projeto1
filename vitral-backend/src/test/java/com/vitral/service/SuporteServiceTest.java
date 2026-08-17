package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.SuporteRequest;
import com.vitral.dto.SuporteResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Suporte;
import com.vitral.enumerations.AccountType;
import com.vitral.repository.SuporteRepository;

@ExtendWith(MockitoExtension.class)
class SuporteServiceTest {

    @Mock
    private SuporteRepository suporteRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SuporteService suporteService;

    private Account remetente() {
        Account account = Account.builder()
                .name("Joao")
                .email("joao@email.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build();
        ReflectionTestUtils.setField(account, "id", 7L);
        return account;
    }

    @Test
    @DisplayName("Deve salvar o chamado e notificar a equipe por e-mail")
    void enviar_sucesso_salvaENotificaEquipe() {
        Account remetente = remetente();
        SuporteRequest request = new SuporteRequest("Duvida sobre reserva", "Como faco para reservar?");
        when(suporteRepository.save(any(Suporte.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuporteResponse response = suporteService.enviar(remetente, request);

        assertThat(response.assunto()).isEqualTo("Duvida sobre reserva");
        assertThat(response.status()).isEqualTo("ABERTO");
        verify(emailService).enviarMensagemSuporte(
                "Joao", "joao@email.com", "Duvida sobre reserva", "Como faco para reservar?");
    }

    @Test
    @DisplayName("Deve salvar o chamado mesmo quando a notificacao por e-mail falha")
    void enviar_falhaNaNotificacao_naoInterrompeOChamado() {
        Account remetente = remetente();
        SuporteRequest request = new SuporteRequest("Assunto", "Mensagem de teste");
        when(suporteRepository.save(any(Suporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp indisponivel")).when(emailService)
                .enviarMensagemSuporte("Joao", "joao@email.com", "Assunto", "Mensagem de teste");

        SuporteResponse response = suporteService.enviar(remetente, request);

        assertThat(response.assunto()).isEqualTo("Assunto");
        verify(suporteRepository).save(any(Suporte.class));
    }
}
