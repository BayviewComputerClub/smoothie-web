package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.services.SmoothieSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
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

}
