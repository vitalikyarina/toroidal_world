package com.toroidalworld.compat.create.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrainRelocator.class, remap = false)
public abstract class TrainRelocatorMixin {
    @Shadow
    static List<Vec3> toVisualise;

    @ModifyArg(method = "relocate",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            require = 2,
            allow = 2)
    private static Object toroidal$previewPointInTheViewerFrame(Object point) {
        if (!(point instanceof Vec3 raw)) {
            return point;
        }

        Vec3 anchor = toVisualise == null || toVisualise.isEmpty()
                ? toroidal$viewer()
                : toVisualise.get(toVisualise.size() - 1);
        return anchor == null ? point : CreateClientFrame.nearestCopy(anchor, raw);
    }

    @Unique
    private static @Nullable Vec3 toroidal$viewer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.position();
    }
}
