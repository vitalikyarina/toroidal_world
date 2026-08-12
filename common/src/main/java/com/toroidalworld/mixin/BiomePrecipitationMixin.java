package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

// The one temperature question that is handed no level at all: getPrecipitationAt takes only a position, so unlike
// shouldFreeze and shouldSnow it cannot bind for itself. Everything in the game that asks it through a level is
// already bound by the time it arrives here — but a caller may hold a biome and ask it directly, and one does: Iris
// feeds its shader uniforms from BiomeUniforms, which calls this on the biome every frame. Left unfolded those samples
// read the seam as an edge, and the sky the shader draws parts ways with the ground the server froze.
//
// Deliberately not a render-thread default sitting on the context: the same context carries every noise sample the
// client takes, and a folding left standing for the frame would reach machinery that has no business folding. The
// binding is opened around this one call and closed with it.
@Mixin(Biome.class)
public class BiomePrecipitationMixin {
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation toroidal$foldForLevellessCaller(
            BlockPos pos, Operation<Biome.Precipitation> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return original.call(pos);
        }

        // A caller that came in through a level bound the answer already, and that binding is the better one: it names
        // the level that was actually asked, where this can only name the one the client is looking at.
        if (GenerationTransformerContext.context().wrappedTransformer() != null) {
            return original.call(pos);
        }

        @Nullable ClientLevel level = minecraft.level;
        WorldLoopTransformer bounds =
                level == null ? null : WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (bounds == null) {
            return original.call(pos);
        }

        return GenerationTransformerContext.withTransformer(bounds, () -> original.call(pos));
    }
}
