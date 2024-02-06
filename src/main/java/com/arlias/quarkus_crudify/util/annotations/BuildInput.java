package com.arlias.quarkus_crudify.util.annotations;

import com.arlias.quarkus_crudify.input_builder.common.InputBuilder;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Repeatable(BuildInputs.class)
@Target({ElementType.FIELD})
@Retention(RUNTIME)
public @interface BuildInput {

     Class<? extends InputBuilder> value();
     Class<? extends Enum<?>>[] argsTypeEnum() default {};
     String[] onMethods() default {};

}