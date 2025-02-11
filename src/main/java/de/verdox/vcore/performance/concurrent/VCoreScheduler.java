/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.performance.concurrent;

import de.verdox.vcore.plugin.SystemLoadable;
import de.verdox.vcore.plugin.VCorePlugin;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VCoreScheduler implements SystemLoadable {
    private final VCorePlugin<?, ?> vCorePlugin;
    private final ScheduledExecutorService scheduledExecutorService;

    public VCoreScheduler(VCorePlugin<?, ?> vCorePlugin) {
        this.vCorePlugin = vCorePlugin;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(4, new DefaultThreadFactory(vCorePlugin.getPluginName() + "Scheduler"));
    }

    public ScheduledFuture<?> asyncInterval(Runnable task, long delay, long interval) {
        return scheduledExecutorService.scheduleAtFixedRate(new CatchingRunnable(task), delay * 50, interval * 50, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> asyncSchedule(Runnable task, long delay) {
        return scheduledExecutorService.schedule(new CatchingRunnable(task), delay * 50, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> asyncSchedule(Runnable task, @NonNegative long delay, TimeUnit timeUnit) {
        return scheduledExecutorService.schedule(new CatchingRunnable(task), delay, timeUnit);
    }

    public void async(Runnable task) {
        scheduledExecutorService.execute(new CatchingRunnable(task));
    }

    public void waitUntilShutdown() {
        shutdown();
        vCorePlugin.consoleMessage("&6Waiting 20s for Scheduler to shut down&7!", true);
        try {
            scheduledExecutorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            vCorePlugin.consoleMessage("&cScheduler was interrupted&7!", true);
            e.printStackTrace();
        }
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void shutdown() {
        vCorePlugin.consoleMessage("&6Shutting down Scheduler&7!", true);
        scheduledExecutorService.shutdown();
    }
}
