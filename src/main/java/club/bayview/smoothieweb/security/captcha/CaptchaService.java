package club.bayview.smoothieweb.security.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.regex.Pattern;

// from baeldung

@Service
public class CaptchaService {
    private final static Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);

    @Autowired
    private CaptchaSettings captchaSettings;

    @Autowired
    private ReCaptchaAttemptService reCaptchaAttemptService;

    @Autowired
    private RestOperations restTemplate;

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    public static final class ReCaptchaUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 5861310537366287163L;
        public ReCaptchaUnavailableException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static final class ReCaptchaInvalidException extends RuntimeException {
        private static final long serialVersionUID = 5861310537366287163L;
        public ReCaptchaInvalidException(final String message) {
            super(message);
        }
    }

    @Bean
    public RestOperations restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3 * 1000);
        factory.setReadTimeout(7 * 1000);
        return new RestTemplate(factory);
    }

    public void processResponse(final String response, ServerWebExchange req) {
        LOGGER.debug("Attempting to validate response {}", response);

        if (reCaptchaAttemptService.isBlocked(getClientIP(req))) {
            throw new ReCaptchaInvalidException("Client exceeded maximum number of failed attempts");
        }

        if (!responseSanityCheck(response)) {
            throw new ReCaptchaInvalidException("Response contains invalid characters");
        }

        final URI verifyUri = URI.create(String.format("https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s&remoteip=%s", getReCaptchaSecret(), response, getClientIP(req)));
        try {
            final GoogleResponse googleResponse = restTemplate.getForObject(verifyUri, GoogleResponse.class);
            LOGGER.debug("Google's response: {} ", googleResponse.toString());

            if (!googleResponse.isSuccess()) {
                if (googleResponse.hasClientError()) {
                    reCaptchaAttemptService.reCaptchaFailed(getClientIP(req));
                }
                throw new ReCaptchaInvalidException("reCaptcha was not successfully validated");
            }
        } catch (RestClientException rce) {
            throw new ReCaptchaUnavailableException("Registration unavailable at this time.  Please try again later.", rce);
        }
        reCaptchaAttemptService.reCaptchaSucceeded(getClientIP(req));
    }

    private boolean responseSanityCheck(final String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

    public String getReCaptchaSite() {
        return captchaSettings.getSite();
    }

    public String getReCaptchaSecret() {
        return captchaSettings.getSecret();
    }

    private String getClientIP(ServerWebExchange req) {
        return req.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

}
