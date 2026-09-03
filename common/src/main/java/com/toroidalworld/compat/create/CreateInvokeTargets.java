package com.toroidalworld.compat.create;

public final class CreateInvokeTargets {
    public static final String BLOCK_POS_SUBTRACT =
            "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;";

    public static final String TRACK_NODE_LOCATION_GET_LOCATION =
            "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;";

    private CreateInvokeTargets() {
    }
}
