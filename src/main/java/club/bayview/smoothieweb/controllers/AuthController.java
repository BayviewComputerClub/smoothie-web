package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import club.bayview.smoothieweb.security.captcha.CaptchaService;
import club.bayview.smoothieweb.services.SmoothieEmailService;
import club.bayview.smoothieweb.services.SmoothieEmailVerificationService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
    SmoothieEmailVerificationService emailVerificationService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterForm {

        @NotNull
        @Size(min = 2, max = 15)
        @Pattern(regexp = "^[A-Za-z0-9]+$")
        private String username;

        @NotNull
        @Size(min = 3)
        private String password;

        @NotNull
        private String confirmPassword;

        @NotNull
        private String email;
    }

    @GetMapping("/logout")
    public Mono<String> logoutGetRoute() {
        return Mono.just("redirect:/");
    }

    @GetMapping("/login")
    public Mono<String> loginGetRoute() {
        return Mono.just("login");
    }

    @GetMapping("/register")
    public Mono<String> registerGetRoute(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
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
    public Mono<String> registerPostRoute(@Valid RegisterForm registerForm, BindingResult result, ServerWebExchange req, Model model) {
        model.addAttribute("form", registerForm);

        if (!registerForm.confirmPassword.equals(registerForm.password)) {
            result.rejectValue("confirmPassword", "confirmPassword.password", "The passwords must match.");
        }

        return userService.findUserByHandle(registerForm.username) // validation
                .doOnNext(u -> result.rejectValue("username", "username.taken", "That username is taken!"))
                .then(userService.findUserByEmail(registerForm.email))
                .doOnNext(u -> result.rejectValue("email", "email.taken", "That email has already been used!"))

                // retrieve captcha
                .then(req.getFormData())
                .flatMap(formData -> {
                    String captchaRes = formData.getFirst("g-recaptcha-response");
                    try {
                        captchaService.processResponse(captchaRes, req);
                    } catch (Exception e) {
                        result.reject("captcha", "Captcha failed! Try again and prove you're not a robot.");
                        return Mono.error(e);
                    }

                    if (result.hasErrors()) { // form errors
                        return Mono.error(new Exception());
                    } else {
                        // create user
                        User user = new User(registerForm.username, registerForm.email, registerForm.password);
                        user.encodePassword();

                        // enable account if email verification is disabled
                        if (!emailVerificationService.enabled) {
                            user.setEnabled(true);
                        }
                        return userService.saveUser(user);
                    }
                })

                // send verification email
                .flatMap(u -> {
                    if (emailVerificationService.enabled) {
                        emailVerificationService.sendVerificationEmail(u);
                        return Mono.just("redirect:/verify-email");
                    } else {
                        return Mono.just("redirect:/login");
                    }
                })

                // error handling
                .onErrorResume(e -> {
                    e.printStackTrace();
                    if (e instanceof MailSendException) {
                        e.printStackTrace();
                        return Mono.just("error/error");
                    } else {
                        return Mono.just("register");
                    }
                });
    }

}
