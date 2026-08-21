package com.toroidalworld.compat.create;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class CreateFactoryPanelFold {
    public static FactoryPanelPosition canonical(@Nullable Level level, FactoryPanelPosition position) {
        BlockPos canonical = CreateSeamFold.canonical(level, position.pos());
        return canonical.equals(position.pos()) ? position : new FactoryPanelPosition(canonical, position.slot());
    }

    public static void canonicalisePanels(@Nullable Level level,
            Map<FactoryPanelPosition, FactoryPanelConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }

        Map<FactoryPanelPosition, FactoryPanelConnection> folded = new LinkedHashMap<>(connections.size());
        boolean moved = false;
        for (FactoryPanelConnection connection : connections.values()) {
            FactoryPanelPosition from = canonical(level, connection.from);
            moved |= !from.equals(connection.from);
            connection.from = from;
            folded.put(from, connection);
        }

        if (moved) {
            connections.clear();
            connections.putAll(folded);
        }
    }

    public static void canonicaliseLinks(@Nullable Level level, Map<BlockPos, FactoryPanelConnection> connections) {
        if (connections.isEmpty()) {
            return;
        }

        Map<BlockPos, FactoryPanelConnection> folded = new LinkedHashMap<>(connections.size());
        boolean moved = false;
        for (FactoryPanelConnection connection : connections.values()) {
            FactoryPanelPosition from = canonical(level, connection.from);
            moved |= !from.equals(connection.from);
            connection.from = from;
            folded.put(from.pos(), connection);
        }

        if (moved) {
            connections.clear();
            connections.putAll(folded);
        }
    }

    public static void canonicaliseTargets(@Nullable Level level, Set<FactoryPanelPosition> targets) {
        if (targets.isEmpty()) {
            return;
        }

        Set<FactoryPanelPosition> folded = new LinkedHashSet<>(targets.size());
        boolean moved = false;
        for (FactoryPanelPosition target : targets) {
            FactoryPanelPosition canonical = canonical(level, target);
            moved |= !canonical.equals(target);
            folded.add(canonical);
        }

        if (moved) {
            targets.clear();
            targets.addAll(folded);
        }
    }

    private CreateFactoryPanelFold() {
    }
}
