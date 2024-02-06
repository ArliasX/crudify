package com.arlias.quarkus_crudify.util.annotations;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Target({ElementType.FIELD})
@Retention(RUNTIME)
public @interface IgnoreInput {

    String[] when() default {};

}