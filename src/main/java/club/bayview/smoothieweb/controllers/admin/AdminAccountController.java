package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Controller
public class AdminAccountController {

    @Autowired
    SmoothieUserService userService;

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminUsers(Model model) {
        return userService.findUsers().collectList().flatMap(users -> {
            model.addAttribute("users", users);
            return Mono.just("admin/users");
        });
    }

    @NoArgsConstructor
    @Getter
    @Setter
    static class AdminAccountEditForm {
        boolean admin,
                enabled; // enabled by email verification

        String handle, email, password, description;

        AdminAccountEditForm (User user) {
            admin = user.isAdmin();
            enabled = user.isEnabled();
            handle = user.getHandle();
            email = user.getEmail();
            description = user.getDescription();
        }

        // this will edit the original copy
        User toUser(User original) {
            if (isAdmin()) {
                original.getRoles().add(Role.ROLE_ADMIN);
            } else {
                original.getRoles().remove(Role.ROLE_ADMIN);
            }

            if (getPassword() != null && !getPassword().trim().isEmpty()) {
                original.setPassword(getPassword());
                original.encodePassword();
            }
            if (getHandle() != null && !getHandle().trim().isEmpty()) {
                original.setHandle(getHandle());
            }
            if (getEmail() != null && !getEmail().trim().isEmpty()) {
                original.setEmail(getEmail());
            }
            if (getDescription() != null && !getDescription().trim().isEmpty()) {
                original.setDescription(getDescription());
            }
            original.setEnabled(isEnabled());

            return original;
        }
    }

    @GetMapping("/user/{handle}/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminManageAccount(@PathVariable String handle, Model model) {
        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> {
                    model.addAttribute("form", new AdminAccountEditForm(user));
                    return Mono.just("admin/manage-user");
                })
                .onErrorResume(e -> Mono.just("404"));
    }

    @PostMapping("/user/{handle}/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> postAdminManageAccount(@PathVariable String handle, @Valid AdminAccountEditForm form, BindingResult result, Model model) {
        if (result.hasErrors()) { // TODO
            return Mono.just("admin/manage-user");
        }
        return userService.findUserByHandle(handle)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(user -> userService.saveUser(form.toUser(user)))
                .flatMap(user -> Mono.just("redirect:/user/" + user.getHandle() + "/admin"))
                .onErrorResume(e -> {
                    if (e instanceof NotFoundException) {
                        return Mono.just("404");
                    } else {
                        e.printStackTrace();
                        return Mono.just("500");
                    }
                });
    }

}
