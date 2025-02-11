/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.synchronization.networkmanager.player.listener;

import de.verdox.vcore.plugin.VCorePlugin;
import de.verdox.vcore.plugin.listener.VCoreListener;
import de.verdox.vcore.synchronization.networkmanager.NetworkManager;
import de.verdox.vcore.synchronization.networkmanager.player.VCorePlayer;
import de.verdox.vcore.synchronization.pipeline.parts.Pipeline;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 01.08.2021 02:53
 */
public class PlayerProxyListener extends VCoreListener.VCoreBungeeListener implements VCorePlayerCacheListener {
    private final NetworkManager<?> networkManager;

    public PlayerProxyListener(NetworkManager<?> networkManager) {
        super((VCorePlugin.BungeeCord) networkManager.getPlugin());
        this.networkManager = networkManager;
    }

    @net.md_5.bungee.event.EventHandler
    public void loginEvent(LoginEvent e) {
        UUID uuid = e.getConnection().getUniqueId();
        if (uuid == null)
            return;
        if (!plugin.getServices().getPipeline().exist(VCorePlayer.class, uuid, Pipeline.QueryStrategy.LOCAL, Pipeline.QueryStrategy.GLOBAL_CACHE))
            return;
        plugin.consoleMessage("&ePlayer tried to connect twice&7: &b" + e.getConnection().getName() + " &8[&a" + e.getConnection().getUniqueId() + "&8]", false);
        VCorePlayer vCorePlayer = networkManager.getPlugin().getCoreInstance().getPlayerAPI().getVCorePlayer(uuid);
        e.setCancelled(true);
        e.setCancelReason(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cDu bist bereits auf dem Netzwerk. Bitte neu verbinden&7!")));
        networkManager.getPlugin().getCoreInstance().getPlayerAPI().kickPlayer(vCorePlayer, "&cLogged in from another location");
        plugin.getServices().getPipeline().delete(VCorePlayer.class, uuid, true, Pipeline.QueryStrategy.ALL);
    }

    @net.md_5.bungee.event.EventHandler
    public void onJoin(PostLoginEvent e) {
        plugin.async(() -> {
            sendPlayerPing(getPlugin(),
                    networkManager.getServerPingManager().getServerName(),
                    networkManager.getServerType(), PlayerPingType.JOIN,
                    e.getPlayer().getUniqueId(),
                    e.getPlayer().getName());
        });
        //plugin.getCoreInstance().getPlayerAPI().broadcastMessage("&8[&a+&8] &e" + e.getPlayer().getName(), PlayerMessageType.CHAT, GlobalProperty.NETWORK);
    }

    @net.md_5.bungee.event.EventHandler
    public void onQuit(PlayerDisconnectEvent e) {
        plugin.async(() -> {
            sendPlayerPing(getPlugin(),
                    networkManager.getServerPingManager().getServerName(),
                    networkManager.getServerType(), PlayerPingType.QUIT,
                    e.getPlayer().getUniqueId(),
                    e.getPlayer().getName());
        });
        //plugin.getCoreInstance().getPlayerAPI().broadcastMessage("&8[&c-&8] &e" + e.getPlayer().getName(), PlayerMessageType.CHAT, GlobalProperty.NETWORK);
    }
}