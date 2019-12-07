package club.bayview.smoothieweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class SmoothieWebApplication {

    private Logger logger = LoggerFactory.getLogger(SmoothieWebApplication.class);

    public static ApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(SmoothieWebApplication.class, args);
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            logger.info("-=-=-=-=-=- SmoothieWeb has arrived! -=-=-=-=-=-");
        };
    }
}
