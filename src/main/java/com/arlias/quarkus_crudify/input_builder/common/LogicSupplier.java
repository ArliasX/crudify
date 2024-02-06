package com.arlias.quarkus_crudify.input_builder.common;

import io.vertx.ext.web.RoutingContext;

public interface LogicSupplier {

    <R> void supply(RoutingContext ctx, R data);

}
