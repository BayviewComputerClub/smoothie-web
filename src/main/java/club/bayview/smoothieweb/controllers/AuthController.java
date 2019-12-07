package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
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
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AuthController {

    @Autowired
    private SmoothieUserService userDetailsService;

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
    public String logoutGetRoute() {
        return "logout";
    }


    @GetMapping("/login")
    public Mono<String> loginGetRoute(Model model) {
        return Mono.just("login");
    }

    @GetMapping("/register")
    public String registerGetRoute(Model model) {
        return "register";
    }

    @PostMapping("/register")
    public ModelAndView registerPostRoute(@Valid RegisterForm form, BindingResult result) {
        ModelAndView page = new ModelAndView();

        if (userDetailsService.findByHandle(form.username).block() != null) {
            result.rejectValue("username", "error.user", "The username has already been taken!");
        } else if (userDetailsService.findByEmail(form.email).block() != null) {
            result.rejectValue("email", "error.user", "The email has already been used!");
        }

        if (result.hasErrors()) {
            page.setViewName("register");
        } else {
            userDetailsService.saveUser(new User(form.username, form.email, form.password));
            page.setViewName("redirect:/login");
            // TODO success registering message
        }
        return page;
    }

}
