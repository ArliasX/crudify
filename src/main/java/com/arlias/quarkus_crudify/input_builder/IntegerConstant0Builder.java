package com.arlias.quarkus_crudify.input_builder;

import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class IntegerConstant0Builder implements InputBuilder<Integer, Integer> {

    @Override
    public Integer build(Integer s) {
       return 0;
    }

}
