package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.toroidalworld.compat.create.client.CreateClientFrame;
import com.toroidalworld.core.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SchematicTransformation.class, remap = false)
public class SchematicTransformationMixin {
    @Shadow
    private Vec3 chasingPos;

    @Shadow
    private Vec3 prevChasingPos;

    @Shadow
    private BlockPos target;

    @ModifyVariable(method = "init", argsOnly = true, at = @At("HEAD"))
    private BlockPos toroidal$anchorInViewerFrame(BlockPos anchor) {
        return CreateClientFrame.inViewerFrame(anchor);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void toroidal$reanchorOnViewer(CallbackInfo ci) {
        DeckTransformation seat = CreateClientFrame.inViewerFrameTransformation(target);
        if (seat.isIdentity()) {
            return;
        }

        target = seat.apply(target);
        chasingPos = seat.apply(chasingPos);
        prevChasingPos = seat.apply(prevChasingPos);
    }
}
