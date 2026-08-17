package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.MensagemRequest;
import com.vitral.entity.Account;
import com.vitral.entity.Mensagem;
import com.vitral.enumerations.AccountType;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.AccountRepository;
import com.vitral.repository.MensagemRepository;

@ExtendWith(MockitoExtension.class)
class MensagemServiceTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private MensagemService mensagemService;

    @Test
    @DisplayName("Deve enviar mensagem entre contas diferentes")
    void enviar_sucesso_salvaMensagem() {
        Account remetente = account(1L, "Usuario", AccountType.USUARIO);
        Account destinatario = account(2L, "Sebo", AccountType.SEBO);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinatario));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> {
            Mensagem mensagem = invocation.getArgument(0);
            ReflectionTestUtils.setField(mensagem, "id", 10L);
            return mensagem;
        });

        var response = mensagemService.enviar(remetente, new MensagemRequest(2L, "O livro esta disponivel?"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.remetenteId()).isEqualTo(1L);
        assertThat(response.destinatarioId()).isEqualTo(2L);
        assertThat(response.conteudo()).isEqualTo("O livro esta disponivel?");
        assertThat(response.lida()).isFalse();
    }

    @Test
    @DisplayName("Deve impedir mensagem enviada para a propria conta")
    void enviar_paraSiMesmo_lancaBadRequest() {
        Account account = account(1L, "Usuario", AccountType.USUARIO);

        assertThatThrownBy(() -> mensagemService.enviar(account, new MensagemRequest(1L, "Teste")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve informar destinatario inexistente")
    void enviar_destinatarioInexistente_lancaNotFound() {
        Account remetente = account(1L, "Usuario", AccountType.USUARIO);
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensagemService.enviar(remetente, new MensagemRequest(99L, "Ola")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar conversa e marcar mensagens recebidas como lidas")
    void listarConversaCom_marcaRecebidasComoLidas() {
        Account usuario = account(1L, "Usuario", AccountType.USUARIO);
        Account sebo = account(2L, "Sebo", AccountType.SEBO);
        Mensagem recebida = Mensagem.builder()
                .remetente(sebo)
                .destinatario(usuario)
                .conteudo("Sim, esta disponivel")
                .lida(false)
                .build();
        Mensagem enviada = Mensagem.builder()
                .remetente(usuario)
                .destinatario(sebo)
                .conteudo("Obrigado")
                .lida(false)
                .build();
        var pageable = PageRequest.of(0, 20);
        when(mensagemRepository.findConversaComConta(1L, 2L, pageable))
                .thenReturn(new PageImpl<>(List.of(recebida, enviada), pageable, 2));

        var resultado = mensagemService.listarConversaCom(usuario, 2L, pageable);

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(recebida.getLida()).isTrue();
        assertThat(enviada.getLida()).isFalse();
    }

    private Account account(Long id, String nome, AccountType type) {
        Account account = Account.builder()
                .name(nome)
                .email(nome.toLowerCase() + "@email.com")
                .passwordHash("hash")
                .type(type)
                .emailVerificado(true)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
