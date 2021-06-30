/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.pipeline;

import de.verdox.vcore.performance.concurrent.CatchingRunnable;
import de.verdox.vcore.plugin.SystemLoadable;
import de.verdox.vcore.synchronization.pipeline.datatypes.VCoreData;
import de.verdox.vcore.synchronization.pipeline.parts.DataSynchronizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 24.06.2021 17:14
 */
public class PipelineDataSynchronizer implements DataSynchronizer {
    private final PipelineManager pipelineManager;

    PipelineDataSynchronizer(PipelineManager pipelineManager){
        this.pipelineManager = pipelineManager;
    }

    @Override
    public void synchronize(@Nonnull DataSourceType source, @Nonnull DataSourceType destination, @Nonnull Class<? extends VCoreData> dataClass, @Nonnull UUID objectUUID) {
        synchronize(source, destination, dataClass, objectUUID, null);
    }

    @Override
    public synchronized void synchronize(@Nonnull DataSourceType source, @Nonnull DataSourceType destination, @Nonnull Class<? extends VCoreData> dataClass, @Nonnull UUID objectUUID, @Nullable Runnable callback) {
        pipelineManager.getExecutorService().submit(new CatchingRunnable(() -> doSynchronisation(source, destination, dataClass, objectUUID,callback)));
    }

    public void doSynchronisation(@Nonnull DataSourceType source, @Nonnull DataSourceType destination, @Nonnull Class<? extends VCoreData> dataClass, @Nonnull UUID objectUUID, @Nullable Runnable callback){
        if(source.equals(destination))
            return;
        if(pipelineManager.globalCache == null && (source.equals(DataSourceType.GLOBAL_CACHE) || destination.equals(DataSourceType.GLOBAL_CACHE)))
            return;
        if(pipelineManager.globalStorage == null && (source.equals(DataSourceType.GLOBAL_STORAGE) || destination.equals(DataSourceType.GLOBAL_STORAGE)))
            return;

        if(source.equals(DataSourceType.LOCAL)){

            if(!pipelineManager.localCache.dataExist(dataClass, objectUUID))
                return;
            VCoreData data = pipelineManager.localCache.getData(dataClass, objectUUID);
            Map<String, Object> dataToSave = data.serialize();
            // Local to Global Cache
            if(destination.equals(DataSourceType.GLOBAL_CACHE))
                pipelineManager.globalCache.save(dataClass,objectUUID,dataToSave);
                // Local to Global Storage
            else if(destination.equals(DataSourceType.GLOBAL_STORAGE))
                pipelineManager.globalStorage.save(dataClass, objectUUID, dataToSave);
        }
        else if(source.equals(DataSourceType.GLOBAL_CACHE)){
            if(!pipelineManager.globalCache.dataExist(dataClass, objectUUID))
                return;
            Map<String, Object> globalCachedData = pipelineManager.globalCache.loadData(dataClass, objectUUID);

            if(destination.equals(DataSourceType.LOCAL)) {
                if (!pipelineManager.localCache.dataExist(dataClass, objectUUID))
                    pipelineManager.localCache.save(dataClass, pipelineManager.localCache.instantiateData(dataClass, objectUUID));
                VCoreData data = pipelineManager.localCache.getData(dataClass, objectUUID);
                data.deserialize(globalCachedData);
                data.onLoad();
            }
            else if(destination.equals(DataSourceType.GLOBAL_STORAGE))
                pipelineManager.globalStorage.save(dataClass, objectUUID, globalCachedData);
        }
        else if(source.equals(DataSourceType.GLOBAL_STORAGE)){
            if(!pipelineManager.globalStorage.dataExist(dataClass, objectUUID))
                return;
            Map<String, Object> globalSavedData = pipelineManager.globalStorage.loadData(dataClass, objectUUID);

            if(destination.equals(DataSourceType.LOCAL)) {
                if (!pipelineManager.localCache.dataExist(dataClass, objectUUID))
                    pipelineManager.localCache.save(dataClass, pipelineManager.localCache.instantiateData(dataClass, objectUUID));
                VCoreData data = pipelineManager.localCache.getData(dataClass, objectUUID);
                data.deserialize(globalSavedData);
                data.onLoad();
            }
            else if(destination.equals(DataSourceType.GLOBAL_CACHE))
                pipelineManager.globalCache.save(dataClass,objectUUID,globalSavedData);
        }
        if(callback != null)
            callback.run();
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void shutdown() {
        try {
            pipelineManager.getExecutorService().shutdown();
            pipelineManager.getExecutorService().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
