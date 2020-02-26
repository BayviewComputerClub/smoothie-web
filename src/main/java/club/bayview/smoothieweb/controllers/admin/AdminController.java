package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
public class AdminController {

    @Autowired
    SmoothieSettingsService settingsService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminRoute() {
        return Mono.just("admin/admin");
    }

    @GetMapping("/admin/problems")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminProblemsRoute() {
        return Mono.just("admin/problems");
    }

    @NoArgsConstructor
    @Getter
    @Setter
    static class GeneralForm {

        @NotNull
        private String siteName, tagLine, homeContent;

        GeneralForm(GeneralSettings settings) {
            this.siteName = settings.getSiteName();
            this.tagLine = settings.getTagLine();
            this.homeContent = settings.getHomeContent();
        }

        GeneralSettings toGeneralSettings(GeneralSettings settings) {
            settings.setSiteName(siteName);
            settings.setTagLine(tagLine);
            settings.setHomeContent(homeContent);
            return settings;
        }
    }

    @GetMapping("/admin/general")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getAdminGeneralRoute(Model model) {
        model.addAttribute("form", new GeneralForm(settingsService.getGeneralSettings()));
        return Mono.just("admin/edit-general");
    }

    @PostMapping("/admin/general")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> postAdminGeneralRoute(Model model, @Valid GeneralForm form, BindingResult res) {
        GeneralSettings settings = settingsService.getGeneralSettings();
        if (res.hasErrors()) {
            model.addAttribute("form", new GeneralForm(settings));
            return Mono.just("admin/edit-general");
        }

        return settingsService.saveGeneralSettings(form.toGeneralSettings(settings))
                .flatMap(s -> Mono.just("redirect:/admin/general"));
    }
}
