package club.bayview.smoothieweb;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.web.context.request.RequestContextListener;

import javax.annotation.PostConstruct;

@Configuration
@EnableReactiveMongoRepositories
public class SmoothieMongoLoader extends AbstractReactiveMongoConfiguration {

    private Logger logger = LoggerFactory.getLogger(SmoothieMongoLoader.class);

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:'main'}")
    private String databaseName;

    @Value("${smoothieweb.admin.password:'password'}")
    private String adminPassword;

    @Bean
    public LoggingEventListener mongoEventListener() {
        return new LoggingEventListener();
    }

    @Override
    protected String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(String.format("mongodb://localhost:%d", port));
    }

    @Autowired
    private SmoothieUserService userService;

    @PostConstruct
    public void init() {
        if (userService.findByUsername("admin").block() == null) {
            User admin = new User("admin", "", adminPassword);
            admin.getRoles().add(Role.ROLE_ADMIN);
            admin.getRoles().add(Role.ROLE_EDITOR);
            userService.saveUser(admin);
        }
        logger.info("-=-=-=-=- MongoDB Loaded -=-=-=-=-");
    }
}
