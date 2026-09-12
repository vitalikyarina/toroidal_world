package com.toroidalworld.compat.create;

public final class CatnipInjectionTargets {
    public static final String NBT_HELPER_READ_BLOCK_POS =
            "Lnet/createmod/catnip/nbt/NBTHelper;readBlockPos"
                    + "(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;";

    public static final String OUTLINER_SHOW_LINE =
            "Lnet/createmod/catnip/outliner/Outliner;showLine"
                    + "(Ljava/lang/Object;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/createmod/catnip/outliner/Outline$OutlineParams;";

    private CatnipInjectionTargets() {
    }
}
