/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.custom.nbtholders.location;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.verdox.vcore.plugin.wrapper.types.WorldChunk;
import de.verdox.vcore.util.bukkit.keys.ChunkKey;
import de.verdox.vcore.util.bukkit.keys.LocationKey;
import de.verdox.vcore.util.bukkit.keys.SplitChunkKey;
import de.verdox.vcorepaper.VCorePaper;
import de.verdox.vcorepaper.custom.nbtholders.NBTHolder;
import de.verdox.vcorepaper.custom.nbtholders.NBTHolderImpl;
import de.verdox.vcorepaper.custom.nbtholders.location.event.nbtlocation.NBTBlockDeleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 10.08.2021 13:33
 */
public class NBTLocation extends NBTHolderImpl<Location> {

    private final int chunkY;
    private final int chunkX;
    private final int chunkZ;
    private final WorldChunk worldChunk;
    private final SplitChunkKey splitChunkKey;

    private NBTFile nbtFile;
    private NBTCompound chunkCompound;
    private NBTCompound splitChunkCompound;
    private NBTCompound blockCompound;

    public NBTLocation(Location location) {
        super(location);
        chunkY = dataHolder.getBlockY() / 16;
        chunkX = dataHolder.getBlockX() >> 4;
        chunkZ = dataHolder.getBlockZ() >> 4;

        worldChunk = new WorldChunk(dataHolder.getWorld().getName(), chunkX, chunkZ);
        splitChunkKey = new SplitChunkKey(worldChunk, chunkY);
    }

    @Override
    protected NBTCompound getNbtCompound() {
        if (Objects.isNull(this.nbtFile)) {
            try {
                this.nbtFile = VCorePaper.getInstance().getBlockFileStorage().loadNBTFile(worldChunk).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                VCorePaper.getInstance().consoleMessage("&cCould not load NBT File for: " + dataHolder, true);
                e.printStackTrace();
                throw new IllegalStateException();
            }
        }
        if (Objects.isNull(this.chunkCompound))
            this.chunkCompound = this.nbtFile.getOrCreateCompound(((ChunkKey) splitChunkKey).toString());
        if (Objects.isNull(this.splitChunkCompound))
            this.splitChunkCompound = this.chunkCompound.getOrCreateCompound(splitChunkKey.toString());
        if (Objects.isNull(this.blockCompound))
            this.blockCompound = this.splitChunkCompound.getOrCreateCompound(new LocationKey(this.dataHolder).toStringWithoutWorld());
        return this.blockCompound;
    }

    @Override
    public void removeKey(@Nonnull String key) {
        blockCompound.removeKey(key);
        clearGarbage();
    }

    /**
     * Not usable right now -> Object references must be declared new if compounds are cleared
     */
    private void clearGarbage() {
        if (blockCompound.getKeys().isEmpty()) {
            splitChunkCompound.removeKey(blockCompound.getName());
            this.blockCompound = null;
        }
        if (splitChunkCompound.getKeys().isEmpty()) {
            chunkCompound.removeKey(splitChunkCompound.getName());
            this.splitChunkCompound = null;
        }
        if (chunkCompound.getKeys().isEmpty()) {
            nbtFile.removeKey(chunkCompound.getName());
            this.nbtFile = null;
        }
    }

    @Override
    public void save() {
        try {
            clearGarbage();
            nbtFile.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the NBT Storage of a Tile Entity if the BlockState of the block at the specified location is instance of TileState
     * Note that in order to make this function work it is probably needed for the chunk to be loaded where the block is located at
     * <p>
     * This function is not thread safe in every server software (e.g. Spigot)
     *
     * @return Vanilla Compound of the block if exists
     */
    @Nullable
    @Override
    public NBTHolder getVanillaCompound() {
        return this;
    }

    @Override
    public void delete() {
        NBTBlockDeleteEvent NBTBlockDeleteEvent = new NBTBlockDeleteEvent(this);
        Bukkit.getPluginManager().callEvent(NBTBlockDeleteEvent);
        if (!NBTBlockDeleteEvent.isCancelled())
            splitChunkCompound.removeKey(new LocationKey(this.dataHolder).toStringWithoutWorld());
    }
}
