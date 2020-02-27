package club.bayview.smoothieweb.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class SmoothieErrorController {
    @RequestMapping("/error")
    public Mono<String> requestError() {
//        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
//
//        if (status != null) {
//            int statusCode = Integer.parseInt(status.toString());
//
//            if (statusCode == HttpStatus.NOT_FOUND.value()) {
//                return Mono.just("error/404");
//            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
//                return Mono.just("error/500");
//            }
//        }

        return Mono.just("error/error");
    }

}
