package com.toroidalworld.client.shape.mixin;

import com.toroidalworld.client.shape.ShapeCustomizers;
import com.toroidalworld.shape.WorldShape;
import com.toroidalworld.shape.WorldShapes;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public class WorldTabMixin {
    @Unique
    private static final Component toroidal$SHAPE_LABEL = Component.translatable("gui.toroidal_world.world_shape");

    @Unique
    private static final Component toroidal$CUSTOMIZE_LABEL = Component.translatable("selectWorld.customizeType");

    @Unique
    private static final int toroidal$SHAPE_BUTTON_WIDTH = 150;

    @Unique
    private static final int toroidal$SHAPE_BUTTON_HEIGHT = 20;

    @Unique
    private Button toroidal$customizeShapeButton;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/CommonLayouts;labeledElement(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/layouts/Layout;",
                    ordinal = 0))
    private void toroidal$addWorldShapeRow(CallbackInfo ci, @Local GridLayout.RowHelper helper) {
        helper.addChild(CycleButton.builder(WorldShape::label, WorldShapes.selected())
                .withValues(WorldShapes.shapes())
                .create(0, 0, toroidal$SHAPE_BUTTON_WIDTH, toroidal$SHAPE_BUTTON_HEIGHT, toroidal$SHAPE_LABEL,
                        (button, shape) -> {
                            WorldShapes.select(shape);
                            toroidal$refreshCustomizeButton();
                        }));

        this.toroidal$customizeShapeButton = helper.addChild(
                Button.builder(toroidal$CUSTOMIZE_LABEL, button -> toroidal$openCustomizer()).build());
        toroidal$refreshCustomizeButton();
    }

    @Unique
    private void toroidal$refreshCustomizeButton() {
        this.toroidal$customizeShapeButton.active = ShapeCustomizers.of(WorldShapes.selected()) != null;
    }

    @Unique
    private static void toroidal$openCustomizer() {
        ShapeCustomizers.Customizer customizer = ShapeCustomizers.of(WorldShapes.selected());
        if (customizer == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        if (parent != null) {
            minecraft.setScreen(customizer.createScreen(parent));
        }
    }
}
