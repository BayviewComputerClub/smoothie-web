package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalHeaderHandler {

    @Autowired
    SmoothieSettingsService settingsService;

    @ModelAttribute("siteName")
    public String getSiteName() {
        return settingsService.getGeneralSettings().getSiteName();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User))
            return false;

        User u = (User) auth.getPrincipal();
        return u.getRoles().contains(Role.ROLE_ADMIN);
    }

}
