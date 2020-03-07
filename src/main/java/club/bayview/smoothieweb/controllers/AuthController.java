package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import club.bayview.smoothieweb.security.captcha.CaptchaService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AuthController {

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    SmoothieAuthenticationProvider authenticationProvider;

    @Autowired
    private CaptchaService captchaService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterForm {
        @NotNull
        private String username;

        @NotNull
        private String password;

        @NotNull
        private String email;

    }

    // ~~~~~~~~~~

    @GetMapping("/logout")
    public Mono<String> logoutGetRoute() {
        return Mono.just("redirect:/");
    }

    @GetMapping("/login")
    public Mono<String> loginGetRoute() {
        return Mono.just("login");
    }

    @GetMapping("/register")
    public Mono<String> registerGetRoute() {
        return Mono.just("register");
    }

    @PostMapping("/register")
    public Mono<String> registerPostRoute(@Valid RegisterForm form, BindingResult result, ServerWebExchange req, Model model) {

        return req.getFormData()
                .flatMap(formData -> {
                    String captchaRes = formData.getFirst("g-recaptcha-response");
                    try {
                        captchaService.processResponse(captchaRes, req);
                    } catch (Exception e) {
                        result.reject("captcha", "Captcha failed! Try again and prove you're not a robot.");
                        return Mono.error(e);
                    }

                    if (userService.findUserByHandle(form.username).block() != null) {
                        result.rejectValue("username", "error.user", "The username has already been taken!");
                    } else if (userService.findUserByEmail(form.email).block() != null) {
                        result.rejectValue("email", "error.user", "The email has already been used!");
                    }

                    if (result.hasErrors()) {
                        return Mono.error(new Exception());
                    } else {
                        User user = new User(form.username, form.email, form.password);
                        user.encodePassword();
                        userService.saveUser(user).block();
                        return Mono.just("redirect:/login");
                        // TODO "Conf email sent" message
                    }
                })
                .onErrorResume(e -> Mono.just("register"));
    }

}
