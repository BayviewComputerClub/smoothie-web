package club.bayview.smoothieweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SmoothieWebApplication {

    private Logger logger = LoggerFactory.getLogger(SmoothieWebApplication.class);

    public static ApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(SmoothieWebApplication.class, args);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            logger.info("-=-=-=-=-=- SmoothieWeb has arrived! -=-=-=-=-=-");
        };
    }
}
