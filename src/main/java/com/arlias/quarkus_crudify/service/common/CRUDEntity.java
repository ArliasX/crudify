package com.arlias.quarkus_crudify.service.common;


import com.arlias.quarkus_crudify.model.common.PanacheCustomEntity;
import lombok.ToString;

@ToString
public class CRUDEntity {

    public Class<? extends PanacheCustomEntity> entity;

    public boolean isValid(){
        return entity != null;
    }

}
