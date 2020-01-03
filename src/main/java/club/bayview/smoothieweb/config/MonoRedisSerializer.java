package club.bayview.smoothieweb.config;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;

public class MonoRedisSerializer implements RedisSerializer<Object> {
    Kryo kryo = new Kryo();

    MonoRedisSerializer () {
        kryo.register(Mono.class);
        kryo.setRegistrationRequired(false);
    }

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, o);
        output.flush();
        output.close();
        return output.toBytes();
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes.length == 0) return null;
        return kryo.readClassAndObject(new ByteBufferInput(bytes));
    }
}
