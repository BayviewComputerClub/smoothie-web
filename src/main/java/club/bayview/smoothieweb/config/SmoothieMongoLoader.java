package club.bayview.smoothieweb.config;

import club.bayview.smoothieweb.models.GeneralSettings;
import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.security.SmoothieAuthenticationProvider;
import club.bayview.smoothieweb.services.SmoothieSettingsService;
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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import javax.annotation.PostConstruct;

@Configuration
@EnableReactiveMongoRepositories
public class SmoothieMongoLoader extends AbstractReactiveMongoConfiguration {

    private Logger logger = LoggerFactory.getLogger(SmoothieMongoLoader.class);

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

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
        return MongoClients.create("mongodb://" + host + ":" + port);
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

    @Autowired
    private SmoothieSettingsService settingsService;

    // let mongo client automatically create indexes
    @Override
    public boolean autoIndexCreation() {
        return true;
    }

    // Default mongo objects
    @PostConstruct
    public void init() {

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
        logger.info("-=-=-=-=- MongoDB Loaded -=-=-=-=-");
    }
}
