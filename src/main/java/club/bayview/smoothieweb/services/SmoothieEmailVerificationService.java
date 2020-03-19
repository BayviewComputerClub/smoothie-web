package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.User;
import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.*;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.*;
import com.auth0.jwt.JWTVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
public class SmoothieEmailVerificationService {

    private static final String JWT_ISSUER = "smoothie-web";

    @Autowired
    SmoothieSettingsService settingsService;

    @Autowired
    SmoothieEmailService emailService;

    @Value("${smoothieweb.email-verification.enabled}")
    public boolean enabled;

    @Value("${smoothieweb.email-verification.secret}")
    String secret;

    @Value("${smoothieweb.url}")
    String siteUrl;

    @Value("${smoothieweb.contact-email}")
    String contactEmail;

    /**
     * Create a new JWT for a user to send via email to verify
     *
     * @param user The user that needs to be verified
     * @return The created JWT. Null on error.
     */
    public String createJWT(User user) {
        try {
            Algorithm alg = Algorithm.HMAC512(secret);
            Calendar exp = Calendar.getInstance();
            exp.add(Calendar.DATE, 1);

            return JWT.create()
                    .withIssuer(JWT_ISSUER)
                    .withSubject(user.getUsername())
                    .withExpiresAt(exp.getTime())
                    .withIssuedAt(Calendar.getInstance().getTime())
                    .sign(alg);

        } catch (JWTCreationException exception) {
            return null;
        }
    }

    /**
     * Verify a JWT
     *
     * @param token The JWT to verify
     * @return The username of the token, if verification was successful. Null otherwise.
     */

    public String verifyJWT(String token) {
        try {
            Algorithm alg = Algorithm.HMAC512(secret);
            JWTVerifier verifier = JWT.require(alg)
                    .withIssuer(JWT_ISSUER)
                    .acceptLeeway(1)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public void sendVerificationEmail(User user) {
        String siteName = settingsService.getGeneralSettings().getSiteName(), jwt = createJWT(user);
        emailService.sendTextEmail(user.getEmail(), "Verify your email for " + siteName,
                "Hey " + user.getUsername() + ",\n\n" +
                        "Please go to " + siteUrl + "/verify-email/" + jwt + " to verify your email address for " + siteName + ".\n\n" +
                        "Note that this link will expire in 24 hours.\n\n" +
                        "If you did not expect to receive this message, you can feel free to ignore it; your data/email address has NOT been compromised.\n\n" +
                        "Please do not reply to this message; this email is not monitored. Please email " + contactEmail + " For any issues with " + siteName + ".\n\n" +
                        "Sincerely,\n" +
                        siteName + " admins");
    }
}