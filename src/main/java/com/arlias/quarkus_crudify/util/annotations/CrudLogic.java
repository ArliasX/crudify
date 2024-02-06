package com.arlias.quarkus_crudify.util.annotations;


import com.arlias.quarkus_crudify.enums.ExecutionPhase;
import com.arlias.quarkus_crudify.input_builder.common.LogicSupplier;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Repeatable(CrudLogics.class)
@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface CrudLogic {

    Class<? extends LogicSupplier> value();

    String[] onMethods() default {};
    ExecutionPhase executionPhase() default ExecutionPhase.WHENEVER;

}
