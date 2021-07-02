/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.bukkitplayerhandler.playerdata;

import de.verdox.vcore.synchronization.pipeline.annotations.*;
import de.verdox.vcore.synchronization.pipeline.datatypes.PlayerData;
import de.verdox.vcore.synchronization.pipeline.player.VCorePlayer;
import de.verdox.vcore.plugin.VCorePlugin;
import de.verdox.vcorepaper.VCorePaper;
import de.verdox.vcorepaper.bukkitplayerhandler.BukkitPlayerHandler;
import de.verdox.vcorepaper.bukkitplayerhandler.model.SerializableJsonInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 20.06.2021 00:27
 */
@DataStorageIdentifier(identifier = "BukkitPlayerHandlerPlayerData")
@RequiredSubsystemInfo(parentSubSystem = BukkitPlayerHandler.class)
@VCoreDataContext(preloadStrategy = PreloadStrategy.LOAD_ON_NEED, dataContext = DataContext.GLOBAL, cleanOnNoUse = false, time = 30, timeUnit = TimeUnit.MINUTES)
public class PlayerHandlerData extends PlayerData {

    @VCorePersistentData
    private Map<String, Map<String, Object>> inventoryCache = new ConcurrentHashMap<>();

    @VCorePersistentData
    public boolean restoreVanillaInventory = true;

    @VCorePersistentData
    private String activeInventoryID = null;

    public PlayerHandlerData(VCorePlugin<?,?> plugin, UUID playerUUID) {
        super(plugin, playerUUID);
    }

    @Override
    public void onCreate() {

    }

    public void saveInventory(){
        Player player = Bukkit.getPlayer(getObjectUUID());
        if(player == null)
            return;
        saveInventory(() -> player);
    }

    public void saveInventory(Supplier<Player> supplier){
        if(activeInventoryID == null)
            saveInventory(supplier,"vanilla");
        else
            saveInventory(supplier,activeInventoryID);
    }

    public void saveInventory(Supplier<Player> supplier, String inventoryID){
        Player player = supplier.get();
        if(player == null)
            return;
        ItemStack[] storageContents = player.getInventory().getStorageContents().clone();
        ItemStack[] armorContents = player.getInventory().getArmorContents().clone();
        ItemStack[] enderChest = player.getEnderChest().getStorageContents();

        ItemStack offHand = player.getInventory().getItemInOffHand();
        SerializableJsonInventory serializableInventory = new SerializableJsonInventory(inventoryID,storageContents,armorContents, enderChest, offHand, player.getHealth(), player.getFoodLevel(), player.getExp(), new HashSet<>(player.getActivePotionEffects()));

        inventoryCache.put(inventoryID,serializableInventory.getData());
        VCorePaper.getInstance().consoleMessage("&eInventory &6"+inventoryID+" &eof player &b"+getObjectUUID()+" &esaved&7!", true);
    }

    public void restoreInventory(@Nonnull Supplier<Player> supplier){
        if(this.activeInventoryID == null)
            restoreInventory("vanilla", supplier);
        else
            restoreInventory(activeInventoryID, supplier);
    }

    public boolean hasInventory(@Nonnull String inventoryID){
        return inventoryCache.containsKey(inventoryID);
    }

    public void restoreInventory(@Nonnull String inventoryID, @Nonnull Supplier<Player> supplier){
        Player player = supplier.get();
        if(player == null)
            return;
        if(!inventoryCache.containsKey(inventoryID))
            return;
        this.activeInventoryID = inventoryID;
        SerializableJsonInventory serializableInventory = new SerializableJsonInventory(inventoryCache.get(inventoryID));
        serializableInventory.restoreInventory(player, null);
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onCleanUp() {

    }
}
