package com.roger.redis.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * A {@link RedisSerializer} implementation that uses <a href="https://github.com/EsotericSoftware/kryo">Kryo 5</a>
 * for fast, compact binary serialization of Redis cache values.
 *
 * <h2>Why Kryo?</h2>
 * <ul>
 *   <li><strong>Compact binary format</strong> — significantly smaller payloads than JSON or JDK serialization,
 *       reducing memory usage in Redis and network bandwidth.</li>
 *   <li><strong>Speed</strong> — serialization and deserialization are considerably faster than
 *       {@link java.io.ObjectOutputStream}/{@link java.io.ObjectInputStream} and most JSON libraries.</li>
 *   <li><strong>No schema required</strong> — unlike Protobuf or Avro, Kryo does not require external schema
 *       definitions, making it ideal for caching arbitrary Java objects.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Kryo instances are <strong>not</strong> thread-safe. This implementation uses a {@link ThreadLocal} to provide
 * each thread with its own pre-configured {@link Kryo} instance, avoiding synchronization overhead while remaining
 * safe in multi-threaded environments such as Spring MVC request processing.</p>
 *
 * <h2>Class Registration</h2>
 * <p>Common JDK collection types are pre-registered for optimal performance. Unregistered classes are still
 * supported (with a small performance penalty) because {@code registrationRequired} is set to {@code false}.</p>
 *
 * @see RedisSerializer
 * @see Kryo
 */
public class KryoRedisSerializer implements RedisSerializer<Object> {

    private static final Logger log = LoggerFactory.getLogger(KryoRedisSerializer.class);

    /**
     * Thread-local Kryo instances, each pre-configured with common class registrations,
     * reference tracking, and relaxed registration mode.
     */
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

        // Allow serialization of classes that are not explicitly registered (just slower).
        kryo.setRegistrationRequired(false);

        // Enable reference tracking to handle circular references correctly.
        kryo.setReferences(true);

        // Pre-register common collection and array types for optimal serialization size and speed.
        kryo.register(ArrayList.class);
        kryo.register(HashMap.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(HashSet.class);
        kryo.register(Object[].class);
        kryo.register(byte[].class);

        return kryo;
    });

    /**
     * Serializes the given object into a byte array using Kryo.
     *
     * @param value the object to serialize; may be {@code null}
     * @return the serialized bytes, or {@code null} if the input is {@code null}
     * @throws SerializationException if serialization fails
     */
    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return null;
        }

        try {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            try (Output output = new Output(baos)) {
                kryo.writeClassAndObject(output, value);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to serialize object of type [{}]: {}",
                    value.getClass().getName(), e.getMessage());
            throw new SerializationException("Failed to serialize object using Kryo", e);
        }
    }

    /**
     * Deserializes the given byte array back into an object using Kryo.
     *
     * @param bytes the serialized bytes; may be {@code null} or empty
     * @return the deserialized object, or {@code null} if the input is {@code null} or empty
     * @throws SerializationException if deserialization fails
     */
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            try (Input input = new Input(bytes)) {
                return kryo.readClassAndObject(input);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize byte array of length [{}]: {}",
                    bytes.length, e.getMessage());
            throw new SerializationException("Failed to deserialize object using Kryo", e);
        }
    }
}
