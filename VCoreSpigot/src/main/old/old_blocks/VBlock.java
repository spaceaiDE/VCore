/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.custom.old_blocks;

import de.verdox.vcorepaper.VCorePaper;
import de.verdox.vcorepaper.custom.CustomData;
import de.verdox.vcorepaper.custom.CustomDataHolder;
import de.verdox.vcorepaper.custom.old_blocks.enums.VBlockEventPermission;
import de.verdox.vcorepaper.custom.old_blocks.files.VBlockSaveFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class VBlock extends CustomDataHolder<Location, NBTBlockHolder, CustomBlockManager> {

    private final BlockPersistentData blockPersistentData;
    private String blockDataAsString;

    public VBlock(Location blockLocation, CustomBlockManager customBlockManager, BlockPersistentData blockPersistentData) {
        super(blockLocation, customBlockManager);
        this.blockPersistentData = blockPersistentData;
        if (getBlockPersistentData().getJsonObject().containsKey("vBlockBlockData"))
            blockDataAsString = (String) getBlockPersistentData().getJsonObject().get("vBlockBlockData");
    }

    /**
     * Updates the internal cached blockData.
     *
     * @param blockData
     */
    public void updateBlockData(BlockData blockData) {
        this.blockDataAsString = blockData.getAsString();
        getBlockPersistentData().getJsonObject().put("vBlockBlockData", blockDataAsString);
    }

    public void dropItemInWorld(ItemStack itemStack, org.bukkit.util.Consumer<Item> beforeDrop) {
        Location blockLocation = getBlockPersistentData().getLocation();
        Bukkit.getScheduler().runTask(VCorePaper.getInstance(), () -> {
            blockLocation.getWorld().dropItemNaturally(blockLocation.clone().add(0, 1, 0), itemStack, beforeDrop);
        });
    }

    public String getBlockDataString() {
        return blockDataAsString;
    }

    public void addVBlockTickCallback(Consumer<VBlock> callback) {
        blockPersistentData.addTickCallback(callback);
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

    public boolean isBlockPermissionAllowed(VBlockEventPermission vBlockEventPermission) {
        return vBlockEventPermission.isAllowed(this);
    }

    public void allowBlockPermission(VBlockEventPermission vBlockEventPermission, boolean allowed) {
        vBlockEventPermission.setAllowed(this, allowed);
    }

    /**
     * Only deletes the internal data of the VBlock
     */
    public void deleteData() {
        synchronized (getBlockPersistentData().getJsonObject()) {
            getBlockPersistentData().clearTickCallbacks();
            getBlockPersistentData().getJsonObject().clear();
            getBlockPersistentData().getVBlockSaveFile().save();
        }
    }

    public int getChunkX() {
        return getDataHolder().clone().getBlockX() & 0xF; // keep the 4 least significant bits, range 0-15
    }

    public int getChunkY() {
        return getDataHolder().clone().getBlockX() & 0xFF; // and 8 least significant, range 0-255
    }

    public int getChunkZ() {
        return getDataHolder().clone().getBlockZ() & 0xF; // keep the 4 least significant bits, range 0-15
    }

    /**
     * Deletes the VBlock and all save Files
     */
    public void delete() {
        VBlockSaveFile vBlockSaveFile = getBlockPersistentData().getVBlockSaveFile();
        getCustomDataManager().unloadSaveFile(vBlockSaveFile);
        vBlockSaveFile.delete();
    }

    public BlockPersistentData getBlockPersistentData() {
        return blockPersistentData;
    }

    @NotNull
    @Override
    public NBTBlockHolder getNBTCompound() {
        return new NBTBlockHolder(getDataHolder(), blockPersistentData);
    }
}
