package com.arlias.quarkus_crudify.service;

import com.arlias.quarkus_crudify.configuration.ArliasThreadFactory;
import com.arlias.quarkus_crudify.exception.CustomException;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.vertx.web.RoutingExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class TransactionsEnvs {

    public static final String RESPONSE_FIELD = "res_field";
    public static final String HTTP_METHOD = "http_method";
    public static final String CONTEXT = "context";
    public static final String REQUEST_CONTEXT = "req_context";
    public static final String JPA_CONTEXT = "jpa_context";
    public static final String THREAD_POOL = "thread_pool";

    private static volatile TransactionsEnvs instance;

    private final Map<String, EnvMap> transactionsEnvMaps = new HashMap<>();

    private TransactionsEnvs(){
        super();
    }

    public static synchronized ExecutorService initContext(RoutingExchange ex){
        if(instance == null){
            instance = new TransactionsEnvs();
        }
        String uuid = UUID.randomUUID().toString();
        log.debug("Operation [{}] INITIALIZED: Request {}", uuid, ex.request().uri());
        instance.transactionsEnvMaps.put(uuid, EnvMap.autoInitializeFromRoutingContext(ex, uuid));
        return (ExecutorService) instance.transactionsEnvMaps.get(uuid).transactionsEnvs.get(THREAD_POOL);
    }

    public static synchronized void clearCurrentThreadEnvs(){
        if(instance.transactionsEnvMaps.containsKey(Thread.currentThread().getThreadGroup().getName())) {
            RoutingExchange ex = (RoutingExchange) instance.transactionsEnvMaps.get(Thread.currentThread().getThreadGroup().getName())
                    .transactionsEnvs
                    .get(REQUEST_CONTEXT);

            log.debug("Operation [{}] CONSUMED: closing Thread map", Thread.currentThread().getThreadGroup().getName(), ex.request().uri());
            instance.transactionsEnvMaps.remove(Thread.currentThread().getThreadGroup().getName());
        } else {
            log.debug("Operation [{}] FAILED: failed closing Thread map", Thread.currentThread().getThreadGroup().getName());
            CustomException.get(CustomException.ErrorCode.INTERNAL, "Bad Routing").boom();
        }
    }


    public static synchronized <T> T pullProp(String envName){
        if(instance.transactionsEnvMaps.containsKey(Thread.currentThread().getThreadGroup().getName())) {
            RoutingExchange ex = (RoutingExchange) instance.transactionsEnvMaps.get(Thread.currentThread().getThreadGroup().getName())
                    .transactionsEnvs
                    .get(REQUEST_CONTEXT);
            log.debug("Operation [{}]: requested env {}", Thread.currentThread().getThreadGroup().getName(), envName);
            return (T) instance.transactionsEnvMaps.get(Thread.currentThread().getThreadGroup().getName())
                    .transactionsEnvs
                    .get(envName);
        } else {
            log.debug("Operation [{}]: FAILED requesting env {}", Thread.currentThread().getThreadGroup().getName(), envName);
            return null;
        }
    }

    private static class EnvMap {

        private final Map<String, Object> transactionsEnvs = new HashMap<>();

        static synchronized EnvMap autoInitializeFromRoutingContext(RoutingExchange ex, String uuid){
            EnvMap envMap = new EnvMap();
            envMap.transactionsEnvs.put(RESPONSE_FIELD, new ArrayList<>(ex.context().queryParam(RESPONSE_FIELD)));
            envMap.transactionsEnvs.put(HTTP_METHOD, ex.request().method().name());
            envMap.transactionsEnvs.put(REQUEST_CONTEXT, ex);
            envMap.transactionsEnvs.put(CONTEXT, ex.context());
            envMap.transactionsEnvs.put(JPA_CONTEXT, new JpaOperations());
            envMap.transactionsEnvs.put(THREAD_POOL, Executors.newCachedThreadPool(new ArliasThreadFactory(uuid)));
            return envMap;
        }

    }


}

