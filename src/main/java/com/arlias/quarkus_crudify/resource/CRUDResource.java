package com.arlias.quarkus_crudify.resource;

import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import com.arlias.quarkus_crudify.service.CRUDManager;
import com.arlias.quarkus_crudify.service.PanacheEntityManager;
import com.arlias.quarkus_crudify.service.TransactionsEnvs;
import com.arlias.quarkus_crudify.service.common.FindAllSelectionType;
import com.arlias.quarkus_crudify.util.SortInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.web.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.MultiMap;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class CRUDResource {

    @Inject
    CRUDManager crudManager;

    @Route(path = "generated/:entity", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.POST)
    public Uni<?> saveEndpoint(RoutingExchange ex, @Param String entity, @Body LinkedHashMap<String, Object> data) throws JsonProcessingException {
        log.info("POST on {} from {}", entity, ex.request().host());
        return Uni.createFrom()
                .item(data)
                .emitOn(TransactionsEnvs.initContext(ex))
                .map(v -> {
                    try {
                        return crudManager.loadManager(entity).save(v);
                    } catch (CustomException e) {
                        ex.response().setStatusCode(e.getErrorCode().code);
                        ex.response().setStatusMessage(e.getParsedErrorMessage());
                        return e.getExtensions();
                    }
                });
    }
//
//    @Route(path = "generated/:entity/merge", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.POST)
//    public Uni<Response> mergeEndpoint(RoutingContext ex.context()) throws JsonProcessingException {
//        String entity = ex.context().pathParam("entity");
//
//        PanacheEntityManager manager = crudManager.loadManager(entity);
//
//        log.info("POST invoked on path {}. Trying to save entity through manager", entity);
//        return Uni.createFrom()
//                .item(new ObjectMapper().readValue(ex.context().getBodyAsString(), manager.getTypeOfDTO()))
//                .emitOn(TransactionsEnvs.initializeForCurrentThread(ex.context(), ex.request()))
//                .map(v -> manager.save(v))
//                .map(e -> e.toJson())
//                .map(Response::ok)
//                .map(Response.ResponseBuilder::build);
//    }


    @Route(path = "generated/:entity", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.GET)
    public Uni<?> findAllEndpoint(RoutingExchange ex, @Param String entity) {

        log.info("GET on {} from {}", entity, ex.request().host());
        return Uni.createFrom()
                .voidItem()
                .emitOn(TransactionsEnvs.initContext(ex))
                .map(v -> {
                    FindAllSelectionType selectionType = FindAllSelectionType.valueOf(ex.getParam("selection_type").orElse("STANDART"));
                    PanacheEntityManager manager = crudManager.loadManager(entity);
                    MultiMap queryParams = ex.context().queryParams();

                    String search = ex.getParam("search").orElse("");
                    int page = ex.getParam("page").map(Integer::parseInt).orElse(0);
                    int size = ex.getParam("size").map(Integer::parseInt).orElse(50);
                    SortInput sort = SortInput.of(
                            Optional.ofNullable(queryParams.getAll("by")).orElse(List.of("id")).toArray(String[]::new),
                            ex.getParam("desc").map(Boolean::parseBoolean).orElse(true)
                    );

                    Map<String, Tuple2<String, String>> filters = queryParams.names().parallelStream()
                            .filter(n -> !List.of("search", "page", "size", "by", "desc", TransactionsEnvs.RESPONSE_FIELD).contains(n))
                            .map(n -> {
                                String filterData = queryParams.get(n);
                                int opIndex = filterData.indexOf(" ");
                                if(opIndex == -1){
                                    return Map.entry(n, Tuple2.of("=", filterData));
                                } else {
                                    String operation = filterData.substring(0, opIndex);
                                    String filter = filterData.substring(opIndex + 1);
                                    return Map.entry(n, Tuple2.of(operation, filter));
                                }
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    return manager.performCrudAndBuildListResponse(() -> {
                        switch (selectionType) {
                            case ARCHIVED:
                                return Tuple2.of(manager.findAllArchived(filters, search, page, size, sort), manager.countAllArchivedPages(filters, search, size));
                            case ALL:
                                return Tuple2.of(manager.findAllGeneral(filters, search, page, size, sort), manager.countAllGeneralPages(filters, search, size));
                            case STANDART:
                            default:
                                return Tuple2.of(manager.findAll(filters, search, page, size, sort), manager.countAllPages(filters, search, size));
                        }
                    });
                });
    }


    @Route(path = "generated/:entity/count", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.GET)
    public Uni<?> countAllEndpoint(RoutingExchange ex, @Param String entity) {

        log.info("GET on {}/count from {}", entity, ex.request().host());
        return Uni.createFrom()
                .voidItem()
                .emitOn(TransactionsEnvs.initContext(ex))
                .map(v -> {
                    FindAllSelectionType selectionType = FindAllSelectionType.valueOf(ex.getParam("selection_type").orElse("STANDART"));
                    PanacheEntityManager manager = crudManager.loadManager(entity);
                    MultiMap queryParams = ex.context().queryParams();

                    String search = ex.getParam("search").orElse("");

                    Map<String, Tuple2<String, String>> filters = queryParams.names().parallelStream()
                            .filter(n -> !List.of("search", "page", "size", "by", "desc", TransactionsEnvs.RESPONSE_FIELD).contains(n))
                            .map(n -> {
                                String filterData = queryParams.get(n);
                                int opIndex = filterData.indexOf(" ");
                                if(opIndex == -1){
                                    return Map.entry(n, Tuple2.of("=", filterData));
                                } else {
                                    String operation = filterData.substring(0, opIndex);
                                    String filter = filterData.substring(opIndex + 1);
                                    return Map.entry(n, Tuple2.of(operation, filter));
                                }
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    Map<String, Long> res = new HashMap<>();

                    switch (selectionType) {
                        case ARCHIVED:
                            res.put("count", manager.countAllArchived(filters, search));
                        case ALL:
                            res.put("count", manager.countAllGeneral(filters, search));
                        case STANDART:
                        default:
                            res.put("count", manager.countAll(filters, search));
                    }

                    return res;

                });
    }


    @Route(path = "generated/:entity/:id", produces = ReactiveRoutes.APPLICATION_JSON, methods = {Route.HttpMethod.PUT, Route.HttpMethod.GET})
    public Uni<?> findSingleOrUpdateEndpoint(RoutingExchange ex, @Body LinkedHashMap<String, Object> data, @Param String entity, @Param Long id) throws JsonProcessingException {

        PanacheEntityManager manager = crudManager.loadManager(entity);
        log.info("{} on {}/{} from", ex.request().method().name(), id, entity, ex.request().host());

        switch (ex.request().method().name()) {
            case "GET":
                FindAllSelectionType selectionType = FindAllSelectionType.valueOf(Optional.ofNullable(ex.context().queryParams().get("selection_type")).orElse("STANDART"));
                switch (selectionType) {
                    case ALL:
                        return Uni.createFrom()
                                .item(id)
                                .emitOn(TransactionsEnvs.initContext(ex))
                                .map(v -> {
                                    try {
                                        PanacheCustomEntity crudResult = manager.findGeneralById(v);
                                        if (crudResult != null) {
                                            return crudResult;
                                        } else {
                                            ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                            ex.response().setStatusMessage("No Result");
                                            return "No Result";
                                        }
                                    } catch (CustomException e) {
                                        ex.response().setStatusCode(e.getErrorCode().code);
                                        ex.response().setStatusMessage(e.getParsedErrorMessage());
                                        return e.getExtensions();
                                    }
                                });
                    case ARCHIVED:
                        return Uni.createFrom()
                                .item(id)
                                .emitOn(TransactionsEnvs.initContext(ex))
                                .map(v -> {
                                    try {
                                        PanacheCustomEntity crudResult = manager.findArchivedById(v);
                                        if (crudResult != null) {
                                            return crudResult.toJson();
                                        } else {
                                            ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                            ex.response().setStatusMessage("No Result");
                                            return "No Result";
                                        }
                                    } catch (CustomException e) {
                                        ex.response().setStatusCode(e.getErrorCode().code);
                                        ex.response().setStatusMessage(e.getParsedErrorMessage());
                                        return e.getExtensions();
                                    }
                                });
                    case STANDART:
                    default:
                        return Uni.createFrom()
                                .item(id)
                                .emitOn(TransactionsEnvs.initContext(ex))
                                .map(v -> {
                                    try {
                                        PanacheCustomEntity crudResult = manager.findById(v);
                                        if (crudResult != null) {
                                            return Response.ok(crudResult.toJson()).build();
                                        } else {
                                            ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                            ex.response().setStatusMessage("No Result");
                                            return "No Result";
                                        }
                                    } catch (CustomException e) {
                                        ex.response().setStatusCode(e.getErrorCode().code);
                                        ex.response().setStatusMessage(e.getParsedErrorMessage());
                                        return e.getExtensions();
                                    }
                                });
                }
            case "PUT":
                return Uni.createFrom()
                        .item(Tuple2.of(id, new ObjectMapper().readValue(ex.context().getBodyAsString(), LinkedHashMap.class)))
                        .emitOn(TransactionsEnvs.initContext(ex))
                        .map(v -> {
                            try {
                                PanacheCustomEntity crudResult = manager.update(v.getItem1(), v.getItem2());
                                if (crudResult != null) {
                                    return Response.ok(crudResult.toJson()).build();
                                } else {
                                    ex.response().setStatusCode(Response.Status.NOT_MODIFIED.getStatusCode());
                                    ex.response().setStatusMessage(Response.Status.NOT_MODIFIED.getReasonPhrase());
                                    return Response.Status.NOT_MODIFIED.getReasonPhrase();
                                }
                            } catch (CustomException e) {
                                ex.response().setStatusCode(e.getErrorCode().code);
                                ex.response().setStatusMessage(e.getParsedErrorMessage());
                                return e.getExtensions();
                            }
                        });
        }

        return Uni.createFrom().voidItem().map(v -> Response.status(Response.Status.METHOD_NOT_ALLOWED).build());
    }


    @Route(path = "generated/:entity/:id", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.DELETE)
    public Uni<?> deleteEndpoint(RoutingExchange ex, @Param String entity, @Param Long id) throws JsonProcessingException {
        log.info("DELETE on {}/{} from", id, entity, ex.request().host());

        return Uni.createFrom()
                .item(id)
                .emitOn(TransactionsEnvs.initContext(ex))
                .map(v -> {
                    boolean hard = ex.getParam("hard").map(Boolean::parseBoolean).orElse(false);
                    PanacheEntityManager manager = crudManager.loadManager(entity);
                    if (hard) {
                        return manager.performCrudAndBuildObjectResponse(() -> manager.hardDelete(v));
                    } else {
                        return manager.performCrudAndBuildObjectResponse(() -> manager.softDelete(v));
                    }
                });
    }

}
