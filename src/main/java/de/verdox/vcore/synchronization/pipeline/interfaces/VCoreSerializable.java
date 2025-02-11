/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.pipeline.interfaces;

import de.verdox.vcore.synchronization.pipeline.annotations.VCorePersistentData;
import de.verdox.vcore.synchronization.pipeline.datatypes.CustomPipelineData;
import de.verdox.vcore.util.VCoreUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 25.06.2021 00:48
 */
public interface VCoreSerializable {
    static Set<String> getPersistentDataFieldNames(Class<? extends VCoreSerializable> vCoreDataClass) {
        return Arrays.stream(vCoreDataClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(VCorePersistentData.class) != null)
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    static Set<Field> getPersistentDataFields(Class<? extends VCoreSerializable> vCoreDataClass) {
        return Arrays.stream(vCoreDataClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(VCorePersistentData.class) != null)
                .collect(Collectors.toSet());
    }

    @NotNull
    default Map<String, Object> serialize() {
        Map<String, Object> serializedData = new HashMap<>();
        getPersistentDataFieldNames(getClass()).forEach(dataKey -> {
            try {
                Field field = getClass().getDeclaredField(dataKey);
                field.setAccessible(true);
                Object data = field.get(this);
                if (data instanceof Collection<?>) {
                    Collection<?> collection = (Collection<?>) data;
                    serializedData.put(field.getName(), new ArrayList<>(collection));
                } else if (data instanceof CustomPipelineData) {
                    serializedData.put(dataKey, ((CustomPipelineData) data).getUnderlyingMap());
                } else
                    serializedData.put(field.getName(), field.get(this));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        serializedData.remove("_id");
        return serializedData;
    }

    /**
     * @param serializedData Data to update
     * @return Map containing data that was changed with values before the change
     */
    default Map<String, Object> deserialize(Map<String, Object> serializedData) {
        Map<String, Object> dataBeforeDeserialization = new HashMap<>();
        serializedData.forEach((key, value) -> {
            if (key.equals("objectUUID"))
                return;
            if (key.equals("_id"))
                return;

            try {
                Field field = getClass().getDeclaredField(key);
                field.setAccessible(true);
                dataBeforeDeserialization.put(key, field.get(this));
                if (value == null)
                    field.set(this, null);
                if (!field.getType().isPrimitive()) {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        // Instantiation of Collection if value is null in the first place
                        if (field.get(this) == null)
                            field.set(field.getType().getConstructor().newInstance(), this);
                        Collection<?> fieldCollection = (Collection<?>) field.get(this);
                        fieldCollection.clear();
                        fieldCollection.addAll((Collection) value);
                    } else if (CustomPipelineData.class.isAssignableFrom(field.getType()) && value instanceof Map) {
                        Class<? extends CustomPipelineData> type = (Class<? extends CustomPipelineData>) field.getType();
                        CustomPipelineData customPipelineData = instantiateCustomPipelineData(type);
                        customPipelineData.getUnderlyingMap().putAll((Map<? extends String, ?>) value);
                        field.set(this, customPipelineData);
                    } else {
                        try {
                            field.set(this, VCoreUtil.getTypeUtil().castData(value, field.getType()));
                        } catch (ClassCastException e) {
                            field.set(this, value);
                        }
                    }
                } else
                    field.set(this, value);
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                //System.err.println("Field " + key + " not found. Cleanup Task for missing fields will be implemented in a future release");
            }
        });
        return dataBeforeDeserialization;
    }

    @NotNull
    private <T extends CustomPipelineData> T instantiateCustomPipelineData(Class<? extends T> type) {
        T instantiated;
        try {
            // Searching for empty Constructor
            instantiated = type.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            try {
                instantiated = type.getConstructor(Map.class).newInstance(new ConcurrentHashMap<>());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException instantiationException) {
                throw new IllegalStateException("Could not find an empty constructor or a Map Constructor");
            }
        }
        return instantiated;
    }
}
