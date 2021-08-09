/*
 * Copyright (c) 2021. Lukas Jonsson
 */

package de.verdox.vcorepaper.custom.blocks.debug;

import de.verdox.vcorepaper.custom.annotation.NBTIdentifier;
import de.verdox.vcorepaper.custom.blocks.data.VBlockCustomData;

import javax.annotation.Nonnull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 07.08.2021 02:12
 */
@NBTIdentifier(nbtKey = "debugKey")
public class BlockDebugData extends VBlockCustomData<Long> {
    @Nonnull
    @Override
    public Class<Long> getTypeClass() {
        return Long.class;
    }

    @Override
    public Long defaultValue() {
        return System.currentTimeMillis();
    }
}
