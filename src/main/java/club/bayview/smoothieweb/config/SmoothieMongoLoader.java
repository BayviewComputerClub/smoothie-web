package club.bayview.smoothieweb.config;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import club.bayview.smoothieweb.services.SmoothieUserService;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

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

    @Autowired
    SmoothieAuthenticationProvider authenticationProvider;

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

    @Bean
    public ReactiveGridFsTemplate reactiveGridFsTemplate() throws Exception {
        return new ReactiveGridFsTemplate(reactiveMongoDbFactory(), mappingMongoConverter());
    }

    @Bean
    public ReactiveMongoTemplate mongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), "databaseName");
    }

    @Autowired
    private SmoothieUserService userService;

    // let mongo client automatically create indexes
    @Override
    public boolean autoIndexCreation() {
        return true;
    }

    @PostConstruct
    public void init() {

        // create default admin account
        if (userService.findByUsername("admin").block() == null) {
            User admin = new User("admin", "", adminPassword);
            admin.getRoles().add(Role.ROLE_ADMIN);
            admin.getRoles().add(Role.ROLE_EDITOR);
            admin.setPassword(authenticationProvider.passwordEncoder.encode(admin.getPassword()));;
            userService.saveUser(admin).block();
        }
        logger.info("-=-=-=-=- MongoDB Loaded -=-=-=-=-");
    }
}
