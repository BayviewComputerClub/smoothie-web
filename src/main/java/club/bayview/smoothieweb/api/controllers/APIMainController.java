package club.bayview.smoothieweb.api.controllers;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class APIMainController {
    @Autowired
    private SmoothieSettingsService settingsService;

    @Autowired
    private SmoothieUserService userService;

    @RequestMapping("/api/v1/home")
    public GeneralSettings getHomeSettings() {
        return settingsService.getGeneralSettings();
    }

    @RequestMapping("/api/v1/ranking")
    public Mono<List<User>> getUserRanking() {
        return userService.findUsers().collectList();
    }
}
