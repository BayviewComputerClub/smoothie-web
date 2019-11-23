package club.bayview.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AuthController {

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

}
