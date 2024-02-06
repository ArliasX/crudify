package com.arlias.quarkus_crudify.input_builder;

import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
@Unremovable
public class UUIDBuilder implements InputBuilder<String, String> {
    @Override
    public String build(String s) {
       return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
