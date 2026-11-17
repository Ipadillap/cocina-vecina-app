package cl.udec.cocinavecina.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VerificationMailer {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String publicUrl;

    public VerificationMailer(
        ObjectProvider<JavaMailSender> mailSender,
        @Value("${cocinavecina.public-url}") String publicUrl
    ) {
        this.mailSender = mailSender;
        this.publicUrl = publicUrl;
    }

    public void send(String destination, String token) {
        final String link = publicUrl + "/v1/auth/verify?token=" + token;
        final JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("SMTP no configurado; enlace de verificación para {}: {}", destination, link);
            return;
        }
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destination);
        message.setSubject("Verifica tu cuenta de CocinaVecina");
        message.setText("Hola. Para activar tu cuenta abre este enlace (vence en 24 horas):\n\n" + link);
        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo de verificación a {}: {}", destination, e.getMessage());
        }
    }
}
