package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.toroidalworld.core.LogRateGate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public final class CreateConnectivityProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LogRateGate SAMPLE_GATE = new LogRateGate();

    public static void reportConnected(BlockGetter level, BlockPos pos, BlockPos other, boolean connected) {
        BlockPos oneController = controllerAt(level, pos);
        BlockPos twoController = controllerAt(level, other);
        boolean disagree = oneController != null && twoController != null && !oneController.equals(twoController);
        boolean halfMissing = (oneController == null) != (twoController == null);
        if (!disagree && !halfMissing && !SAMPLE_GATE.tryPass()) {
            return;
        }

        LOGGER.info("[create-compat] ct_connected side={} pos_x={} pos_y={} pos_z={} other_x={} other_y={}"
                + " other_z={} one_ctrl_x={} one_ctrl_y={} one_ctrl_z={} two_ctrl_x={} two_ctrl_y={} two_ctrl_z={}"
                + " disagree={} half_missing={} connected={}",
                side(level), pos.getX(), pos.getY(), pos.getZ(), other.getX(), other.getY(), other.getZ(),
                x(oneController), y(oneController), z(oneController),
                x(twoController), y(twoController), z(twoController),
                disagree, halfMissing, connected);
    }

    private static @Nullable BlockPos controllerAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IMultiBlockEntityContainer container
                ? container.getController()
                : null;
    }

    private static String side(BlockGetter level) {
        if (level instanceof Level worldLevel) {
            return worldLevel.isClientSide ? "client" : "server";
        }

        return "region";
    }

    private static Object x(@Nullable BlockPos pos) {
        return pos == null ? "none" : pos.getX();
    }

    private static Object y(@Nullable BlockPos pos) {
        return pos == null ? "none" : pos.getY();
    }

    private static Object z(@Nullable BlockPos pos) {
        return pos == null ? "none" : pos.getZ();
    }

    private CreateConnectivityProbe() {
    }
}
