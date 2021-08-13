/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.pipeline.interfaces;

import de.verdox.vcore.synchronization.pipeline.annotations.VCorePersistentData;
import de.verdox.vcore.util.VCoreUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 25.06.2021 00:48
 */
public interface VCoreSerializable {
    static Set<String> getPersistentDataFieldNames(Class<? extends VCoreSerializable> vCoreDataClass){
        return Arrays.stream(vCoreDataClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(VCorePersistentData.class) != null)
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    static Set<Field> getPersistentDataFields(Class<? extends VCoreSerializable> vCoreDataClass){
        return Arrays.stream(vCoreDataClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(VCorePersistentData.class) != null)
                .collect(Collectors.toSet());
    }

    default Map<String, Object> serialize(){
        Map<String, Object> serializedData = new HashMap<>();
        getPersistentDataFieldNames(getClass()).forEach(dataKey -> {
            try {
                Field field = getClass().getDeclaredField(dataKey);
                field.setAccessible(true);
                //if(field.get(this) == null)
                //    return;
                serializedData.put(field.getName(), field.get(this));
            } catch (NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
        });
        serializedData.remove("_id");
        return serializedData;
    }

    /**
     *
     * @param serializedData Data to update
     * @return Map containing data that was changed with values before the change
     */
    default Map<String, Object> deserialize(Map<String, Object> serializedData){
        Map<String, Object> dataBeforeDeserialization = new HashMap<>();
        serializedData.forEach((key, value) -> {
            if(key.equals("objectUUID"))
                return;
            if(key.equals("_id"))
                return;
            if(value == null)
                return;

            try {
                Field field = getClass().getDeclaredField(key);
                field.setAccessible(true);
                dataBeforeDeserialization.put(key,field.get(this));
                if(!field.getType().isPrimitive()){
                    try{
                        field.set(this, VCoreUtil.getTypeUtil().castData(value,field.getType()));
                    }
                    catch (ClassCastException e){
                        field.set(this,value);
                    }
                }
                else
                    field.set(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                System.err.println("Field e not found. Cleanup Task for missing fields will be implemented in a future release");
            }
        });
        return dataBeforeDeserialization;
    }
}
