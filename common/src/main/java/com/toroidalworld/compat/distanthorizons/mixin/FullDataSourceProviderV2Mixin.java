package com.toroidalworld.compat.distanthorizons.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2;
import com.seibel.distanthorizons.core.file.structure.ISaveStructure;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;

@Mixin(FullDataSourceProviderV2.class)
public class FullDataSourceProviderV2Mixin {
    @Shadow
    @Final
    public FullDataSourceV2Repo repo;

    @Inject(
            method = "<init>(Lcom/seibel/distanthorizons/core/level/IDhLevel;"
                    + "Lcom/seibel/distanthorizons/core/file/structure/ISaveStructure;Ljava/io/File;)V",
            at = @At("RETURN"))
    private void toroidal$bindRepoLevel(IDhLevel level, ISaveStructure saveStructure, File saveDirOverride,
            CallbackInfo ci) {
        ((DhRepoLevel) this.repo).toroidal$bindLevel(level);
    }

}
