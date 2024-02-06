package com.arlias.quarkus_crudify.configuration;

import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.io.IOException;
import java.time.Instant;

public class SerializationPanacheEntityModule extends SimpleModule {

    public SerializationPanacheEntityModule() {
        super(PackageVersion.VERSION);
        this.addSerializer(PanacheCustomEntity.class, new PanacheCustomEntitySerializer());
//        this.addDeserializer(PanacheCustomEntity.class, new PanacheCustomEntityDeserializer());
    }

//
//    static class PanacheCustomEntityDeserializer extends JsonDeserializer<PanacheCustomEntity>{
//
//        @Override
//        public PanacheCustomEntity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
//            return null;
//        }
//    }

    static class PanacheCustomEntitySerializer extends JsonSerializer<PanacheCustomEntity> {

        @Override
        public void serialize(PanacheCustomEntity panacheCustomEntity, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeObject(panacheCustomEntity.toJson());
        }
    }
}
