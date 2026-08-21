package com.toroidalworld.compat.create;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class CreateInventoryFold {
    public static @Nullable InventoryIdentifier fold(Level level, @Nullable InventoryIdentifier identifier) {
        if (identifier == null) {
            return null;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return identifier;
        }

        return new SeamIdentifier(canonical(transformer, identifier), transformer);
    }

    private static InventoryIdentifier canonical(WorldLoopTransformer transformer, InventoryIdentifier identifier) {
        return switch (identifier) {
            case InventoryIdentifier.Single single ->
                    new InventoryIdentifier.Single(transformer.blocks.wrap(single.pos()));
            case InventoryIdentifier.MultiFace multiFace ->
                    new InventoryIdentifier.MultiFace(transformer.blocks.wrap(multiFace.pos()), multiFace.sides());
            case InventoryIdentifier.Pair pair -> new InventoryIdentifier.Pair(
                    transformer.blocks.wrap(pair.first()), transformer.blocks.wrap(pair.second()));
            case InventoryIdentifier.Bounds bounds -> seamBounds(transformer, bounds);
            default -> identifier;
        };
    }

    private static InventoryIdentifier seamBounds(WorldLoopTransformer transformer, InventoryIdentifier.Bounds bounds) {
        if (!transformer.crossesBounds(bounds.bounds())) {
            return bounds;
        }

        return new SeamBounds(List.copyOf(transformer.splitAcrossBounds(bounds.bounds())));
    }

    private CreateInventoryFold() {
    }

    private static final class SeamIdentifier implements InventoryIdentifier {
        private final InventoryIdentifier canonical;
        private final WorldLoopTransformer transformer;

        private SeamIdentifier(InventoryIdentifier canonical, WorldLoopTransformer transformer) {
            this.canonical = canonical;
            this.transformer = transformer;
        }

        @Override
        public boolean contains(BlockFace face) {
            BlockPos canonicalPos = transformer.blocks.wrap(face.getPos());
            return canonical.contains(
                    canonicalPos == face.getPos() ? face : new BlockFace(canonicalPos, face.getFace()));
        }

        // The transformer is how this identity folds, not what it names: letting it into equality would make Create
        // de-duplicate two levels' inventories differently in a wrapped world than in an unwrapped one.
        @Override
        public boolean equals(Object other) {
            return other instanceof SeamIdentifier seam && canonical.equals(seam.canonical);
        }

        @Override
        public int hashCode() {
            return canonical.hashCode();
        }

        @Override
        public String toString() {
            return canonical.toString();
        }
    }

    private record SeamBounds(List<BoundingBox> regions) implements InventoryIdentifier {
        @Override
        public boolean contains(BlockFace face) {
            for (BoundingBox region : regions) {
                if (region.isInside(face.getPos())) {
                    return true;
                }
            }

            return false;
        }
    }
}
