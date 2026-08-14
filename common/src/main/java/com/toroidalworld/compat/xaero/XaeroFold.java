package com.toroidalworld.compat.xaero;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// The bridge the Xaero's Minimap mixins talk to. Xaero reads the client level and the raw player position, which
// near the seam run whole world widths from the server's truth, and it has no public API to hook — so its render
// and distance reads are taken to the copy nearest the camera (a waypoint across the seam draws beside the player
// and measures the short way), and the spawn its multiplayer waypoint-store identity is derived from is folded to
// the canonical copy (the anchor sync legitimately re-sends the spawn as the copy nearest the player, so every lap
// would shift the id by a world width and open a fresh waypoint "world"). On an unwrapped level the shape is
// absent and every operation is the identity, byte-for-byte.
//
// The shape is re-resolved per call rather than cached: the synced bounds can arrive or change after the level
// exists, and a cached adapter would keep answering with the transformer it was built on.
public final class XaeroFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Object[] coordReadoutDisplays;

    private static ToroidalShape shape() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static BlockPos foldWorldNodeSpawn(BlockPos spawn) {
        ToroidalShape shape = shape();
        if (shape == null || spawn == null) {
            return spawn;
        }

        BlockPos folded = shape.fold(spawn);
        return folded.getX() == spawn.getX() && folded.getZ() == spawn.getZ() ? spawn : folded;
    }

    // The camera entity rather than the player: the element handlers measure everything against the render
    // position, which follows whatever entity the camera rides (spectating included). Null only between levels,
    // where nothing renders from these reads anyway — identity keeps the call harmless.
    private static double cameraCoord(Direction.Axis axis) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return Double.NaN;
        }

        Vec3 position = camera.position();
        return axis == Direction.Axis.X ? position.x : position.z;
    }

    // An element render coordinate (already in map space) taken to the copy nearest the camera — the shared choke
    // point all three element-render handlers subtract the render position from.
    public static double nearestElementCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        double ref = cameraCoord(axis);
        if (Double.isNaN(ref)) {
            return coord;
        }

        return shape.nearestCoord(axis, ref, coord);
    }

    // A waypoint block coordinate taken to the copy nearest the camera. The offset between copies is a whole
    // number of world widths, so the rounding only strips float error, never moves the block.
    public static int nearestWaypointBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        double ref = cameraCoord(axis);
        if (Double.isNaN(ref)) {
            return coord;
        }

        return (int) Math.round(shape.nearestCoord(axis, ref, coord));
    }

    // The player position handed to the coordinate readouts under the minimap (coords / overworld coords / chunk
    // coords lines), folded canonical. Only those three displays: the others on the same pipeline (biome, light)
    // query the client level at this position, and the client's loaded chunks sit at the mirror coordinates — a
    // folded position would read unloaded ground. The displays are told apart by identity against the three
    // BuiltInInfoDisplays constants, resolved reflectively once — there is no compile dependency on Xaero.
    public static BlockPos foldInfoDisplayPos(Object infoDisplay, BlockPos playerPos) {
        ToroidalShape shape = shape();
        if (shape == null || playerPos == null) {
            return playerPos;
        }

        Object[] targets = coordReadoutDisplays();
        boolean isCoordReadout = false;
        for (Object target : targets) {
            if (target == infoDisplay) {
                isCoordReadout = true;
                break;
            }
        }

        if (!isCoordReadout) {
            return playerPos;
        }

        BlockPos folded = shape.fold(playerPos);
        return folded.getX() == playerPos.getX() && folded.getZ() == playerPos.getZ() ? playerPos : folded;
    }

    private static Object[] coordReadoutDisplays() {
        Object[] resolved = coordReadoutDisplays;
        if (resolved != null) {
            return resolved;
        }

        try {
            Class<?> displays = Class.forName("xaero.hud.minimap.info.BuiltInInfoDisplays");
            resolved = new Object[] {
                    displays.getField("COORDINATES").get(null),
                    displays.getField("OVERWORLD_COORDINATES").get(null),
                    displays.getField("CHUNK_COORDINATES").get(null),
            };
        } catch (ReflectiveOperationException e) {
            LOGGER.info("[xaero-compat] info_fold_targets_missing error={}", e.toString());
            resolved = new Object[0];
        }

        coordReadoutDisplays = resolved;
        return resolved;
    }

    private XaeroFold() {
    }
}
