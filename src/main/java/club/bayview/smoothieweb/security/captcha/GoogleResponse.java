package club.bayview.smoothieweb.security.captcha;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// from baeldung

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"success", "challenge_ts", "hostname", "error-codes"})
@Getter
@Setter
public class GoogleResponse {

    @JsonProperty("success")
    private boolean success;
    @JsonProperty("challenge_ts")
    private String challengeTs;
    @JsonProperty("hostname")
    private String hostname;
    @JsonProperty("error-codes")
    private ErrorCode[] errorCodes;

    enum ErrorCode {
        MissingSecret, InvalidSecret, MissingResponse, InvalidResponse;

        private static Map<String, ErrorCode> errorsMap = new HashMap<>(4);

        static {
            errorsMap.put("missing-input-secret", MissingSecret);
            errorsMap.put("invalid-input-secret", InvalidSecret);
            errorsMap.put("missing-input-response", MissingResponse);
            errorsMap.put("invalid-input-response", InvalidResponse);
        }

        @JsonCreator
        public static ErrorCode forValue(final String value) {
            return errorsMap.get(value.toLowerCase());
        }
    }

    @JsonIgnore
    public boolean hasClientError() {
        final ErrorCode[] errors = getErrorCodes();
        if (errors == null) {
            return false;
        }
        for (final ErrorCode error : errors) {
            switch (error) {
                case InvalidResponse:
                case MissingResponse:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "GoogleResponse{" + "success=" + success + ", challengeTs='" + challengeTs + '\'' + ", hostname='" + hostname + '\'' + ", errorCodes=" + Arrays.toString(errorCodes) + '}';
    }

}
