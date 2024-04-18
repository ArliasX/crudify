package com.arlias.quarkus_crudify.service;

import com.arlias.quarkus_crudify.enums.ExecutionPhase;
import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import com.arlias.quarkus_crudify.util.SortInput;
import com.arlias.quarkus_crudify.util.annotations.*;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Page;
import io.quarkus.vertx.web.RoutingExchange;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PanacheEntityManager<ENTITY extends PanacheCustomEntity> {

    @Getter
    private final Class<ENTITY> typeOfENTITY;
    private final List<String> stringFields;
    private final List<String> dataFields;
    private final List<String> entityBeanFields;
    private final Map<String, Field> entityFields;

    public String getSearchQueryParams() {
        return stringFields.parallelStream()
                .map(sf -> " or lower(" + sf + ") like LOWER(:search) ")
                .collect(Collectors.joining(" "));
    }

    public String getFilterQueryParams(String field, String operation) {
        return entityBeanFields.parallelStream()
                .filter(df -> df.equals(field))
                .map(sf -> " or " + sf + " " + operation + " :" + sf + " ")
                .collect(Collectors.joining(" "));
    }


    public PanacheEntityManager(Class<ENTITY> entityClass) {
        super();
        this.typeOfENTITY = entityClass;

        List<Field> fields = Arrays.stream(typeOfENTITY.getDeclaredFields())
                .collect(Collectors.toList());
        fields.addAll(Arrays.asList(PanacheCustomEntity.class.getDeclaredFields()));

        entityFields = new HashMap<>();
        fields.forEach(f -> entityFields.put(f.getName(), f));

        stringFields = fields.parallelStream()
                .filter(f -> f.getType().getSimpleName().equals(String.class.getSimpleName()))
                .map(f -> {
                    String fieldName = "";
                    if (f.isAnnotationPresent(Column.class)) {
                        fieldName = f.getAnnotation(Column.class).name();
                    }
                    if (fieldName == null || fieldName.isBlank()) {
                        fieldName = f.getName();
                    }
                    return fieldName;
                })
                .collect(Collectors.toList());

        dataFields = fields.parallelStream()
                .filter(f -> !f.getName().equalsIgnoreCase("id"))
                .map(f -> {
                    String fieldName = "";
                    if (f.isAnnotationPresent(Column.class)) {
                        fieldName = f.getAnnotation(Column.class).name();
                    }
                    if (fieldName == null || fieldName.isBlank()) {
                        fieldName = f.getName();
                    }
                    return fieldName;
                })
                .collect(Collectors.toList());

        entityBeanFields = fields.parallelStream()
                .map(Field::getName)
                .filter(name -> !name.equalsIgnoreCase("id"))
                .collect(Collectors.toList());

    }


    public PanacheEntityManager() {
        super();
        this.typeOfENTITY = (Class<ENTITY>)
                ((ParameterizedType) getClass()
                        .getSuperclass()
                        .getGenericSuperclass())
                        .getActualTypeArguments()[0];

        List<Field> fields = Arrays.stream(typeOfENTITY.getDeclaredFields())
                .collect(Collectors.toList());
        fields.addAll(Arrays.asList(PanacheCustomEntity.class.getDeclaredFields()));

        entityFields = new HashMap<>();
        fields.forEach(f -> entityFields.put(f.getName(), f));

        stringFields = fields.parallelStream()
                .filter(f -> f.getType().getSimpleName().equals(String.class.getSimpleName()))
                .map(f -> {
                    String fieldName = "";
                    if (f.isAnnotationPresent(Column.class)) {
                        fieldName = f.getAnnotation(Column.class).name();
                    }
                    if (fieldName == null || fieldName.isBlank()) {
                        fieldName = f.getName();
                    }
                    return fieldName;
                })
                .collect(Collectors.toList());

        dataFields = fields.parallelStream()
                .filter(f -> !f.getName().equalsIgnoreCase("id"))
                .map(f -> {
                    String fieldName = "";
                    if (f.isAnnotationPresent(Column.class)) {
                        fieldName = f.getAnnotation(Column.class).name();
                    }
                    if (fieldName == null || fieldName.isBlank()) {
                        fieldName = f.getName();
                    }
                    return fieldName;
                })
                .collect(Collectors.toList());

        entityBeanFields = fields.parallelStream()
                .map(Field::getName)
                .filter(name -> !name.equalsIgnoreCase("id"))
                .collect(Collectors.toList());
    }

    public List<ENTITY> bulkSave(List<LinkedHashMap<String, Object>> input) throws CustomException {
        return bulkSave(input.stream());
    }

    public List<ENTITY> bulkSave(Stream<LinkedHashMap<String, Object>> input) throws CustomException{
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> entities = input.sequential()
                .map(this::toENTITY)
                .peek(e -> performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, e))
                .collect(Collectors.toList());
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            jpaContext.persist(entities);
            jpaContext.flush(entities);
            entities = entities.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.DURING_TRANSACTION, e))
                    .collect(Collectors.toList());
            transaction.commit();
            entities = entities.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, e))
                    .collect(Collectors.toList());
            return entities;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public List<ENTITY> saveAll(List<ENTITY> input) throws CustomException{
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> entities = input.stream()
                .peek(e -> performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, e))
                .collect(Collectors.toList());
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            jpaContext.persist(entities);
            jpaContext.flush(entities);
            entities = entities.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.DURING_TRANSACTION, e))
                    .collect(Collectors.toList());
            transaction.commit();
            entities = entities.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, e))
                    .collect(Collectors.toList());
            return entities;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public ENTITY save(LinkedHashMap<String, Object> input) throws CustomException{
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY entity = toENTITY(input);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, entity);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }


    public ENTITY save(ENTITY entity) throws CustomException{
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, entity);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

