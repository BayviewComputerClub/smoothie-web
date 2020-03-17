package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import club.bayview.smoothieweb.security.captcha.CaptchaService;
import club.bayview.smoothieweb.services.SmoothieEmailService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Controller
public class AuthController {

    @Autowired
    SmoothieAuthenticationProvider authenticationProvider;

    @Autowired
    SmoothieUserService userService;

    @Autowired
    CaptchaService captchaService;

    @Autowired
    SmoothieEmailService emailService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterForm {

        @NotNull
        @Size(min = 2, max = 15)
        private String username;

        @NotNull
        @Size(min = 3)
        private String password;

        @NotNull
        private String email;
    }

    @GetMapping("/logout")
    public Mono<String> logoutGetRoute() {
        return Mono.just("redirect:/");
    }

    @GetMapping("/login")
    @CrossOrigin(origins = "http://localhost:3000") // TODO move to config file
    public Mono<String> loginGetRoute() {
        return Mono.just("login");
    }

    @GetMapping("/register")
    public Mono<String> registerGetRoute() {
        return Mono.just("register");
    }

    @GetMapping("/verify-email")
    public Mono<String> verifyEmailGetRoute() {
        return Mono.just("verify-email");
    }

    @GetMapping("/welcome")
    public Mono<String> welcomeGetRoute() {
        return Mono.just("welcome");
    }

    @GetMapping("/verify-error")
    public Mono<String> verifyErrorGetRoute() {
        return Mono.just("verify-error");
    }

    @PostMapping("/register")
    public Mono<String> registerPostRoute(@Valid RegisterForm form, BindingResult result, ServerWebExchange req) {

        return req.getFormData().flatMap(formData -> {
            String captchaRes = formData.getFirst("g-recaptcha-response");
            try {
                captchaService.processResponse(captchaRes, req);
            } catch (Exception e) {
                result.reject("captcha", "Captcha failed! Try again and prove you're not a robot.");
                return Mono.error(e);
            }

            if (userService.findUserByHandle(form.username).block() != null) {
                result.rejectValue("username", "error.user", "That username is taken!");
            } else if (userService.findUserByEmail(form.email).block() != null) {
                result.rejectValue("email", "error.user", "That email has already been used!");
            }

            if (result.hasErrors()) {
                return Mono.error(new Exception());
            } else {
                User user = new User(form.username, form.email, form.password);
                user.encodePassword();
                userService.saveUser(user).block();
                emailService.sendVerificationEmail(user);
                return Mono.just("redirect:/verify-email");
            }
        }).onErrorResume(e -> Mono.just("register"));
    }

}
