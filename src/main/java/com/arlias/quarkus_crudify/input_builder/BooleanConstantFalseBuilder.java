package com.arlias.quarkus_crudify.input_builder;

import io.quarkus.arc.Unremovable;
import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class BooleanConstantFalseBuilder implements InputBuilder<Boolean, Boolean> {

    @Override
    public Boolean build(Boolean s) {
       return false;
    }

}
