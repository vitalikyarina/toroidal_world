package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.brigadier.Command;
import com.simibubi.create.infrastructure.command.GlueCommand;
import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(value = GlueCommand.class, remap = false)
public class GlueCommandMixin {
    @Unique
    private static final String FROM_ARGUMENT = "from";

    @Unique
    private static final String TO_ARGUMENT = "to";

    @ModifyArg(method = "register",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"))
    private static Command<CommandSourceStack> toroidal$refuseAnAmbiguousBond(Command<CommandSourceStack> glue) {
        return context -> {
            BlockPos from = BlockPosArgument.getLoadedBlockPos(context, FROM_ARGUMENT);
            BlockPos to = BlockPosArgument.getLoadedBlockPos(context, TO_ARGUMENT);
            SeamCommandErrors.requireUnambiguousRegion(
                    WorldLoopAttachments.wrappedTransformerOf(context.getSource().getLevel()),
                    BoundingBox.fromCorners(from, to));
            return glue.run(context);
        };
    }
}
