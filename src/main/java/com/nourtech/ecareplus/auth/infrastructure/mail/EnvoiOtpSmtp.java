package com.nourtech.ecareplus.auth.infrastructure.mail;
import com.nourtech.ecareplus.auth.application.service.EnvoiOtp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EnvoiOtpSmtp implements EnvoiOtp {
    private final JavaMailSender sender; private final String host; private final String from;
    public EnvoiOtpSmtp(ObjectProvider<JavaMailSender> sender, @Value("${spring.mail.host:}") String host,
                        @Value("${app.mail.from:}") String from) { this.sender=sender.getIfAvailable(); this.host=host; this.from=from; }
    public void envoyer(String destinataire, String code) {
        if (sender == null || host.isBlank() || from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Le service d'envoi de code n'est pas configuré");
        SimpleMailMessage message = new SimpleMailMessage(); message.setFrom(from); message.setTo(destinataire);
        message.setSubject("Votre code eCARE+"); message.setText("Votre code de vérification est : " + code + ". Il expire dans 10 minutes.");
        sender.send(message);
    }
}
