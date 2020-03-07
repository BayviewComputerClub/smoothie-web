package club.bayview.smoothieweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class SmoothieSessionConfig {

//    @Bean
//    public ReactiveSessionRepository reactiveSessionRepository() {
//        return new ReactiveMapSessionRepository(new ConcurrentHashMap<>());
//    }
}
