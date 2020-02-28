package club.bayview.smoothieweb.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
@Order(-2)
public class GlobalErrorExceptionHandler implements ErrorWebExceptionHandler {

    // fill in global header handler attributes
    @Autowired
    GlobalHeaderHandler headerHandler;

    @Autowired
    ViewResolver viewResolver;

    @Value("${smoothieweb.error.debug:false}")
    private boolean showDebugErrors;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        exchange.getAttributes().put("siteName", headerHandler.getSiteName());
        exchange.getAttributes().put("isAdmin", false); // TODO

        String viewName = "error/error";
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException e = (ResponseStatusException) ex;
            if (e.getStatus().is4xxClientError()) {
                viewName = "error/404";
            } else if (e.getStatus().is5xxServerError()) {
                viewName = "error/500";
                ex.printStackTrace();
            }
        } else {
            ex.printStackTrace();
        }

        if (showDebugErrors) {
            exchange.getAttributes().put("error", ex.getMessage());
        }

        return viewResolver
                .resolveViewName(viewName, Locale.US)
                .flatMap(v -> v.render(exchange.getAttributes(), MediaType.TEXT_HTML, exchange));
    }
}
