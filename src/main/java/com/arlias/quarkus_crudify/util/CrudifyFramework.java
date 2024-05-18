package com.arlias.quarkus_crudify.util;

import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.service.TransactionsEnvs;
import io.quarkus.vertx.web.RoutingExchange;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import javax.ws.rs.core.Response;

public class CrudifyFramework {


    public static synchronized Uni<?> buildResponse(RoutingExchange ex, Producer producer){
        return Uni.createFrom()
                .voidItem()
                .emitOn(TransactionsEnvs.initContext(ex))
                .map((v) -> Unchecked.unchecked( () -> {
                    try {
                        return producer.produce();
                    } catch (CustomException ce){
                        CustomException.get(ce.getErrorCode(), ce.getParsedErrorMessage()).boom();
                    } catch (Exception e){
                        CustomException.get(CustomException.ErrorCode.INTERNAL, e.getMessage()).boom();
                    }
                    return "";
                }))
                .map(us -> CustomException.parseResponse(ex, us));
    }


    public static synchronized Uni<Response> buildResponse(HttpServerRequest request, HttpServerResponse response, Producer producer){
        return Uni.createFrom()
                .voidItem()
                .emitOn(TransactionsEnvs.initAsyncDirectContext(request))
                .map((v) -> {
                    try {
                        return Response.ok(producer.produce()).build();
                    } catch (CustomException ce){
                        return Response.status(ce.getErrorCode().code).entity(ce.getParsedErrorMessage()).build();
                    } catch (Throwable t){
                        return Response.status(500).entity(t.getCause()).build();
                    }
                });
    }


    @FunctionalInterface
    public interface Producer {
        Object produce() throws Exception;
    }

}
