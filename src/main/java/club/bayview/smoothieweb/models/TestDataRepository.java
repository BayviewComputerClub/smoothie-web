package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.config.SmoothieMongoLoader;
import com.google.common.primitives.Bytes;
import com.mongodb.reactivestreams.client.gridfs.helpers.AsyncStreamHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TestDataRepository {

    @Autowired
    SmoothieMongoLoader mongoLoader;

    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    DefaultCoder coder = new DefaultCoder(true, TestData.class);

    public Mono<String> getTestDataHash(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().findOne(new Query(Criteria.where("_id").is(dataId))).flatMap(file -> {
            if (file == null) return Mono.just("");
            return Mono.just(file.getMetadata().getString("hash"));
        });
    }

    public Mono<TestData> getTestData(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().findOne(new Query(Criteria.where("_id").is(dataId))).flatMap(file -> {
            try {
                return mongoLoader.reactiveGridFsTemplate().getResource(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Mono.just(null);
        }).flatMap(file -> {
            if (file == null) return Mono.just(new TestData(new ArrayList<>()));

            try {
                List<byte[]> bytes = new ArrayList<>();
                AtomicLong length = new AtomicLong();

                return file.getDownloadStream().collectList().flatMap(buffers -> {
                    for (var buff : buffers) {
                        try {
                            bytes.add(buff.asInputStream().readAllBytes());
                            length.addAndGet(bytes.get(bytes.size() - 1).length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    byte[] data = Bytes.concat(bytes.toArray(new byte[bytes.size()][]));

                    return Mono.just((TestData) coder.toObject(data));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Mono.just(new TestData(new ArrayList<>()));
        });
    }

    public Mono<ObjectId> addTestData(TestData data, String id) throws Exception {
        byte[] serialized = conf.asByteArray(data);
        String hash = DigestUtils.md5Hex(serialized);
        return mongoLoader.reactiveGridFsTemplate().store(AsyncStreamHelper.toAsyncInputStream(serialized), id, null, new TestDataMeta(hash));
    }

    public Mono<Void> removeTestData(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().delete(new Query(Criteria.where("_id").is(dataId)));
    }

}
