package club.bayview.smoothieweb.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = {"club.bayview.smoothieweb.repositories"})
public class SmoothieMongoLoader extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:'main'}")
    private String databaseName;

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
        return new ReactiveMongoTemplate(reactiveMongoClient(), databaseName);
    }

    // let mongo client automatically create indexes
    @Override
    public boolean autoIndexCreation() {
        return true;
    }

}
