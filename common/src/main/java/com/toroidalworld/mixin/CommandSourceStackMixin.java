package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin {
    @Shadow
    @Final
    private Vec3 worldPosition;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private EntityAnchorArgument.Anchor anchor;

    // Where /execute in decides what "the same place" means in another dimension. Vanilla multiplies the source
    // position by the ratio of the two DimensionType coordinate scales — the nether's hardcoded 8 — but a looped world
    // chooses its own nether scale, so on a 1:2 nether the vanilla ratio lands four widths off, outside the small
    // nether entirely.
    //
    // The real ratio is read off the two dimensions' own bounds, per axis — through the one crossing operation the
    // portal path in NetherPortalBlockMixin also goes by, since the widths already carry the mapping and nothing else
    // stores it. An axis that does not close in both worlds has no width for a ratio to be read from, and keeps the
    // scale the dimensions declare. The mapped position is then wrapped into the destination's bounds — the source
    // position is a point the source world holds, so the scaled copy lands inside the destination by construction
    // today, but the wrap keeps that true if the two widths ever stop being set together.
    //
    // Only when both dimensions wrap. A crossing where either side is vanilla-shaped (a non-looped world, or a
    // datapack dimension the shape never touched) keeps vanilla's scale ratio untouched.
    @WrapOperation(
            method = "withLevel",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$mapByWidthRatio(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) ServerLevel newLevel) {
        WorldLoopTransformer destination = WorldLoopAttachments.wrappedTransformerOf(newLevel);
        WorldLoopTransformer source = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (destination == null || source == null) {
            return original.call(x, y, z);
        }

        double declaredScale = DimensionType.getTeleportationScale(
                this.level.dimensionType(), newLevel.dimensionType());
        Vec3 mapped = destination.mapFrom(source, this.worldPosition, declaredScale);
        return original.call(mapped.x, y, mapped.z);
    }

    // Turning the command source towards a point is the same subtraction Entity.lookAt makes, written out a second time
    // for a source that may have no entity behind it at all. Both ends are places the world holds, so nothing here
    // reads as wrong — but a pair straddling the bounds is a whole width apart in those numbers: the yaw names the far
    // copy and the overstated horizontal leg flattens the pitch towards level, so every ^local step resolved afterwards
    // walks off into empty ground.
    //
    // The point becomes its copy nearest the source before vanilla ever subtracts, which is where it physically is, and
    // the rotation then falls out of the vanilla arithmetic unchanged. The nearest copy rather than a folded delta
    // because the source's own position need not be inside the world — `/execute positioned ~500 ~ ~` puts it laps out,
    // and only a fold that wraps both ends first survives that. The anchor moves Y alone, so it is vanilla's own `from`
    // that is asked; Y comes back untouched and carries the real pitch.
    //
    // Both entries — `facing <x y z>` and `facing entity <target> <anchor>` — arrive here, the latter through
    // facing(Entity, Anchor). An unwrapped dimension keeps the vanilla path.
    @ModifyVariable(
            method = "facing(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/commands/CommandSourceStack;",
            at = @At("HEAD"),
            argsOnly = true)
    private Vec3 toroidal$faceNearestCopy(Vec3 pos) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return pos;
        }

        Vec3 from = this.anchor.apply((CommandSourceStack) (Object) this);
        return transformer.vectors.nearestCopy(from, pos);
    }
}
