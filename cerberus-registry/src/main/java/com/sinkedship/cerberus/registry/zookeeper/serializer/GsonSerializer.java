package com.sinkedship.cerberus.registry.zookeeper.serializer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A serializer that uses GSON to serialize/deserialize as JSON.
 *
 * @author Derrick Guan
 */
public class GsonSerializer<T> implements InstanceSerializer<T> {

    private final Gson gson;
    private final Class<T> payloadClass;

    public GsonSerializer(Class<T> payloadClass) {
        this.payloadClass = payloadClass;
        gson = new GsonBuilder()
                .registerTypeAdapter(Class.class, new ClassTypeAdapter())
                .create();
    }

    @Override
    public byte[] serialize(ServiceInstance<T> serviceInstance) throws Exception {
        return gson.toJson(serviceInstance).getBytes();
    }

    @Override
    public ServiceInstance<T> deserialize(byte[] bytes) throws Exception {
        String json = new String(bytes);
        return gson.fromJson(json, getType());
    }

    private Type getType() {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {payloadClass};
            }

            @Override
            public Type getRawType() {
                return ServiceInstance.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    private static final class ClassTypeAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return Class.forName(json.getAsString());
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }
}
