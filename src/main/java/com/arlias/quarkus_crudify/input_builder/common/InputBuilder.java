package com.arlias.quarkus_crudify.input_builder.common;

public interface InputBuilder <T, R> {

    R build(T t);

}
