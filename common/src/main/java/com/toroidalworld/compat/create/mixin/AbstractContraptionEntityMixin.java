package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public abstract class AbstractContraptionEntityMixin {
    @Shadow
    public abstract Vec3 getAnchorVec();

    @Shadow
    public abstract Vec3 getPrevAnchorVec();

    @ModifyVariable(method = "toLocalVector(Lnet/minecraft/world/phys/Vec3;FZ)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 toroidal$globalVecInTheAnchorFrame(Vec3 globalVec, @Local(argsOnly = true) boolean prevAnchor) {
        Vec3 anchor = prevAnchor ? getPrevAnchorVec() : getAnchorVec();
        return CreateSeamFold.nearestCopy(toroidal$self().level(), anchor, globalVec);
    }

    @ModifyVariable(method = "getContactPointMotion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 toroidal$contactPointInTheAnchorFrame(Vec3 globalContactPoint) {
        return CreateSeamFold.nearestCopy(toroidal$self().level(), getPrevAnchorVec(), globalContactPoint);
    }

    @ModifyExpressionValue(method = "shouldActorTrigger",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;position:Lnet/minecraft/world/phys/Vec3;"))
    private @Nullable Vec3 toroidal$previousActorPositionInTheCurrentFrame(@Nullable Vec3 previousPosition,
            @Local(argsOnly = true) Vec3 actorPosition) {
        if (previousPosition == null) {
            return null;
        }

        return CreateSeamFold.nearestCopy(toroidal$self().level(), actorPosition, previousPosition);
    }

    @WrapOperation(method = "moveCollidedEntitiesOnDisassembly",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/StructureTransform;"
                            + "apply(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$disassemblyLandingInTheAnchorFrame(StructureTransform transform, Vec3 localVec,
            Operation<Vec3> original) {
        return CreateSeamFold.nearestCopy(toroidal$self().level(), getAnchorVec(),
                original.call(transform, localVec));
    }

    private Entity toroidal$self() {
        return (Entity) (Object) this;
    }
}
