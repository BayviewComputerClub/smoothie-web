package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.repositories.GeneralSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmoothieSettingsService {

    @Autowired
    GeneralSettingsRepository settingsRepository;

    // always cache, since there is one instance
    GeneralSettings settings = null;

    public GeneralSettings getGeneralSettings() {
        if (settings == null) {
            settings = settingsRepository.findAll().next().block(); // block only on first database fetch
        }
        return settings;
    }

    public Mono<GeneralSettings> saveGeneralSettings(GeneralSettings settings) {
        this.settings = settings;
        return settingsRepository.save(settings);
    }

}
