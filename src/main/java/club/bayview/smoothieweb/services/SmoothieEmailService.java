package club.bayview.smoothieweb.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class SmoothieEmailService {

    @Autowired
    JavaMailSender emailSender;

    @Autowired
    SmoothieSettingsService settingsService;

    @Autowired
    SmoothieEmailVerificationService emailVerificationService;

    public void sendTextEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
