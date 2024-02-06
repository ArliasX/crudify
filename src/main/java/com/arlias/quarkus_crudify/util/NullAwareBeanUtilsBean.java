package com.arlias.quarkus_crudify.util;

import org.apache.commons.beanutils.BeanUtilsBean;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.InvocationTargetException;

@ApplicationScoped
public class NullAwareBeanUtilsBean extends BeanUtilsBean {

    @Override
    public void copyProperty(Object dest, String name, Object value)
            throws IllegalAccessException, InvocationTargetException {
        if(value==null)return;
        super.copyProperty(dest, name, value);
    }
}
