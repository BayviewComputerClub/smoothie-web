package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.SessionUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.security.Principal;

@Controller
public class UserController {

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SessionUtils sessionUtils;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    static class ProfileForm {
        String description;

        ProfileForm(User user) {
            description = user.getDescription();
        }

        private User toUser(User user) {
            user.setDescription(getDescription());
            return user;
        }
    }

    @GetMapping("/user/{handle}")
    public Mono<String> getProfileRoute(@PathVariable String handle, Model model, Authentication auth, Principal principal) {
        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    if (/*auth != null && auth.getAuthorities() != null && auth.getAuthorities().contains(new SimpleGrantedAuthority(Role.ROLE_ADMIN.getName())) ||*/ (principal != null && principal.getName().equalsIgnoreCase(user.getHandle()))) {
                        model.addAttribute("allowEdit", true);
                    } else {
                        model.addAttribute("allowEdit", false);
                    }
                    model.addAttribute("user", user);
                    return Mono.just("user/profile");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @GetMapping("/user/{handle}/edit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getEditProfileRoute(@PathVariable String handle, Model model, Principal principal) {

        if (!principal.getName().equalsIgnoreCase(handle)) {
            return Mono.just("no"); // no permission
        }

        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("profileForm", new ProfileForm(user));
                    model.addAttribute("user", user);
                    return Mono.just("user/edit-profile");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @PostMapping("/user/{handle}/edit")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postEditProfileRoute(@Valid ProfileForm form, BindingResult res, @PathVariable String handle, Model model, Principal principal) {
        if (!principal.getName().equalsIgnoreCase(handle)) {
            return Mono.just("no");
        }

        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    if (res.hasErrors()) {
                        model.addAttribute("profileForm", form);
                        model.addAttribute("user", user);
                        return Mono.just("user/edit-profile");
                    }

                    return userService.saveUser(form.toUser(user))
                            .flatMap(user1 -> Mono.just("redirect:/user/" + user.getHandle()));
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    static class ChangePasswordForm {

        @NotEmpty
        @Size(min = 3)
        String password, currentPassword;

        private User toUser(User user) {
            user.setPassword(password);
            user.encodePassword();
            return user;
        }
    }

    @GetMapping("/account/settings")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getSettingsRoute(Model model, Principal principal) {
        return userService.findUserByHandle(principal.getName())
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("user", user);
                    return Mono.just("user/edit-user");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @GetMapping("/account/change-password")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> getChangePasswordRoute(Model model, Principal principal) {
        return userService.findUserByHandle(principal.getName())
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("changePasswordForm", new ChangePasswordForm());
                    model.addAttribute("user", user);
                    return Mono.just("user/change-password");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @PostMapping("/account/change-password")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<String> postChangePasswordRoute(@Valid ChangePasswordForm form, BindingResult res, Model model, Principal principal) {

        return userService.findUserByHandle(principal.getName())
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    if (!user.isPassword(form.currentPassword)) {
                        res.rejectValue("currentPassword", "error.pass", "Incorrect password!");
                    }

                    if (res.hasErrors()) {
                        model.addAttribute("changePasswordForm", form);
                        model.addAttribute("user", user);
                        return Mono.just("user/change-password");
                    }

                    return userService.saveUser(form.toUser(user))
                            .flatMap(user1 -> Mono.just("redirect:/account/settings"));
                }).onErrorResume(e -> Mono.just("404"));
    }
}
