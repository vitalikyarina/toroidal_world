package com.toroidalworld.compat.create.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem.ShoppingList;
import com.simibubi.create.content.logistics.tableCloth.TableClothOverlayRenderer;
import com.toroidalworld.compat.create.client.CreateClientFrame;

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

        List<IntAttached<BlockPos>> folded = null;
        List<IntAttached<BlockPos>> purchases = canonical.purchases();
        for (int entry = 0; entry < purchases.size(); entry++) {
            IntAttached<BlockPos> purchase = purchases.get(entry);
            BlockPos stored = purchase.getValue();
            BlockPos nearest = CreateClientFrame.nearestCopy(Minecraft.getInstance().level, stored);
            if (nearest != stored && folded == null) {
                folded = new ArrayList<>(purchases.subList(0, entry));
            }

            if (folded != null) {
                folded.add(IntAttached.with(purchase.getFirst(), nearest));
            }
        }

        return folded == null ? canonical
                : new ShoppingList(folded, canonical.shopOwner(), canonical.shopNetwork());
    }
}
