package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

// The phantom flies by no navigation and no shared arrival helper: its whole flight rests on a single point it is
// steering at, held in absolute coordinates on the phantom itself. The move control reads that point three times a tick
// as three raw differences from where the phantom stands, and turns two of them into a heading.
//
// The point is chosen with no seam between it and the phantom — a circle 5 to 15 blocks around an anchor, or the target
// itself. The phantom then flies over the boundary and is wrapped, and from that tick the difference carries the width
// of the world with the wrong sign. The heading points the long way round, so the phantom leaves for the far side of the
// world instead of continuing its orbit, and nothing brings it back: the point it is flying at never changes.
//
// The fold is taken on the field read, the way the bat's is, so the angle, the two lengths, the pitch and the three
// components of the movement it finally sets are all vanilla's own, computed on inputs that name one world copy.
// Folding at the write instead goes stale within a few ticks — the point is chosen once and the phantom wraps out from
// under it.
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public class PhantomMoveControlMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Phantom phantom;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/monster/Phantom;moveTargetPoint:Lnet/minecraft/world/phys/Vec3;",
                    opcode = Opcodes.GETFIELD))
    private Vec3 toroidal$moveTargetThroughSeam(Vec3 moveTargetPoint) {
        return SeamSteering.nearestCopy(this.phantom, moveTargetPoint);
    }
}
