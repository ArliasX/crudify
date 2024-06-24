package com.arlias.quarkus_crudify.resource;

import com.arlias.quarkus_crudify.enums.ExecutionPhase;
import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import com.arlias.quarkus_crudify.service.CRUDManager;
import com.arlias.quarkus_crudify.service.PanacheEntityManager;
import com.arlias.quarkus_crudify.service.TransactionsEnvs;
import com.arlias.quarkus_crudify.service.common.FindAllSelectionType;
import com.arlias.quarkus_crudify.util.CrudifyFramework;
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
        log.info("POST on {} from {}", entity, ex.request().remoteAddress().host());

        return CrudifyFramework.buildResponse(ex, () -> {
            try {
                return crudManager.loadManager(entity).save(data);
            } catch (CustomException e) {
                ex.response().setStatusCode(e.getErrorCode().code);
                ex.response().setStatusMessage(e.getParsedErrorMessage());
                return e.getExtensions();
            }
        });
    }

    @Route(path = "generated/:entity", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.GET)
    public Uni<?> findAllEndpoint(RoutingExchange ex, @Param String entity) {

        log.info("GET on {} from {}", entity, ex.request().remoteAddress().host());

        return CrudifyFramework.buildResponse(ex, () -> {

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

        log.info("GET on {}/count from {}", entity, ex.request().remoteAddress().host());

        return CrudifyFramework.buildResponse(ex, () -> {

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

        log.info("{} on {}/{} from", ex.request().method().name(), id, entity, ex.request().remoteAddress().host());

        return CrudifyFramework.buildResponse(ex, () -> {

            PanacheEntityManager manager = crudManager.loadManager(entity);
            PanacheCustomEntity crudResult = null;
            switch (ex.request().method().name()) {
                case "GET":
                    FindAllSelectionType selectionType = FindAllSelectionType.valueOf(Optional.ofNullable(ex.context().queryParams().get("selection_type")).orElse("STANDART"));
                    switch (selectionType) {
                        case ALL:
                            crudResult = manager.findGeneralById(id);
                            if (crudResult != null) {
                                return crudResult;
                            } else {
                                ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                ex.response().setStatusMessage("No Result");
                                return "No Result";
                            }
                        case ARCHIVED:
                            crudResult = manager.findArchivedById(id);
                            if (crudResult != null) {
                                return crudResult.toJson();
                            } else {
                                ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                ex.response().setStatusMessage("No Result");
                                return "No Result";
                            }
                        case STANDART:
                        default:
                            crudResult = manager.findById(id);
                            if (crudResult != null) {
                                return Response.ok(crudResult.toJson()).build();
                            } else {
                                ex.response().setStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                                ex.response().setStatusMessage("No Result");
                                return "No Result";
                            }
                    }
                case "PUT":
                    crudResult = manager.update(id, new ObjectMapper().readValue(ex.context().getBodyAsString(), LinkedHashMap.class));
                    if (crudResult != null) {
                        return Response.ok(crudResult.toJson()).build();
                    } else {
                        ex.response().setStatusCode(Response.Status.NOT_MODIFIED.getStatusCode());
                        ex.response().setStatusMessage(Response.Status.NOT_MODIFIED.getReasonPhrase());
                        return Response.Status.NOT_MODIFIED.getReasonPhrase();
                    }
            }

            CustomException.get(CustomException.ErrorCode.UNVAILABLE, "UNVAILABLE").boom();
            return "";
        });
    }


    @Route(path = "generated/:entity/:id", produces = ReactiveRoutes.APPLICATION_JSON, methods = Route.HttpMethod.DELETE)
    public Uni<?> deleteEndpoint(RoutingExchange ex, @Param String entity, @Param Long id) throws JsonProcessingException {
        log.info("DELETE on {}/{} from", id, entity, ex.request().remoteAddress().host());

        return CrudifyFramework.buildResponse(ex, () -> {
            boolean hard = ex.getParam("hard").map(Boolean::parseBoolean).orElse(false);
            PanacheEntityManager manager = crudManager.loadManager(entity);
            if (hard) {
                return manager.performCrudAndBuildObjectResponse(() -> {
                    manager.performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, manager.findById(id));
                    return manager.hardDelete(id);
                });
            } else {
                manager.performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, manager.findById(id));
                return manager.performCrudAndBuildObjectResponse(() -> manager.softDelete(id));
            }
        });
    }

}
