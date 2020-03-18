package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SmoothieStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SmoothieUserService userService;
    @Autowired
    private SmoothieSettingsService settingsService;

    @Value("${smoothieweb.admin.password:'password'}")
    private String adminPassword;

    private Logger logger = LoggerFactory.getLogger(SmoothieStartup.class);

    // Default mongo objects
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // create default admin account
            if (userService.findByUsername("admin").block() == null) {
                User admin = new User("admin", "", adminPassword);
                admin.encodePassword();
                admin.setEnabled(true);
                admin.getRoles().add(Role.ROLE_ADMIN);
                admin.getRoles().add(Role.ROLE_EDITOR);
                userService.saveUser(admin).block();
            }

            if (settingsService.getGeneralSettings() == null) {
                GeneralSettings settings = new GeneralSettings();
                settings.setSiteName("smoothie-web");
                settings.setTagLine("i like potatoes.");
                settings.setHomeContent("Welcome to this smoothie-web instance.\n" +
                        "\n" +
                        "Sit down, have a smoothie, and enjoy hitting the keyboard furiously. (~˘▾˘)~\n" +
                        "\n" +
                        " \\ (•◡•) /");
                settingsService.saveGeneralSettings(settings).block();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("-=-=-=-=- Post Initialization Completed -=-=-=-=-");
    }
}
