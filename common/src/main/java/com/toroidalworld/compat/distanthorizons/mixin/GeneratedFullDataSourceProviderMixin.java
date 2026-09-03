package com.toroidalworld.compat.distanthorizons.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataSourceProvider;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2;
import com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalResult;

@Mixin(GeneratedFullDataSourceProvider.class)
public class GeneratedFullDataSourceProviderMixin {
    @WrapMethod(method = "queuePositionForRetrieval")
    private CompletableFuture<DataSourceRetrievalResult> toroidal$retrieveTheOneSection(Long genPos,
            Operation<CompletableFuture<DataSourceRetrievalResult>> original) {
        ToroidalShape shape = ((DhRepoLevel) ((FullDataSourceProviderV2) (Object) this).repo).toroidal$shape();
        return original.call(shape == null ? genPos : DhKeys.foldSection(shape, genPos));
    }
}
