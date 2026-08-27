package com.toroidalworld.compat.create.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem.ShoppingList;
import com.simibubi.create.content.logistics.tableCloth.TableClothOverlayRenderer;
import com.toroidalworld.compat.create.client.CreateClientFrame;
import com.toroidalworld.core.FoldedCopies;

import net.createmod.catnip.data.IntAttached;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = TableClothOverlayRenderer.class, remap = false)
public abstract class TableClothOverlayRendererMixin {
    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/tableCloth/ShoppingListItem;"
                            + "getList(Lnet/minecraft/world/item/ItemStack;)"
                            + "Lcom/simibubi/create/content/logistics/tableCloth/ShoppingListItem$ShoppingList;"))
    private static @Nullable ShoppingList toroidal$purchaseKeysInTheViewerFrame(@Nullable ShoppingList canonical) {
        if (canonical == null) {
            return null;
        }

        List<IntAttached<BlockPos>> purchases = canonical.purchases();
        List<IntAttached<BlockPos>> folded = FoldedCopies.of(purchases, purchase -> {
            BlockPos stored = purchase.getValue();
            BlockPos nearest = CreateClientFrame.nearestCopy(Minecraft.getInstance().level, stored);
            return nearest == stored ? purchase : IntAttached.with(purchase.getFirst(), nearest);
        });

        return folded == purchases ? canonical
                : new ShoppingList(folded, canonical.shopOwner(), canonical.shopNetwork());
    }
}
