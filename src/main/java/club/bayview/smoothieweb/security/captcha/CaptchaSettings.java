package club.bayview.smoothieweb.security.captcha;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.recaptcha.key")
@Getter
@Setter
@NoArgsConstructor
public class CaptchaSettings {
    private String site;
    private String secret;
}
