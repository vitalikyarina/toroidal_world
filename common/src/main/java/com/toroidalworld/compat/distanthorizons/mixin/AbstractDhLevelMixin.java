package com.toroidalworld.compat.distanthorizons.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.seibel.distanthorizons.core.level.AbstractDhLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;
import com.seibel.distanthorizons.core.sql.repo.ChunkHashRepo;

@Mixin(AbstractDhLevel.class)
public class AbstractDhLevelMixin {
    @Shadow
    public ChunkHashRepo chunkHashRepo;

    @Shadow
    public BeaconBeamRepo beaconBeamRepo;

    @Inject(method = "createAndSetSupportingRepos", at = @At("RETURN"))
    private void toroidal$bindRepoLevels(File databaseFile, CallbackInfo ci) {
        IDhLevel level = (IDhLevel) (Object) this;
        if (this.chunkHashRepo != null) {
            ((DhRepoLevel) this.chunkHashRepo).toroidal$bindLevel(level);
        }

        if (this.beaconBeamRepo != null) {
            ((DhRepoLevel) this.beaconBeamRepo).toroidal$bindLevel(level);
        }
    }
}
