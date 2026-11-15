package cl.udec.cocinavecina.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class VerificationMailer {
    private static final Logger log = LoggerFactory.getLogger(VerificationMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String publicUrl;

    public VerificationMailer(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${cocinavecina.public-url}") String publicUrl) {
        this.mailSender = mailSender;
        this.publicUrl = publicUrl;
    }

    public void send(String destination, String token) {
        String link = publicUrl + "/v1/auth/verify?token=" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("SMTP no configurado; enlace de verificación para {}: {}", destination, link);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
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
