package com.toroidalworld.compat.create;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.toroidalworld.compat.create.mixin.ConnectedInputAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CrafterGroupFold {
    public static BlockPos foldStoredDelta(Level level, BlockPos owner, BlockPos rawDelta) {
        return CreateSeamFold.foldDelta(level, owner, owner.offset(rawDelta), rawDelta);
    }

    public static void normalizeGroup(ServerLevel level, MechanicalCrafterBlockEntity controller) {
        normalizeOwn(level, controller);
        ConnectedInputAccessor input = (ConnectedInputAccessor) controller.getInput();
        if (!input.toroidal$isController()) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();
        for (BlockPos delta : List.copyOf(input.toroidal$data())) {
            if (delta.equals(BlockPos.ZERO)) {
                continue;
            }

            BlockPos memberPos = CreateSeamFold.canonical(level, controllerPos.offset(delta));
            if (level.getBlockEntity(memberPos) instanceof MechanicalCrafterBlockEntity member) {
                normalizeOwn(level, member);
            }
        }
    }

    public static void normalizeOwn(ServerLevel level, MechanicalCrafterBlockEntity crafter) {
        List<BlockPos> data = ((ConnectedInputAccessor) crafter.getInput()).toroidal$data();
        BlockPos owner = crafter.getBlockPos();
        for (int index = 0; index < data.size(); index++) {
            BlockPos raw = data.get(index);
            BlockPos folded = foldStoredDelta(level, owner, raw);
            if (!folded.equals(raw)) {
                data.set(index, folded);
            }
        }
    }

    public static Set<BlockPos> canonicalMembers(@Nullable ServerLevel level, Set<BlockPos> raw) {
        Set<BlockPos> folded = new LinkedHashSet<>(raw.size());
        for (BlockPos member : raw) {
            folded.add(CreateSeamFold.canonical(level, member));
        }

        return folded;
    }

    private CrafterGroupFold() {
    }
}
