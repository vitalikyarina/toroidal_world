package com.toroidalworld;

public class InjectionTargets {
    public static final String BLOCK_POS_CLOSER_THAN =
            "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z";

    public static final String BLOCK_POS_CLOSER_TO_CENTER_THAN =
            "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z";

    public static final String BLOCK_POS_DIST_MANHATTAN =
            "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I";

    public static final String BLOCK_POS_DIST_SQR =
            "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D";

    public static final String BLOCK_POS_DIST_TO_CENTER_SQR =
            "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D";

    public static final String BLOCK_POS_OFFSET_PACKED =
            "Lnet/minecraft/core/BlockPos;offset(JLnet/minecraft/core/Direction;)J";

    public static final String CHUNK_POS_PACK = "Lnet/minecraft/world/level/ChunkPos;pack(II)J";

    public static final String DENSITY_FUNCTION_COMPUTE =
            "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D";

    public static final String DISTANCE_PREDICATE_MATCHES =
            "Lnet/minecraft/advancements/criterion/DistancePredicate;matches(DDDDDD)Z";

    public static final String ENTITY_GET_EYE_POSITION =
            "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;";

    public static final String ENTITY_GET_X = "Lnet/minecraft/world/entity/Entity;getX()D";

    public static final String ENTITY_GET_Z = "Lnet/minecraft/world/entity/Entity;getZ()D";

    public static final String ENTITY_POSITION =
            "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;";

    public static final String LIGHT_CHUNK_GETTER_GET_CHUNK_FOR_LIGHTING =
            "Lnet/minecraft/world/level/chunk/LightChunkGetter;getChunkForLighting(II)Lnet/minecraft/world/level/chunk/LightChunk;";

    public static final String LIVING_ENTITY_GET_X = "Lnet/minecraft/world/entity/LivingEntity;getX()D";

    public static final String LIVING_ENTITY_GET_Z = "Lnet/minecraft/world/entity/LivingEntity;getZ()D";

    public static final String PATHFINDER_MOB_GET_HOME_POSITION =
            "Lnet/minecraft/world/entity/PathfinderMob;getHomePosition()Lnet/minecraft/core/BlockPos;";

    public static final String PHANTOM_MOVE_TARGET_POINT =
            "Lnet/minecraft/world/entity/monster/Phantom;moveTargetPoint:Lnet/minecraft/world/phys/Vec3;";

    public static final String POSITIONAL_RANDOM_FACTORY_AT =
            "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;";

    public static final String STATIC_CACHE_2D_CREATE =
            "Lnet/minecraft/util/StaticCache2D;create(IIILnet/minecraft/util/StaticCache2D$Initializer;)Lnet/minecraft/util/StaticCache2D;";

    public static final String STREAM_MIN = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;";

    public static final String STREAM_SORTED =
            "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;";

    public static final String VEC3_AT_CENTER_OF =
            "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_CLOSER_THAN =
            "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z";

    public static final String VEC3_DISTANCE_TO =
            "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D";

    public static final String VEC3_NEW = "(DDD)Lnet/minecraft/world/phys/Vec3;";

    public static final String VEC3_SUBTRACT =
            "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";
}
