package com.toroidalworld.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.sql.dto.ChunkHashDTO;
import com.seibel.distanthorizons.core.sql.repo.ChunkHashRepo;

@Mixin(ChunkHashRepo.class)
public class ChunkHashRepoMixin {
    @Unique
    private ToroidalShape toroidal$shape() {
        return ((DhRepoLevel) this).toroidal$shape();
    }

    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILcom/seibel/distanthorizons/core/pos/DhChunkPos;)I")
    private int toroidal$foldWhereKey(PreparedStatement statement, int index, DhChunkPos pos,
            Operation<Integer> original) {
        return original.call(statement, index, DhKeys.foldChunk(toroidal$shape(), pos));
    }

    @WrapMethod(method = "createInsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/ChunkHashDTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldInsert(ChunkHashDTO dto, Operation<PreparedStatement> original) {
        return DhKeys.withFoldedKey(toroidal$shape(), dto, () -> original.call(dto));
    }

    @WrapMethod(method = "createUpdateStatement(Lcom/seibel/distanthorizons/core/sql/dto/ChunkHashDTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldUpdate(ChunkHashDTO dto, Operation<PreparedStatement> original) {
        return DhKeys.withFoldedKey(toroidal$shape(), dto, () -> original.call(dto));
    }
}
