package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieProblemService problemService;

    class ProfileForm {

    }

    @GetMapping("{handle}")
    public Mono<String> getProfileRoute(@PathVariable String handle, Model model) {
        return userService.findUserByHandle(handle).flatMap(user -> {
            if (user == null) return Mono.just("404");

            model.addAttribute("user", user);
            return Mono.just("profile");
        });
    }

    @GetMapping("{handle}/edit")
    public Mono<String> getEditProfileRoute(@PathVariable String handle, Model model, Principal principal) {
        return userService.findUserByHandle(handle).flatMap(user -> {
            if (user == null) return Mono.just("404");


            model.addAttribute("user", user);
            return Mono.just("edit-profile");
        });
    }

    @PostMapping("{handle}/edit")
    public Mono<String> postEditProfileRoute(@PathVariable String handle, Model model, Principal principal) {

    }

}
