package com.toroidalworld.compat.create;

public final class CreateInvokeTargets {
    public static final String BLOCK_POS_SUBTRACT =
            "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;";

    public static final String TRACK_NODE_LOCATION_GET_LOCATION =
            "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;";

    public static final String BEZIER_CONNECTION_GET_BOUNDS =
            "Lcom/simibubi/create/content/trains/track/BezierConnection;getBounds()Lnet/minecraft/world/phys/AABB;";

    public static final String CARRIAGE_POSITION_ANCHOR =
            "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"
                    + "positionAnchor:Lnet/minecraft/world/phys/Vec3;";

    public static final String TRACK_EDGE_GET_POSITION =
            "Lcom/simibubi/create/content/trains/graph/TrackEdge;getPosition"
                    + "(Lcom/simibubi/create/content/trains/graph/TrackGraph;D)Lnet/minecraft/world/phys/Vec3;";

    private CreateInvokeTargets() {
    }
}
