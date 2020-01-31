package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.config.SmoothieMongoLoader;
import club.bayview.smoothieweb.models.testdata.StoredTestData;
import club.bayview.smoothieweb.util.NotFoundException;
import com.google.common.primitives.Bytes;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.reactivestreams.client.gridfs.helpers.AsyncStreamHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TestDataRepository {

    @Autowired
    SmoothieMongoLoader mongoLoader;

    public Mono<String> getTestDataHash(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().findOne(new Query(Criteria.where("_id").is(dataId))).flatMap(file -> {
            if (file == null) return Mono.just("");
            return Mono.just(file.getMetadata().getString("hash"));
        });
    }

    // may return error if not test data not found
    public Flux<byte[]> getRawTestDataFlux(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().findOne(new Query(Criteria.where("_id").is(dataId))).flatMap(file -> {
            try {
                return mongoLoader.reactiveGridFsTemplate().getResource(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Mono.error(new NotFoundException());
        }).flatMapMany(file -> {
            try {
                return file.getDownloadStream().map(buffer -> {
                    byte[] b = new byte[buffer.readableByteCount()];
                    buffer.read(b);
                    return b;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Flux.error(new NotFoundException());
        });
    }

    public Mono<byte[]> getRawTestData(String dataId) throws Exception {
        return getRawTestDataFlux(dataId).collectList().flatMap(bytes -> {
            byte[] data = Bytes.concat(bytes.toArray(new byte[bytes.size()][]));
            return Mono.just(data);
        }).onErrorResume(e -> {
            if (!(e instanceof NotFoundException)) {
                e.printStackTrace();
            }
            return Mono.just(new byte[0]);
        });
    }

    public Mono<StoredTestData.TestData> getTestData(String dataId) throws Exception {
        return getRawTestData(dataId).flatMap(bytes -> {
            if (bytes.length == 0) {
                return Mono.just(StoredTestData.TestData.getDefaultInstance());
            }
            try {
                return Mono.just(StoredTestData.TestData.parseFrom(bytes));
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return Mono.just(StoredTestData.TestData.getDefaultInstance());
        });
    }

    public Mono<ObjectId> addTestData(StoredTestData.TestData data, String id) throws Exception {
        byte[] serialized = data.toByteArray();
        String hash = DigestUtils.md5Hex(serialized);
        return mongoLoader.reactiveGridFsTemplate().store(AsyncStreamHelper.toAsyncInputStream(serialized), id, null, new TestDataMeta(hash));
    }

    public Mono<Void> removeTestData(String dataId) throws Exception {
        return mongoLoader.reactiveGridFsTemplate().delete(new Query(Criteria.where("_id").is(dataId)));
    }

}
