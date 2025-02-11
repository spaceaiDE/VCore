/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcore.plugin.wrapper;

import de.verdox.vcore.plugin.wrapper.bungeecord.BungeePlatform;
import de.verdox.vcore.plugin.wrapper.spigot.SpigotPlatform;
import de.verdox.vcore.synchronization.networkmanager.player.VCorePlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 01.08.2021 20:01
 */
public class BungeePlatformWrapperImpl implements PlatformWrapper {
    private ProxiedPlayer getPlayer(@NotNull VCorePlayer vCorePlayer) {
        return ProxyServer.getInstance().getPlayer(vCorePlayer.getObjectUUID());
    }

    @Override
    public boolean isPlayerOnline(@Nonnull @NotNull UUID playerUUID) {
        return ProxyServer.getInstance().getPlayer(playerUUID) != null;
    }

    @Override
    public boolean isPrimaryThread() {
        return false;
    }

    @Override
    public void shutdown() {
        ProxyServer.getInstance().stop();
    }

    @Override
    public InetSocketAddress getPlayerAddress(@Nonnull @NotNull UUID playerUUID) {
        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerUUID);
        if (proxiedPlayer == null)
            return null;
        return proxiedPlayer.getAddress();
    }

    @Override
    public SpigotPlatform getSpigotPlatform() {
        return null;
    }

    @Override
    public BungeePlatform getBungeePlatform() {
        return new BungeePlatform() {
            @Override
            public boolean sendToServer(@Nonnull @NotNull UUID playerUUID, @Nonnull @NotNull String serverName) {
                ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerUUID);
                if (proxiedPlayer == null)
                    return false;
                if (serverName.equals(proxiedPlayer.getServer().getInfo().getName()))
                    return false;
                ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(serverName);
                if (serverInfo == null)
                    return false;
                proxiedPlayer.connect(serverInfo);
                return true;
            }

            @Override
            public boolean kickPlayer(@Nonnull @NotNull UUID playerUUID, @Nonnull @NotNull String kickMessage) {
                ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerUUID);
                if (proxiedPlayer == null)
                    return false;
                proxiedPlayer.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', kickMessage)));
                return true;
            }
        };
    }
}
