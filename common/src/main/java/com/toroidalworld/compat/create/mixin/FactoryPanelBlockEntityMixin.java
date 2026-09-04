package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.toroidalworld.accessors.SeamKeyedBlockEntity;
import com.toroidalworld.compat.create.CreateFactoryPanelFold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = FactoryPanelBlockEntity.class, remap = false)
public abstract class FactoryPanelBlockEntityMixin implements SeamKeyedBlockEntity {
    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$rekeyOnRead(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        toroidal$rekey();
    }

    @Override
    public void toroidal$rekey() {
        FactoryPanelBlockEntity panelBE = (FactoryPanelBlockEntity) (Object) this;
        if (!(panelBE.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (FactoryPanelBehaviour panel : panelBE.panels.values()) {
            CreateFactoryPanelFold.canonicalise(serverLevel, panel);
        }
    }
}
