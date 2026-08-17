package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void naoEnviaQuandoEmailEstaDesabilitadoOuSemSender() {
        EmailService desabilitado = service(mailSender, false);
        desabilitado.enviarConfirmacaoCadastro("user@vitral.com", "token");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        EmailService semSender = service(null, true);
        semSender.enviarConfirmacaoCadastro("user@vitral.com", "token");
    }

    @Test
    void enviaConfirmacaoCadastroComLinkDoFrontend() {
        EmailService service = service(mailSender, true);

        service.enviarConfirmacaoCadastro("user@vitral.com", "abc");

        SimpleMailMessage mensagem = capturarMensagem();
        assertThat(mensagem.getTo()).containsExactly("user@vitral.com");
        assertThat(mensagem.getSubject()).contains("Confirme");
        assertThat(mensagem.getText()).contains("https://app.vitral.test/auth/confirmar?token=abc");
    }

    @Test
    void enviaMensagemSuporteComReplyTo() {
        EmailService service = service(mailSender, true);

        service.enviarMensagemSuporte("Rute", "rute@vitral.com", "Pedido", "Preciso de ajuda");

        SimpleMailMessage mensagem = capturarMensagem();
        assertThat(mensagem.getTo()).containsExactly("suporte@vitral.test");
        assertThat(mensagem.getReplyTo()).isEqualTo("rute@vitral.com");
        assertThat(mensagem.getText()).contains("Rute <rute@vitral.com>").contains("Preciso de ajuda");
    }

    @Test
    void enviaNovoPedidoComESemId() {
        EmailService service = service(mailSender, true);

        service.enviarNotificacaoNovoPedido("sebo@vitral.com", 10L, "Ana", BigDecimal.valueOf(90));
        SimpleMailMessage comId = capturarMensagem();
        assertThat(comId.getText()).contains("(#10)").contains("Ana").contains("90");
        clearInvocations(mailSender);

        service.enviarNotificacaoNovoPedido("sebo@vitral.com", null, "Ana", BigDecimal.valueOf(90));
        SimpleMailMessage semId = capturarMensagem();
        assertThat(semId.getText()).doesNotContain("(#");
    }

    @Test
    void enviaRecuperacaoSenha() {
        EmailService service = service(mailSender, true);

        service.enviarRecuperacaoSenha("user@vitral.com", "reset");

        SimpleMailMessage mensagem = capturarMensagem();
        assertThat(mensagem.getSubject()).contains("Redefina");
        assertThat(mensagem.getText()).contains("https://app.vitral.test/auth/redefinir-senha?token=reset");
    }

    private EmailService service(JavaMailSender sender, boolean enabled) {
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "mailEnabled", enabled);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.vitral.test");
        ReflectionTestUtils.setField(service, "remetente", "noreply@vitral.test");
        ReflectionTestUtils.setField(service, "supportEmail", "suporte@vitral.test");
        return service;
    }

    private SimpleMailMessage capturarMensagem() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
