package com.arlias.quarkus_crudify.input_builder;

import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class BooleanDefaultFalseBuilder implements InputBuilder<Boolean, Boolean> {
    @Override
    public Boolean build(Boolean s) {
       return Optional.ofNullable(s).orElse(false);
    }
}
