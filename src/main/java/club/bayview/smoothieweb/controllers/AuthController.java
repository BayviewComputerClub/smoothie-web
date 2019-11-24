package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AuthController {

    @Autowired
    private SmoothieUserService userDetailsService;

    class LoginForm {
        @NotNull
        String username;

        @NotNull
        String password;
    }

    public static class RegisterForm {
        @NotNull
        private String username;

        @NotNull
        private String password;

        @NotNull
        private String email;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

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

        System.out.println("wut " + form.username + " " + form.email + " " + form.password); // TODO

        if (userDetailsService.findUserByHandle(form.username).block() != null) {
            result.rejectValue("handle", "error.user", "The username has already been taken!");
        } else if (userDetailsService.findUserByEmail(form.email).block() != null) {
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
