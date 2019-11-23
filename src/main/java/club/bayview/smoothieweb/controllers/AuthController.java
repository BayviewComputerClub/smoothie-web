package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AuthController {

    @Autowired
    private SmoothieUserDetailsService userDetailsService;

    class LoginForm {
        @NotNull
        String name;

        @NotNull
        String password;
    }

    @GetMapping("/login")
    public String loginGetRoute() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPostRoute(@Valid LoginForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "login";
        }

        return "redirect:/";
    }


    class RegisterForm {
        @NotNull
        String handle;

        @NotNull
        String password;

        @NotNull
        String email;
    }

    @GetMapping("/register")
    public String registerGetRoute(Model model) {
        return "register";
    }

    @PostMapping("/register")
    public ModelAndView registerPostRoute(@Valid RegisterForm form, BindingResult result) {
        ModelAndView page = new ModelAndView();

        if (userDetailsService.findUserByHandle(form.handle).block() != null) {
            result.rejectValue("handle", "error.user", "The username has already been taken!");
        } else if (userDetailsService.findUserByEmail(form.email).block() != null) {
            result.rejectValue("email", "error.user", "The email has already been used!");
        }

        if (result.hasErrors()) {
            page.setViewName("register");
        } else {
            userDetailsService.saveUser(new User(form.handle, form.email, form.password));
            page.setViewName("login");
            // TODO success registering message
        }
        return page;
    }

}
