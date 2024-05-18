package com.arlias.quarkus_crudify.input_builder;

import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;
import io.quarkus.arc.Unremovable;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class EmailFormatterAndValidatorBuilder implements InputBuilder<String, String> {

    @Override
    public String build(String mail) {
        Optional<String> mailOp = Optional.ofNullable(mail);

        if(mailOp.isPresent()){
            return mailOp.map(ml -> ml.toLowerCase().trim()).get();
        } else {
            CustomException.get(CustomException.ErrorCode.BAD_REQUEST, "Invalid Mail").boom();
            return null;
        }
    }
}
