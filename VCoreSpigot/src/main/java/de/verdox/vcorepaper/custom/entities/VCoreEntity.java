/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.custom.entities;

import de.verdox.vcorepaper.custom.CustomData;
import de.verdox.vcorepaper.custom.CustomDataHolder;
import de.verdox.vcorepaper.custom.nbtholders.entity.NBTEntityHolder;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class VCoreEntity extends CustomDataHolder<Entity, NBTEntityHolder, CustomEntityManager> {

    public VCoreEntity(@NotNull Entity entity, @NotNull CustomEntityManager customEntityManager) {
        super(entity, customEntityManager);
    }

    @Override
    protected <T, R extends CustomData<T>> void onStoreData(Class<? extends R> customDataType, T value) {

    }

    @Override
    protected <T, R extends CustomData<T>> R instantiateData(Class<? extends R> customDataType) {
        try {
            return customDataType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NotNull
    @Override
    public NBTEntityHolder toNBTHolder() {
        return new NBTEntityHolder(getDataHolder());
    }
}
