package com.arlias.quarkus_crudify.model.common;

import com.arlias.quarkus_crudify.exception.CustomException;
import com.arlias.quarkus_crudify.input_builder.BooleanConstantFalseBuilder;
import com.arlias.quarkus_crudify.service.TransactionsEnvs;
import com.arlias.quarkus_crudify.util.NullAwareBeanUtilsBean;
import com.arlias.quarkus_crudify.util.annotations.BuildInput;
import com.arlias.quarkus_crudify.util.annotations.IgnoreInput;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.beanutils.BeanUtilsBean;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.*;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@MappedSuperclass
public class PanacheCustomEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Getter
    @Setter
    @IgnoreInput
    public Long id;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    @Getter
    @Setter
    @BuildInput(BooleanConstantFalseBuilder.class)
    @IgnoreInput(when = "PUT")
    public boolean deleted;

    @Transient
    private final List<Field> fields;

    @Transient
    private final List<Method> getters;


    //NOTE added PROTECTED modifier fields 'cause for some reason only id is loaded as PUBLIC
    public PanacheCustomEntity() {
        super();
        fields = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.getModifiers() == Modifier.PROTECTED || f.getModifiers() == Modifier.PUBLIC)
                .collect(Collectors.toList());
        fields.addAll(Arrays.stream(PanacheCustomEntity.class.getDeclaredFields())
                .filter(f -> f.getModifiers() == Modifier.PROTECTED || f.getModifiers() == Modifier.PUBLIC)
                .collect(Collectors.toList()));

        getters = Arrays.stream(this.getClass().getDeclaredMethods())
                .filter(m -> m.getName().startsWith("get"))
                .filter(m -> m.getModifiers() == Modifier.PROTECTED || m.getModifiers() == Modifier.PUBLIC)
                .collect(Collectors.toList());
        getters.addAll(Arrays.stream(PanacheCustomEntity.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("get"))
                .filter(m -> m.getModifiers() == Modifier.PROTECTED || m.getModifiers() == Modifier.PUBLIC)
                .collect(Collectors.toList()));
    }

    public String getMetaType() {
        return this.getClass().getSimpleName();
    }

    public void copy(PanacheCustomEntity insert) {

        BeanUtilsBean notNull = new NullAwareBeanUtilsBean();
        try {
            notNull.copyProperties(this, insert);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void merge() {
        UserTransaction transaction = CDI.current().select(UserTransaction.class).get();
        JpaOperations jpaContext = TransactionsEnvs.pullProp(TransactionsEnvs.JPA_CONTEXT);
        try {
            transaction.begin();
            jpaContext.persist(this);
            jpaContext.flush(this);
            transaction.commit();
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
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().equals(obj.getClass()) && this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {

        List<Object> fieldsData = fields.parallelStream()
                .filter(f -> !f.getName().startsWith("$"))
                .map(f -> {
                    try {
                        if (f.trySetAccessible()) {
                            return f.get(this);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        fieldsData.add(id);

        return Objects.hash(fieldsData);
    }

//    @Override
//    public String toString() {
//        List<Field> fields = Arrays.stream(this.getClass().getDeclaredFields())
//                .collect(Collectors.toList());
//        fields.addAll(Arrays.asList(PanacheCustomEntity.class.getDeclaredFields()));
//
//        return this.getClass().getSimpleName() + "{" +
//                "id=" + id + ", " +
//                fields.parallelStream()
//                        .filter(f -> !f.getName().startsWith("$") && f.getType().getSuperclass() == null)
//                        .map(f -> {
//                            try {
//                                if (f.trySetAccessible()) {
//                                    return f.getName() + "=" + Optional.ofNullable(f.get(this))
//                                            .map(Object::toString)
//                                            .orElse(null);
//                                }
//                            } catch (IllegalAccessException e) {
//                                e.printStackTrace();
//                            }
//                            return null;
//                        }).filter(Objects::nonNull)
//                        .collect(Collectors.joining(", ")) + '}';
//    }


    private String methodToFieldName(Method m) {
        return m.getName().substring(3, 4).toLowerCase().concat(m.getName().substring(4));
    }


    public final Map<String, Object> toJson(String... skipFields) {
        Map<String, Object> res = null;
        try {
            res = toJsonInner(skipFields);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        TransactionsEnvs.clearCurrentThreadEnvs();
        return res;
    }

    @SafeVarargs
    public final Map<String, Object> toJsonInner(String[] skipFieldsName, Class<? extends PanacheCustomEntity>... skipFields) throws ExecutionException, InterruptedException {

        List<String> resFields = TransactionsEnvs.pullProp(TransactionsEnvs.RESPONSE_FIELD);
        Map<String, Object> res = new HashMap<>();

        getters.stream()
                .filter(m -> resFields == null || resFields.isEmpty() || resFields.contains(methodToFieldName(m)))
                .filter(m -> skipFieldsName == null || skipFieldsName.length == 0 || !Arrays.asList(skipFieldsName).contains(methodToFieldName(m)))
                .filter(m -> !m.getReturnType().getSimpleName().equals("List") && !m.getReturnType().getSimpleName().equals("Set") && (m.getReturnType().getSuperclass() == null || !m.getReturnType().getSuperclass().getSimpleName().equals("PanacheCustomEntity")))
                .forEach(m -> {
                    try {
                        if (m.trySetAccessible()) {
                            res.put(methodToFieldName(m), m.invoke(this));
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        System.out.println("Cannot call getter method : " + m.getName());
                        e.printStackTrace();
                    }

                });

        getters.stream()
                .filter(m -> resFields == null || !resFields.contains(methodToFieldName(m)))
                .filter(m -> skipFieldsName == null || skipFieldsName.length == 0 || !Arrays.asList(skipFieldsName).contains(methodToFieldName(m)))
                .filter(m -> m.getReturnType().getSuperclass() != null && m.getReturnType().getSuperclass().getSimpleName().equals("PanacheCustomEntity"))
                .forEach(m -> {
                    try {
                        if (m.trySetAccessible() && !Arrays.asList(skipFields).contains(m.getReturnType())) {
                            Object o = m.invoke(this);
                            if (o != null) {
                                List<Class<? extends PanacheCustomEntity>> runtimeSkipFields = new ArrayList<>(Arrays.asList(skipFields));
                                runtimeSkipFields.add(this.getClass());
                                res.put(methodToFieldName(m), ((PanacheCustomEntity) o).toJsonInner(skipFieldsName, runtimeSkipFields.toArray(new Class[]{})) );
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        System.out.println("Cannot call getter method : " + m.getName());
                        e.printStackTrace();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });


        getters.stream()
                .filter(m -> resFields == null || !resFields.contains(methodToFieldName(m)))
                .filter(m -> skipFieldsName == null || skipFieldsName.length == 0 || !Arrays.asList(skipFieldsName).contains(methodToFieldName(m)))
                .filter(m -> m.getReturnType().getSimpleName().equals("List") || m.getReturnType().getSimpleName().equals("Set"))
                .forEach(m -> {
                    try {
                        if (m.trySetAccessible()) {
                            Collection c = (Collection) m.invoke(this);
                            if (c != null) {
                                List<Map<String, Object>> subRes = new ArrayList<>();
                                for (Object o : c) {
                                    if (!Arrays.asList(skipFields).contains(o.getClass()) && o.getClass().getSuperclass() != null && o.getClass().getSuperclass().getSimpleName().equals("PanacheCustomEntity")) {
                                        subRes.add(((PanacheCustomEntity) o).toJsonInner(skipFieldsName));
                                    }
                                }
                                res.put(methodToFieldName(m), subRes);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Cannot call getter method : " + m.getName());
                        e.printStackTrace();
                    }

                });
        return res;

    }

    public static Map<String, Object> toJson(List<? extends PanacheCustomEntity> entities, int pages, String... skipFields) {

        Map<String, Object> res = new HashMap<>();
        res.put("data", entities.stream()
                .map(e -> {
                    try {
                        return e.toJsonInner(skipFields);
                    } catch (ExecutionException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList()));
        res.put("pages", pages);
        TransactionsEnvs.clearCurrentThreadEnvs();
        return res;
    }


}
