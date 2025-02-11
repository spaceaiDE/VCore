package de.verdox.vcore.plugin;

import de.verdox.vcore.plugin.subsystem.VCoreSubsystem;
import de.verdox.vcore.synchronization.pipeline.annotations.RequiredSubsystemInfo;
import de.verdox.vcore.synchronization.pipeline.datatypes.PlayerData;
import de.verdox.vcore.synchronization.pipeline.datatypes.ServerData;
import de.verdox.vcore.synchronization.pipeline.datatypes.VCoreData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VCoreSubsystemManager<T extends VCorePlugin<?, R>, R extends VCoreSubsystem<T>> implements SystemLoadable {

    private final List<R> subSystems = new ArrayList<>();
    private final List<R> activatedSubSystems = new ArrayList<>();
    private T plugin;
    private Set<Class<? extends VCoreData>> registeredDataClasses = new HashSet<>();
    private Set<Class<? extends VCoreData>> activeDataClasses = new HashSet<>();

    private Set<Class<? extends PlayerData>> registeredPlayerDataClasses = new HashSet<>();
    private Set<Class<? extends PlayerData>> activePlayerDataClasses = new HashSet<>();

    private Set<Class<? extends ServerData>> registeredServerDataClasses = new HashSet<>();
    private Set<Class<? extends ServerData>> activeServerDataClasses = new HashSet<>();

    private boolean loaded = false;

    VCoreSubsystemManager(T plugin) {
        this.plugin = plugin;
    }

    void enable() {
        plugin.consoleMessage("&eStarting Subsystem Manager&7...", false);
        List<R> providedSubsystems = plugin.provideSubsystems();
        if (providedSubsystems != null) {
            subSystems.addAll(providedSubsystems);
            subSystems.stream()
                    .filter(VCoreSubsystem::isActivated)
                    .forEach(r -> {
                        plugin.consoleMessage("&eActivating Subsystem&7: &b" + r.getClass().getSimpleName(), false);
                        r.onSubsystemEnable();
                        activatedSubSystems.add(r);
                        findRegisteredDataClasses(r);
                        plugin.consoleMessage("&eActivated Subsystem&7: &b" + r.getClass().getSimpleName(), false);
                        plugin.consoleMessage("", false);
                    });
        }
        findActiveDataClasses();
        loaded = true;
    }

    public List<R> getActivatedSubSystems() {
        return activatedSubSystems;
    }

    public List<R> getSubSystems() {
        return subSystems;
    }

    public <S extends VCoreSubsystem<?>> S findSubsystemByClass(Class<? extends S> subsystemClass) {
        return subsystemClass.cast(this.subSystems.stream().filter(r -> r.getClass().equals(subsystemClass)).findAny().orElse(null));
    }

    void disable() {
        plugin.consoleMessage("&eStopping Subsystem Manager&7...", false);
        activatedSubSystems.forEach(r -> {
            r.onSubsystemDisable();
            plugin.consoleMessage("&eDeactivated Subsystem&7: &b" + r, false);
        });
    }

    void findRegisteredDataClasses(VCoreSubsystem<?> subsystem) {
        plugin.consoleMessage("&eSearching for DataClass Implementations&7... &8[&b" + subsystem.getClass().getSimpleName() + "&8]", 1, true);

        //Reflections reflections = new Reflections(plugin.getClass().getPackage().getName(), new SubTypesScanner(false));
        if (subsystem.playerDataClasses() != null) {
            this.registeredPlayerDataClasses.addAll(subsystem.playerDataClasses());
            this.registeredDataClasses.addAll(subsystem.playerDataClasses());
        }
        if (subsystem.serverDataClasses() != null) {
            this.registeredServerDataClasses.addAll(subsystem.serverDataClasses());
            this.registeredDataClasses.addAll(subsystem.serverDataClasses());
        }
        //registeredDataClasses = reflections.getSubTypesOf(VCoreData.class);
        //registeredServerDataClasses = reflections.getSubTypesOf(ServerData.class);
        //registeredPlayerDataClasses = reflections.getSubTypesOf(PlayerData.class);
        plugin.consoleMessage("&aDone searching&7!", 1, true);
        plugin.consoleMessage("&aFound PlayerDataClasses&7: &b" + registeredPlayerDataClasses.size(), 2, true);
        plugin.consoleMessage("&aFound ServerDataClasses&7: &b" + registeredServerDataClasses.size(), 2, true);
    }

    void findActiveDataClasses() {
        activeDataClasses = registeredDataClasses.stream()
                .filter(aClass -> {
                    RequiredSubsystemInfo requiredSubsystemInfo = aClass.getAnnotation(RequiredSubsystemInfo.class);
                    if (requiredSubsystemInfo == null)
                        plugin.consoleMessage("&eWarning &a" + aClass + " &edoes not have RequiredSubsystemInfo Annotation set&7!", 1, false);
                    return requiredSubsystemInfo != null && isActivated(requiredSubsystemInfo);
                })
                .collect(Collectors.toSet());

        activePlayerDataClasses = registeredPlayerDataClasses
                .stream()
                .filter(aClass -> {
                    RequiredSubsystemInfo requiredSubsystemInfo = aClass.getAnnotation(RequiredSubsystemInfo.class);
                    return requiredSubsystemInfo != null && isActivated(requiredSubsystemInfo);
                })
                .collect(Collectors.toSet());

        activeServerDataClasses = registeredServerDataClasses
                .stream()
                .filter(aClass -> {
                    RequiredSubsystemInfo requiredSubsystemInfo = aClass.getAnnotation(RequiredSubsystemInfo.class);
                    return requiredSubsystemInfo != null && isActivated(requiredSubsystemInfo);
                })
                .collect(Collectors.toSet());
    }

    public boolean isActivated(RequiredSubsystemInfo requiredSubsystemInfo) {
        return activatedSubSystems.stream().anyMatch(bukkit -> bukkit.getClass().equals(requiredSubsystemInfo.parentSubSystem()));
    }

    public Set<Class<? extends PlayerData>> getRegisteredPlayerDataClasses() {
        return this.registeredPlayerDataClasses;
    }

    public Set<Class<? extends PlayerData>> getActivePlayerDataClasses() {
        return this.activePlayerDataClasses;
    }

    public Set<Class<? extends ServerData>> getRegisteredServerDataClasses() {
        return this.registeredServerDataClasses;
    }

    public Set<Class<? extends ServerData>> getActiveServerDataClasses() {
        return this.activeServerDataClasses;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void shutdown() {

    }
}
