/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.pipeline.dataconnection.cache;

import de.verdox.vcore.data.annotations.DataContext;
import de.verdox.vcore.data.annotations.PreloadStrategy;
import de.verdox.vcore.data.annotations.VCoreDataContext;
import de.verdox.vcore.data.datatypes.VCoreData;
import de.verdox.vcore.pipeline.dataconnection.DataProvider;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 24.06.2021 15:25
 */
public interface GlobalCache extends DataProvider {

    Map<String, Object> getObjectCache(Class<? extends VCoreData> dataClass, UUID objectUUID);
    Set<Map<String, Object>> getCacheList(Class<? extends VCoreData> dataClass);
    Set<String> getKeys(Class<? extends VCoreData> dataClass);

    boolean dataExist(@Nonnull Class<? extends VCoreData> dataClass, @Nonnull UUID objectUUID);

    static DataContext getContext(Class<? extends VCoreData> dataClass){
        VCoreDataContext vCoreDataContext = dataClass.getAnnotation(VCoreDataContext.class);
        if(vCoreDataContext == null)
            return DataContext.GLOBAL;
        return vCoreDataContext.dataContext();
    }

    static PreloadStrategy getPreloadStrategy(Class<? extends VCoreData> dataClass){
        VCoreDataContext vCoreDataContext = dataClass.getAnnotation(VCoreDataContext.class);
        if(vCoreDataContext == null)
            return PreloadStrategy.LOAD_ON_NEED;
        return vCoreDataContext.preloadStrategy();
    }
}
