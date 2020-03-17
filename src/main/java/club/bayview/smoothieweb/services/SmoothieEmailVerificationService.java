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

    @Autowired
    SmoothieSettingsService settingsService;

    @Value("${smoothieweb.emailVerification.secret}")
    String secret;

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
                    .withIssuer(settingsService.getGeneralSettings().getSiteName())
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
                    .withIssuer(settingsService.getGeneralSettings().getSiteName())
                    .acceptLeeway(1)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}