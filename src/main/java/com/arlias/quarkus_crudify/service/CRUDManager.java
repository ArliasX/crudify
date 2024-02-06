package com.arlias.quarkus_crudify.service;

import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import com.arlias.quarkus_crudify.service.common.CRUDEntity;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.persistence.Entity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class CRUDManager {

    private final Map<String, CRUDEntity> entitiesClasses = new HashMap<>();

    private final Map<String, PanacheEntityManager<? extends PanacheCustomEntity>> managers = new HashMap<>();

    private static final String ENTITY_PACKAGE_NAME = "com.arlias.quarkus_crudify.model";

    @ConfigProperty(name = "crudify.entitypath")
    String entityPath;


    @PostConstruct
    public void init() {

        log.info("Initializing entity managers configuration");

        for (Class<? extends PanacheCustomEntity> entityClass : new Reflections(ENTITY_PACKAGE_NAME).getSubTypesOf(PanacheCustomEntity.class)) {

            if (entityClass.isAnnotationPresent(Entity.class)) {

                log.info("Configuring entity {}", entityClass.getSimpleName());

                CRUDEntity triade = new CRUDEntity();

                triade.entity = entityClass;

                if (triade.isValid()) {
                    entitiesClasses.put(entityClass.getAnnotation(Entity.class).name(), triade);
                } else {
                    log.warn("Error occurred while configuring entity {}, this entity could be unavailable", entityClass.getSimpleName());
                }
            }
        }

        for (Class<? extends PanacheCustomEntity> entityClass : new Reflections(entityPath).getSubTypesOf(PanacheCustomEntity.class)) {

            if (entityClass.isAnnotationPresent(Entity.class)) {

                log.info("Configuring entity {}", entityClass.getSimpleName());

                CRUDEntity triade = new CRUDEntity();

                triade.entity = entityClass;

                if (triade.isValid()) {
                    entitiesClasses.put(entityClass.getAnnotation(Entity.class).name(), triade);
                } else {
                    log.warn("Error occurred while configuring entity {}, this entity could be unavailable", entityClass.getSimpleName());
                }
            }
        }

    }

    public <ENTITY extends PanacheCustomEntity> PanacheEntityManager<ENTITY> loadManager(String path) {
        if (!managers.containsKey(path)) {
            CRUDEntity triade = entitiesClasses.get(path);
            managers.put(path, new PanacheEntityManager(triade.entity));
        }
        return (PanacheEntityManager<ENTITY>) managers.get(path);
    }

    public <ENTITY extends PanacheCustomEntity> PanacheEntityManager<ENTITY> loadManager(Class<? extends PanacheCustomEntity> clazz) {
        String path = clazz.getAnnotation(Entity.class).name();
        if (!this.managers.containsKey(path)) {
            CRUDEntity triade = (CRUDEntity)this.entitiesClasses.get(path);
            this.managers.put(path, new PanacheEntityManager(triade.entity));
        }

        return (PanacheEntityManager)this.managers.get(path);
    }

}
