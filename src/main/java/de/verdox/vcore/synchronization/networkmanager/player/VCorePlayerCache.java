/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.networkmanager.player;

import com.google.common.eventbus.Subscribe;
import de.verdox.vcore.plugin.VCorePlugin;
import de.verdox.vcore.synchronization.messaging.event.MessageEvent;
import de.verdox.vcore.synchronization.messaging.messages.Message;
import de.verdox.vcore.synchronization.messaging.messages.MessageWrapper;
import de.verdox.vcore.synchronization.networkmanager.NetworkManager;
import de.verdox.vcore.synchronization.networkmanager.player.listener.VCorePlayerCacheListener;
import de.verdox.vcore.synchronization.networkmanager.server.ServerInstance;
import de.verdox.vcore.synchronization.networkmanager.server.ServerType;
import de.verdox.vcore.synchronization.pipeline.parts.Pipeline;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 15.06.2021 01:48
 */
public class VCorePlayerCache {

    //TODO: VCOrePlaer auch in DB speichern, Bei Delete evtl mitgeben wo deleted werden soll? (Nur aus Local und Global Cache aber Persistent Speichern -> Alternative zu Bukkit.getOfflinePlayer())
    //TODO: Eigene bukkit.getOfflinePlayer Implementierung mit Mojang API implementieren?

    private final VCorePlugin<?, ?> vCorePlugin;
    private final NetworkManager<?> networkManager;

    public VCorePlayerCache(NetworkManager<?> networkManager) {
        this.networkManager = networkManager;
        this.vCorePlugin = networkManager.getPlugin();
        vCorePlugin.getServices().eventBus.register(new PlayerRedisListener());
    }

    class PlayerRedisListener {
        @Subscribe
        public void playerMessages(MessageEvent e) {
            MessageWrapper messageWrapper = new MessageWrapper(e.getMessage());
            if (!messageWrapper.validate(String.class, String.class, UUID.class, String.class))
                return;

            Message message = e.getMessage();

            String serverType = message.getData(0, String.class);
            String serverName = message.getData(1, String.class);
            UUID playerUUID = message.getData(2, UUID.class);
            String displayName = message.getData(3, String.class);

            UUID serverUUID = vCorePlugin.getCoreInstance().getNetworkManager().getServerCache().getServerUUID(serverName);

            if (messageWrapper.parameterContains("connection", VCorePlayerCacheListener.PlayerPingType.JOIN.name())) {
                if (serverType.equals(ServerType.PROXY.name())) {
                    vCorePlugin.consoleMessage("&eReceived Player Proxy Login&7: &b" + playerUUID + " &8[&a" + displayName + "&8]", false);
                }

                VCorePlayer vCorePlayer = vCorePlugin.getServices().getPipeline().load(VCorePlayer.class, playerUUID, Pipeline.LoadingStrategy.LOAD_PIPELINE, true);
                vCorePlayer.displayName = displayName;

                ServerInstance serverInstance = networkManager.getPlugin().getServices().getPipeline().load(ServerInstance.class, serverUUID, Pipeline.LoadingStrategy.LOAD_PIPELINE, false);
                if (serverInstance == null) {
                    networkManager.getPlugin().consoleMessage("&cPlayer &a" + displayName + " &cjoined a server that is not in Cache&7: &b" + serverName, false);
                    return;
                }
                if (serverInstance.getServerType().equals(ServerType.GAME_SERVER)) {
                    vCorePlayer.currentGameServer = serverInstance.getServerName();
                } else if (serverInstance.getServerType().equals(ServerType.PROXY)) {
                    vCorePlayer.currentProxyServer = serverInstance.getServerName();
                }
                if (serverInstance.getServerName().equals(networkManager.getPlugin().getCoreInstance().getServerName()))
                    vCorePlayer.save(false);
            } else {
                VCorePlayer vCorePlayer = vCorePlugin.getServices().getPipeline().load(VCorePlayer.class, playerUUID, Pipeline.LoadingStrategy.LOAD_PIPELINE, false);
                if (vCorePlayer == null)
                    return;
                if (serverType.equals(ServerType.PROXY.name())) {
                    if (messageWrapper.parameterContains("connection", VCorePlayerCacheListener.PlayerPingType.QUIT.name())) {
                        vCorePlugin.consoleMessage("&eReceived Player Proxy Logout&7: &b" + playerUUID + " &8[&a" + displayName + "&8]", false);
                        vCorePlayer.currentProxyServer = "";
                        vCorePlugin.getServices().getPipeline().delete(VCorePlayer.class, playerUUID);
                    }
                }

                ServerInstance serverInstance = networkManager.getPlugin().getServices().getPipeline().load(ServerInstance.class, serverUUID, Pipeline.LoadingStrategy.LOAD_PIPELINE, false);
                if (serverInstance == null) {
                    networkManager.getPlugin().consoleMessage("&ePlayer &a" + displayName + " &equit from server that is not in Cache&7: &b" + serverName, false);
                    return;
                }
            }
        }
    }
}
