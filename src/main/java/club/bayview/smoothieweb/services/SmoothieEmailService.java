package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class SmoothieEmailService {

    public JavaMailSender emailSender;

    @Autowired
    SmoothieSettingsService settingsService;

    @Autowired
    SmoothieEmailVerificationService emailVerificationService;

    @Value("${smoothieweb.URL}")
    String siteURL;

    @Value("${smoothieweb.contactEmail}")
    String contactEmail;

    public void sendTextEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    public void sendVerificationEmail(User user) {
        String siteName = settingsService.getGeneralSettings().getSiteName(), JWT = emailVerificationService.createJWT(user);
        sendTextEmail(user.getEmail(), "Verify your email for " + siteName,
                "Hey " + user.getUsername() + ",\n\n" +
                        "Please go to " + siteURL + "/verify/" + JWT + " to verify your email address for " + siteName + ".\n\n" +
                        "Note that this link will expire in 24 hours.\n\n" +
                        "If you did not expect to receive this message, you can feel free to ignore it; your data/email address has NOT been compromised.\n\n" +
                        "Please do not reply to this message; this email is not monitored. Please email " + contactEmail + " For any issues with " + siteName + ".\n\n" +
                        "Sincerely,\n" +
                        siteName + " admins");
    }
}
