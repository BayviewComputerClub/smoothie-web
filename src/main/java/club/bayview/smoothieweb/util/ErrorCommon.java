package club.bayview.smoothieweb.util;

import org.slf4j.Logger;
import reactor.core.publisher.Mono;

public class ErrorCommon {

    public static Mono<String> handle404(Throwable e, Logger logger) {
        return handle404(e, logger, "");
    }

    public static Mono<String> handle404(Throwable e, Logger logger, String errorMsg) {
        if (e instanceof NotFoundException) { // simple 404
            return Mono.just("404");
        } else { // all other errors, report
            logger.error(errorMsg, e);
            return Mono.just("500");
        }
    }
}
