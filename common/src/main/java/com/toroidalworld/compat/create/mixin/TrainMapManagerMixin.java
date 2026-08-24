package com.toroidalworld.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.compat.trainmap.TrainMapManager;
import com.simibubi.create.compat.trainmap.TrainMapRenderer;
import com.simibubi.create.compat.trainmap.TrainMapSync;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.client.CarriageBogeyFrame;
import com.toroidalworld.compat.create.client.TrainMapFrame;
import com.toroidalworld.compat.create.client.TrainMapViewFold;
import com.toroidalworld.compat.create.client.TrainMapViewFold.Lap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrainMapManager.class, remap = false)
public abstract class TrainMapManagerMixin {
    @WrapOperation(method = "renderPhase",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getX()I",
                    ordinal = 1))
    private static int toroidal$foldSecondNodeX(TrackNodeLocation other, Operation<Integer> original,
            @Local(name = "nodeLocation") TrackNodeLocation anchor) {
        return TrainMapViewFold.foldNodeKeyX(anchor, original.call(other));
    }

    @WrapOperation(method = "renderPhase",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getZ()I",
                    ordinal = 1))
    private static int toroidal$foldSecondNodeZ(TrackNodeLocation other, Operation<Integer> original,
            @Local(name = "nodeLocation") TrackNodeLocation anchor) {
        return TrainMapViewFold.foldNodeKeyZ(anchor, original.call(other));
    }

    @WrapOperation(method = "drawPoints",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackEdge;getPosition"
                            + "(Lcom/simibubi/create/content/trains/graph/TrackGraph;D)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$canonicaliseStation(TrackEdge edge, TrackGraph graph, double t,
            Operation<Vec3> original) {
        return TrainMapViewFold.canonical(original.call(edge, graph, t));
    }

    @WrapOperation(method = "drawTrains",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/compat/trainmap/TrainMapSync$TrainMapSyncEntry;getPosition"
                            + "(IZD)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$canonicaliseCarriage(TrainMapSync.TrainMapSyncEntry entry, int carriageIndex,
            boolean firstBogey, double time, Operation<Vec3> original,
            @Share("carriageFrame") LocalRef<CarriageBogeyFrame> frameRef) {
        return CarriageBogeyFrame.inOneFrame(entry, carriageIndex, firstBogey, time, original, frameRef);
    }

    @WrapMethod(method = "redrawAll")
    private static void toroidal$bindRedrawFrame(ResourceKey<Level> dimension, Operation<Void> original) {
        TrainMapFrame.during(dimension, () -> original.call(dimension));
    }

    @WrapMethod(method = "renderAndPick")
    private static List<FormattedText> toroidal$pickAcrossCopies(GuiGraphics graphics, int mouseX, int mouseY,
            boolean linearFiltering, Rect2i bounds, Operation<List<FormattedText>> original) {
        return TrainMapFrame.during(TrainMapRenderer.INSTANCE.trackingDim,
                () -> toroidal$pickOnEachCopy(graphics, mouseX, mouseY, linearFiltering, bounds, original));
    }

    @Unique
    private static List<FormattedText> toroidal$pickOnEachCopy(GuiGraphics graphics, int mouseX, int mouseY,
            boolean linearFiltering, Rect2i bounds, Operation<List<FormattedText>> original) {
        Lap[] laps = TrainMapViewFold.laps(bounds);
        PoseStack pose = graphics.pose();
        List<FormattedText> hovered = null;
        for (Lap lap : laps) {
            Rect2i copy = new Rect2i(bounds.getX() - lap.offsetX(), bounds.getY() - lap.offsetZ(),
                    bounds.getWidth(), bounds.getHeight());
            pose.pushPose();
            pose.translate(lap.offsetX(), lap.offsetZ(), 0.0F);
            List<FormattedText> picked = original.call(graphics, mouseX - lap.offsetX(), mouseY - lap.offsetZ(),
                    linearFiltering, copy);
            pose.popPose();
            if (picked != null) {
                hovered = picked;
            }
        }

        return hovered;
    }
}
