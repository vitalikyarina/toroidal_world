package com.toroidalworld.compat.create;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CreateMultiblockFold {
    public static final String CONTROLLER_KEY = "Controller";
    public static final String LAST_KNOWN_POS_KEY = "LastKnownPos";

    public static CompoundTag controllerPairInFrameOf(BlockEntity blockEntity, CompoundTag tag) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide) {
            return tag;
        }

        BlockPos worldPosition = blockEntity.getBlockPos();
        BlockPos rawController = NbtUtils.readBlockPos(tag, CONTROLLER_KEY).orElse(null);
        BlockPos rawLastKnown = NbtUtils.readBlockPos(tag, LAST_KNOWN_POS_KEY).orElse(null);
        BlockPos controller = inFrameOf(level, worldPosition, rawController);
        BlockPos lastKnown = inFrameOf(level, worldPosition, rawLastKnown);
        if (Objects.equals(controller, rawController) && Objects.equals(lastKnown, rawLastKnown)) {
            return tag;
        }

        CompoundTag folded = shallowCopy(tag);
        if (controller != null) {
            folded.put(CONTROLLER_KEY, NbtUtils.writeBlockPos(controller));
        }

        if (lastKnown != null) {
            folded.put(LAST_KNOWN_POS_KEY, NbtUtils.writeBlockPos(lastKnown));
        }

        return folded;
    }

    private static @Nullable BlockPos inFrameOf(Level level, BlockPos worldPosition, @Nullable BlockPos stored) {
        return stored == null ? null : ControllerFrameFold.inFrameOf(level, worldPosition, stored);
    }

    private static CompoundTag shallowCopy(CompoundTag tag) {
        CompoundTag copy = new CompoundTag();
        for (String key : tag.getAllKeys()) {
            copy.put(key, tag.get(key));
        }

        return copy;
    }

    private CreateMultiblockFold() {
    }
}
