package com.vitral.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.url.frontend:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@vitral.com}")
    private String remetente;

    @Value("${app.support.email:vitral.no.reply@gmail.com}")
    private String supportEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isMailEnabled() {
        return mailEnabled && mailSender != null;
    }

    public void enviarMensagemSuporte(String remetenteNome, String remetenteEmail, String assunto, String mensagem) {
        if (!mailEnabled || mailSender == null) {
            return;
        }
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(remetente);
        email.setTo(supportEmail);
        email.setReplyTo(remetenteEmail);
        email.setSubject("Vitral Suporte — " + assunto);
        email.setText("Nova mensagem de suporte recebida.\n\n"
                + "De: " + remetenteNome + " <" + remetenteEmail + ">\n"
                + "Assunto: " + assunto + "\n\n"
                + "Mensagem:\n" + mensagem + "\n");
        mailSender.send(email);
    }

    public void enviarConfirmacaoCadastro(String destinatario, String token) {
        if (!mailEnabled || mailSender == null) {
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Vitral — Confirme seu cadastro");
        mensagem.setText("Olá! Clique no link abaixo para confirmar seu cadastro no Vitral:\n\n"
                + frontendUrl + "/auth/confirmar?token=" + token
                + "\n\nO link expira em 24 horas.\n\nCaso não tenha criado uma conta, ignore este e-mail.");
        mailSender.send(mensagem);
    }

    public void enviarNotificacaoNovoPedido(String destinatario, Long pedidoId, String comprador,
            java.math.BigDecimal total) {
        if (!mailEnabled || mailSender == null) {
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Vitral — Novo pedido recebido");
        mensagem.setText("Voce recebeu um novo pedido no Vitral"
                + (pedidoId != null ? " (#" + pedidoId + ")" : "")
                + " de " + comprador + " no valor de R$ " + total + "."
                + "\n\nO pagamento ja foi aprovado. Acesse o painel para confirmar a entrega:\n\n"
                + frontendUrl + "/vendas\n");
        mailSender.send(mensagem);
    }

    public void enviarRecuperacaoSenha(String destinatario, String token) {
        if (!mailEnabled || mailSender == null) {
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Vitral — Redefina sua senha");
        mensagem.setText("Recebemos uma solicitação para redefinir sua senha no Vitral:\n\n"
                + frontendUrl + "/auth/redefinir-senha?token=" + token
                + "\n\nO link expira em 1 hora. Se você não fez esta solicitação, ignore este e-mail.");
        mailSender.send(mensagem);
    }
}
