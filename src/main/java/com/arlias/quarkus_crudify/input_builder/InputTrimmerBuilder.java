package com.arlias.quarkus_crudify.input_builder;

import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class InputTrimmerBuilder implements InputBuilder<String, String> {

    @Override
    public String build(String input) {
        Optional<String> inputOp = Optional.ofNullable(input);

        if(inputOp.isPresent()){
            return inputOp.map(String::trim).get();
        } else {
            return null;
        }
    }
}
