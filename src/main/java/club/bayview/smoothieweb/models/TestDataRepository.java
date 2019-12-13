package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieMongoLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestDataRepository {

    @Autowired
    SmoothieMongoLoader mongoLoader;

    public Mono<TestData> getTestData(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().findOne(new Query(Criteria.where("_id").is(dataId))).flatMap(file -> {
            try {
                return mongoLoader.reactiveGridFsTemplate().getResource(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Mono.just(null);
        }).flatMap(file -> {
            if (file == null) return Mono.just(null);

            try {
                ObjectMapper om = new ObjectMapper();
                return Mono.just(om.readValue(new String(file.getInputStream().readAllBytes()), TestData.class));
            } catch (IOException e) {
                e.printStackTrace();
                return Mono.just(null);
            }
        });
    }

}
