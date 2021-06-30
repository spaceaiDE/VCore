/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.bukkitplayerhandler.model;

import de.verdox.vcore.synchronization.pipeline.datatypes.serializables.VCoreSerializableJson;
import de.verdox.vcore.synchronization.pipeline.datatypes.serializables.reference.collections.SetBsonReference;
import de.verdox.vcore.synchronization.pipeline.datatypes.serializables.reference.objects.StringBsonReference;
import de.verdox.vcore.synchronization.pipeline.datatypes.serializables.reference.primitives.numbers.DoubleBsonReference;
import de.verdox.vcore.synchronization.pipeline.datatypes.serializables.reference.primitives.numbers.IntegerBsonReference;
import de.verdox.vcore.util.VCoreUtil;
import de.verdox.vcorepaper.custom.util.Serializer;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 20.06.2021 00:19
 */
public class SerializableJsonInventory extends VCoreSerializableJson {
    public SerializableJsonInventory(String id, ItemStack[] storageContents, ItemStack[] armorContents, ItemStack[] enderChest, ItemStack[]extraContents, double health, int foodLevel, float exp, Set<PotionEffect> potionEffects){
        new StringBsonReference(this, "id").setValue(id);
        new StringBsonReference(this, "armorContents").setValue(Serializer.itemStackArrayToBase64(armorContents));
        new StringBsonReference(this, "storageContents").setValue(Serializer.itemStackArrayToBase64(storageContents));
        new StringBsonReference(this, "enderChest").setValue(Serializer.itemStackArrayToBase64(enderChest));
        new StringBsonReference(this, "extraContents").setValue(Serializer.itemStackArrayToBase64(extraContents));
        new DoubleBsonReference(this, "health").setValue(health);
        new DoubleBsonReference(this, "exp").setValue((double) exp);
        new IntegerBsonReference(this, "food").setValue(foodLevel);
        Set<String> serializedPotionEffects = potionEffects.stream().map(potionEffect -> VCoreUtil.getBukkitPlayerUtil().serializePotionEffect(potionEffect)).collect(Collectors.toSet());
        new SetBsonReference<String>(this,"potionEffects").setValue(serializedPotionEffects);
    }

    public boolean restoreInventory(@Nonnull Player player, @Nullable Runnable callback){
        try {
            VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eLade Rüstung&7...");

            ItemStack [] armorContents = deSerializeArmorContents();
            if(armorContents == null){
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.CHAT,"&cKonnte Rüstung nicht wiederherstellen");
            }
            else{
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eRüstung wurde geladen");
                if(armorContents.length != 0)
                    player.getInventory().setArmorContents(armorContents);
            }

            VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eLade Items&7...");
            ItemStack [] storageContents = deSerializeStorageContents();

            if(storageContents == null){
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.CHAT,"&cKonnte Items nicht wiederherstellen");
            }
            else{
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eItems wurden geladen");
                if(storageContents.length != 0)
                    player.getInventory().setStorageContents(storageContents);
            }

            VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eLade EnderChest&7...");
            ItemStack[] enderChest = deSerializeEnderChest();
            if(enderChest == null){
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.CHAT,"&cKonnte EnderChest nicht wiederherstellen");
            }
            else{
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eEnderChest wurde geladen");
                if(enderChest.length != 0)
                    player.getEnderChest().setContents(enderChest);
            }

            VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eLade ExtraContents&7...");
            ItemStack[] extraContents = deSerializeExtraContents();
            if(extraContents == null){
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.CHAT,"&cKonnte ExtraContents nicht wiederherstellen");
            }
            else{
                VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&eExtraContents wurden geladen");
                if(extraContents.length != 0)
                    player.getInventory().setExtraContents(extraContents);
            }

            player.setHealth(getHealth());
            player.setExp((float) getExp());
            player.setFoodLevel(getFoodLevel());
            Set<String> serializedEffects = new SetBsonReference<String>(this,"potionEffects").getValue();
            if(!serializedEffects.isEmpty()){
                new SetBsonReference<String>(this,"potionEffects").getValue().stream()
                        .map(serializedEffect -> VCoreUtil.getBukkitPlayerUtil().deSerializePotionEffect(serializedEffect))
                        .forEach(potionEffect -> {
                            player.removePotionEffect(potionEffect.getType());
                            player.addPotionEffect(potionEffect);
                        });
            }
            VCoreUtil.getBukkitPlayerUtil().sendPlayerMessage(player, ChatMessageType.ACTION_BAR,"&aSpielerdaten wurden geladen");
            if(callback != null)
                callback.run();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ItemStack[] deSerializeStorageContents() throws IOException {
        return Serializer.itemStackArrayFromBase64(new StringBsonReference(this, "storageContents").orElse(null));
    }

    public ItemStack[] deSerializeArmorContents() throws IOException {
        return Serializer.itemStackArrayFromBase64(new StringBsonReference(this, "armorContents").orElse(null));
    }

    public ItemStack[] deSerializeEnderChest() throws IOException {
        return Serializer.itemStackArrayFromBase64(new StringBsonReference(this, "enderChest").orElse(null));
    }

    public ItemStack[] deSerializeExtraContents() throws IOException {
        return Serializer.itemStackArrayFromBase64(new StringBsonReference(this, "extraContents").orElse(null));
    }

    public double getHealth(){
        return new DoubleBsonReference(this, "health").orElse(20d);
    }

    public double getExp(){
        return new DoubleBsonReference(this, "exp").orElse(0d);
    }

    public int getFoodLevel(){
        return new IntegerBsonReference(this, "food").orElse(20);
    }

    public String getID(){
        return new StringBsonReference(this, "storageContents").getValue();
    }
}
