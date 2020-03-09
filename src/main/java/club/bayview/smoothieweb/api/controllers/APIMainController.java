package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class APIMainController {
    @Autowired
    private SmoothieSettingsService settingsService;

    @RequestMapping("/api/home")
    public GeneralSettings getHomeSettings() {
        return settingsService.getGeneralSettings();
    }
}