//    @ExceptionChecked
//    public ENTITY merge(DTO input){
//        log.info("Merge Input {}", input);
//        ENTITY parsedInput = getOrInjectMapper().toENTITY(input);
//        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
//        try {
//            transaction.begin();
//            ENTITY entity = findByMatchingProps(extractNotNullProps(parsedInput));
//            if(entity == null){
//                jpaContext.persist(parsedInput);
//                jpaContext.flush(parsedInput);
//            }
//            entity.copy(parsedInput);
//            jpaContext.persist(entity);
//            jpaContext.flush(entity);
//            transaction.commit();
//            return entity;
//        } catch (Exception e) {
//            e.printStackTrace();
//            // do something on Tx failure
//            try {
//                transaction.rollback();
//            } catch (SystemException ex) {
//                ex.printStackTrace();
//            }
//            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
//        }
//        return null;
//    }

    public ENTITY update(Long id, ENTITY parsedInput) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, parsedInput);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            ENTITY entity = findById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            entity.copy(parsedInput);
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public ENTITY update(Long id, LinkedHashMap<String, Object> input) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY parsedInput = toENTITY(input);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, parsedInput);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            ENTITY entity = findById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            entity.copy(parsedInput);
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public ENTITY rawUpdate(Long id, ENTITY parsedInput) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, parsedInput);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            ENTITY entity = findById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            entity.rawCopy(parsedInput);
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public ENTITY rawUpdate(Long id, LinkedHashMap<String, Object> input) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY parsedInput = toENTITY(input);
        performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, parsedInput);
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            ENTITY entity = findById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            entity.rawCopy(parsedInput);
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            performMethodLogic(ExecutionPhase.DURING_TRANSACTION, entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public List<ENTITY> bulkMerge(List<LinkedHashMap<String, Object>> input) throws CustomException {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> parsedInputs = input.stream()
                .map(this::toMergeENTITY)
                .peek(e -> performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, e))
                .collect(Collectors.toList());

        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            List<ENTITY> storedEntities = findAllByIds(parsedInputs.parallelStream().map(e -> e.id).collect(Collectors.toList()));
            List<ENTITY> toSave = new ArrayList<>();
            parsedInputs.stream()
                .forEach(e -> {
                    if(e.id != null){
                        ENTITY stored = storedEntities.parallelStream()
                                .filter(se -> Objects.equals(se.id, e.id))
                                .findFirst()
                                .get();
                        stored.copy(e);
                        toSave.add(stored);
                    } else {
                        toSave.add(e);
                    }
                });
            List<ENTITY> finalToSave = toSave;

            jpaContext.persist(finalToSave);
            jpaContext.flush(finalToSave);
            finalToSave = finalToSave.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.DURING_TRANSACTION, e))
                    .collect(Collectors.toList());
            transaction.commit();
            finalToSave = finalToSave.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, e))
                    .collect(Collectors.toList());
            return finalToSave;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }


    public List<ENTITY> merge(List<ENTITY> input) throws CustomException {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> parsedInputs = input.stream()
                .peek(e -> performMethodLogic(ExecutionPhase.BEFORE_TRANSACTION, e))
                .collect(Collectors.toList());

        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        try {
            transaction.begin();
            List<ENTITY> storedEntities = findAllByIds(parsedInputs.parallelStream().map(e -> e.id).collect(Collectors.toList()));
            List<ENTITY> toSave = new ArrayList<>();
            parsedInputs.stream()
                    .forEach(e -> {
                        if(e.id != null){
                            ENTITY stored = storedEntities.parallelStream()
                                    .filter(se -> Objects.equals(se.id, e.id))
                                    .findFirst()
                                    .get();
                            stored.copy(e);
                            toSave.add(stored);
                        } else {
                            toSave.add(e);
                        }
                    });
            List<ENTITY> finalToSave = toSave;

            jpaContext.persist(finalToSave);
            jpaContext.flush(finalToSave);
            finalToSave = finalToSave.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.DURING_TRANSACTION, e))
                    .collect(Collectors.toList());
            transaction.commit();
            finalToSave = finalToSave.stream()
                    .peek(e -> performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, e))
                    .collect(Collectors.toList());
            return finalToSave;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public boolean hardDelete(Long id) {
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        try {
            transaction.begin();
            ENTITY entity = findGeneralById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            jpaContext.delete(entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return false;
    }

    public boolean softDelete(Long id) {
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        try {
            transaction.begin();
            ENTITY entity = findById(id);
            if (entity == null) {
                CustomException.get(CustomException.ErrorCode.NOT_FOUND, "Entity {} with id {} is not present in the database", typeOfENTITY.getSimpleName(), id).boom();
            }
            entity.deleted = true;
            jpaContext.persist(entity);
            jpaContext.flush(entity);
            transaction.commit();
            performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return false;
    }


    public ENTITY findByCondition(String query, Map<String, Object> params) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY entity = (ENTITY) jpaContext.find(typeOfENTITY, query, params).firstResult();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
        return entity;
    }

    public List<ENTITY> findAllByCondition(String query, Map<String, Object> params) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> entities = (List<ENTITY>) jpaContext.find(typeOfENTITY, query, params).list();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entities);
        return entities;
    }

    public long hardDeleteAllByCondition(String query, Map<String, Object> params) {
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        try {
            transaction.begin();
            long deletedEntities = jpaContext.delete(typeOfENTITY, query, params);
            transaction.commit();
            return deletedEntities;
        } catch (Exception e) {
            e.printStackTrace();
            // do something on Tx failure
            try {
                transaction.rollback();
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return 0;
    }

    public ENTITY findGeneralById(Long id) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY entity = (ENTITY) jpaContext.find(typeOfENTITY, "id = :id", Map.of("id", id)).firstResult();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
        return entity;
    }

    public ENTITY findById(Long id) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY entity =  (ENTITY) jpaContext.find(typeOfENTITY, "deleted = false and id = :id", Map.of("id", id)).firstResult();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
        return entity;
    }

    public List<ENTITY> findAllByIds(List<Long> ids) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        List<ENTITY> entities = (List<ENTITY>) jpaContext.find(typeOfENTITY, "deleted = false and id in :ids", Map.of("ids", ids)).list();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entities);
        return entities;
    }

    public ENTITY findArchivedById(Long id) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        ENTITY entity =  (ENTITY) jpaContext.find(typeOfENTITY, "deleted = true and id = :id", Map.of("id", id)).firstResult();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, entity);
        return entity;
    }

    public List<ENTITY> findAllGeneral(Map<String, Tuple2<String, String>> filters, String search, int page, int size, SortInput sort) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "(:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        List<ENTITY> result = (List<ENTITY>) jpaContext.find(typeOfENTITY, query, SortInput.getSortOrDefault(sort), filterParams).page(Page.of(page, size)).list();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, result);
        return result;
    }

    public List<ENTITY> findAll(Map<String, Tuple2<String, String>> filters, String search, int page, int size, SortInput sort) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = false and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        List<ENTITY> result = (List<ENTITY>) jpaContext.find(typeOfENTITY, query, SortInput.getSortOrDefault(sort), filterParams).page(Page.of(page, size)).list();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, result);
        return result;
    }

    public List<ENTITY> findAllArchived(Map<String, Tuple2<String, String>> filters, String search, int page, int size, SortInput sort) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = true and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        List<ENTITY> result = (List<ENTITY>) jpaContext.find(typeOfENTITY, query, SortInput.getSortOrDefault(sort), filterParams).page(Page.of(page, size)).list();
        performMethodLogic(ExecutionPhase.AFTER_TRANSACTION, result);
        return result;
    }

    public long countAll(Map<String, Tuple2<String, String>> filters, String search) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = false and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return jpaContext.count(typeOfENTITY, query, filterParams);
    }

    public long countAllArchived(Map<String, Tuple2<String, String>> filters, String search) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = true and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return jpaContext.count(typeOfENTITY, query, filterParams);
    }

    public long countAllGeneral(Map<String, Tuple2<String, String>> filters, String search) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "(:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return jpaContext.count(typeOfENTITY, query, filterParams);
    }


    public int countAllPages(Map<String, Tuple2<String, String>> filters, String search, int size) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = false and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return getPagesCount(jpaContext.count(typeOfENTITY, query, filterParams), size);
    }

    public int countAllArchivedPages(Map<String, Tuple2<String, String>> filters, String search, int size) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "deleted = true and (:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return getPagesCount(jpaContext.count(typeOfENTITY, query, filterParams), size);
    }

    public int countAllGeneralPages(Map<String, Tuple2<String, String>> filters, String search, int size) {
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        String query = "(:search = '' " + getSearchQueryParams() + ")";
        Map<String, Object> filterParams = new HashMap<>();
        filterParams.put("search", search.isBlank() ? "" : ("%" + search + "%"));

        for (Map.Entry<String, Tuple2<String, String>> filter : filters.entrySet()) {
            query = query.concat(" and " + filter.getKey() + " " + filter.getValue().getItem1() + " :" + filter.getKey());
            filterParams.put(filter.getKey(), getFilterParamParsed(filter.getKey(), filter.getValue().getItem2()));
        }
        return getPagesCount(jpaContext.count(typeOfENTITY, query, filterParams), size);
    }

    private int getPagesCount(long count, double size) {
        return (int) Math.ceil(count == 0 ? 0 : count / size);
    }


    public ENTITY toMergeENTITY(LinkedHashMap<String, Object> input) {
        try {

            log.debug("Operation [{}]: Parsing input : {}", Thread.currentThread().getThreadGroup().getName(), input);

            String method = TransactionsEnvs.pullProp(TransactionsEnvs.HTTP_METHOD);
            ENTITY res = typeOfENTITY.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> fieldStruct : entityFields.entrySet()) {
                Field f = fieldStruct.getValue();
                if (f.getName().equals("id")
                        || !f.isAnnotationPresent(IgnoreInput.class)
                        || (f.getAnnotation(IgnoreInput.class).when().length > 0 &&
                                !List.of(f.getAnnotation(IgnoreInput.class).when()).contains(method))) {

                    AtomicReference<Object> value = new AtomicReference<>(input.get(fieldStruct.getKey()));
                    if (f.isAnnotationPresent(BuildInput.class) || f.isAnnotationPresent(BuildInputs.class)) {
                        Stream.of(f.getAnnotationsByType(BuildInput.class))
                                .filter(bi -> bi.onMethods().length == 0 || List.of(bi.onMethods()).contains(method))
                                .forEach(bi -> value.set(CDI.current().select(bi.value()).get().build(value.get())));
                    }

                    if (f.trySetAccessible()) {
                        Object valueData = value.get();
                        if (valueData != null) {
                            //This check is due to an IllegalArgumentException laucnhed on Long casting I don't know why
                            if(f.getType().equals(Long.class) && !(valueData instanceof Long)){
                                f.set(res, Long.valueOf(String.valueOf(valueData)));
                            } else if(f.getType().isEnum()) {
                                f.set(res, Enum.valueOf((Class<Enum>) f.getType(), String.valueOf(valueData)));
                            } else {
                                f.set(res, valueData);
                            }
                        } else {
                            log.warn("Operation [{}]: Field {} setted to null", Thread.currentThread().getThreadGroup().getName(), fieldStruct.getKey());
                        }
                    } else if(value.get() != null){
                        CustomException.get(CustomException.ErrorCode.INTERNAL, "Cannot access field {}", f.getName()).boom();
                    }
                }
            }

            return res;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }

    public ENTITY toENTITY(LinkedHashMap<String, Object> input) {
        try {

            log.debug("Operation [{}]: Parsing input : {}", Thread.currentThread().getThreadGroup().getName(), input);

            String method = TransactionsEnvs.pullProp(TransactionsEnvs.HTTP_METHOD);
            ENTITY res = typeOfENTITY.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> fieldStruct : entityFields.entrySet()) {
                Field f = fieldStruct.getValue();
                if (!f.isAnnotationPresent(IgnoreInput.class) ||
                        (f.getAnnotation(IgnoreInput.class).when().length > 0 &&
                                !List.of(f.getAnnotation(IgnoreInput.class).when()).contains(method))) {

                    AtomicReference<Object> value = new AtomicReference<>(input.get(fieldStruct.getKey()));
                    if (f.isAnnotationPresent(BuildInput.class) || f.isAnnotationPresent(BuildInputs.class)) {
                        Stream.of(f.getAnnotationsByType(BuildInput.class))
                                .filter(bi -> bi.onMethods().length == 0 || List.of(bi.onMethods()).contains(method))
                                .forEach(bi -> value.set(CDI.current().select(bi.value()).get().build(value.get())));
                    }

                    if (f.trySetAccessible()) {
                        Object valueData = value.get();
                        if (valueData != null) {
                            //This check is due to an IllegalArgumentException laucnhed on Long casting I don't know why
                            if(f.getType().equals(Long.class) && !(valueData instanceof Long)){
                                f.set(res, Long.valueOf(String.valueOf(valueData)));
                            } else if(f.getType().isEnum()) {
                                f.set(res, Enum.valueOf((Class<Enum>) f.getType(), String.valueOf(valueData)));
                            } else {
                                f.set(res, valueData);
                            }
                        } else {
                            log.warn("Operation [{}]: Field {} setted to null", Thread.currentThread().getThreadGroup().getName(), fieldStruct.getKey());
                        }
                    } else if(value.get() != null){
                        CustomException.get(CustomException.ErrorCode.INTERNAL, "Cannot access field {}", f.getName()).boom();
                    }
                }
            }

            return res;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
            CustomException.get(CustomException.ErrorCode.INTERNAL, e).boom();
        }
        return null;
    }


    public <R> void performMethodLogic(ExecutionPhase executionPhase, R entity) {
        RoutingContext ctx = TransactionsEnvs.pullProp(TransactionsEnvs.CONTEXT);
        String method = TransactionsEnvs.pullProp(TransactionsEnvs.HTTP_METHOD);
        if(ctx != null) {
            if (typeOfENTITY.isAnnotationPresent(CrudLogic.class) || typeOfENTITY.isAnnotationPresent(CrudLogics.class)) {
                for (CrudLogic cl : typeOfENTITY.getAnnotationsByType(CrudLogic.class)) {
                    if (executionPhase.equals(cl.executionPhase()) && (cl.onMethods().length == 0 || List.of(cl.onMethods()).contains(method))) {
                        CDI.current().select(cl.value()).get().supply(ctx, entity);
                    }
                }
            }
        }
    }



    public <R> Object performCrudAndBuildObjectResponse(Supplier<R> crud) {
        RoutingExchange ex = TransactionsEnvs.pullProp(TransactionsEnvs.REQUEST_CONTEXT);

        try {
            R crudResult = crud.get();
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
    }

    public <R extends Tuple2<List<PanacheCustomEntity>, Integer>> Object performCrudAndBuildListResponse(Supplier<R> crud) {

        RoutingExchange ex = TransactionsEnvs.pullProp(TransactionsEnvs.REQUEST_CONTEXT);

        try {
            R crudResult = crud.get();
            if (crudResult != null) {
                return PanacheCustomEntity.toJson(crudResult.getItem1(), crudResult.getItem2());
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
    }

//    private ENTITY findByMatchingProps(Map<String, Object> props){
//        StringBuilder query = new StringBuilder("");
//        int cont = 0;
//        for (String prop : props.keySet()) {
//            if(cont > 0)
//                query.append(" and ");
//            query.append(prop);
//            query.append(" = :");
//            query.append(prop);
//            cont++;
//        }
//        return (ENTITY) jpaContext.find(typeOfENTITY, query.toString(), props).firstResult();
//    }
//
//
//    private Map<String, Object> extractNotNullProps(ENTITY entity){
//        Map<String, Object> res = new HashMap<>();
//        List<Field> fields = Arrays.stream(typeOfENTITY.getDeclaredFields())
//                .collect(Collectors.toList());
//
//        for (String prop : dataFields) {
//            fields.parallelStream().filter(f -> f.getName().equalsIgnoreCase(prop)).findFirst()
//                    .ifPresent(f -> {
//                        try {
//                            if(f.trySetAccessible()){
//                                Object o = f.get(entity);
//                                if(o != null){
//                                    res.put(prop, o);
//                                }
//                            }
//                        } catch (Exception e){}
//                    });
//        }
//
//        System.out.println("extracted fields : " + res);
//
//        return res;
//    }

    public Object getFilterParamParsed(String paramName, String data){
        try {

        switch (typeOfENTITY.getDeclaredField(paramName).getType().getSimpleName()){
            case "Long":
                return Long.parseLong(data);
            case "Integer":
                return Integer.parseInt(data);
            case "Boolean":
                return Boolean.parseBoolean(data);
            case "Double":
                return Double.parseDouble(data);
            case "Float":
                return Float.parseFloat(data);
            default:
                return data;
        }
        } catch (NoSuchFieldException e) {
            CustomException.get(CustomException.ErrorCode.BAD_REQUEST, e).boom();
        }
        return null;
    }

}
