package com.toroidalworld.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhFold;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;

import net.minecraft.core.Direction;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

@Mixin(FullDataSourceV2Repo.class)
public class FullDataSourceV2RepoMixin {
    @Unique
    private ToroidalShape toroidal$shape() {
        return ((DhRepoLevel) this).toroidal$shape();
    }

    @Unique
    private long toroidal$fold(long pos) {
        ToroidalShape shape = toroidal$shape();
        return shape == null ? pos : DhKeys.foldSection(shape, pos);
    }

    @Unique
    private PreparedStatement toroidal$withFoldedPos(FullDataSourceV2DTO dto, Supplier<PreparedStatement> statement) {
        long raw = dto.pos;
        dto.pos = toroidal$fold(raw);
        try {
            return statement.get();
        } finally {
            dto.pos = raw;
        }
    }

    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILjava/lang/Long;)I")
    private int toroidal$foldWhereKey(PreparedStatement statement, int index, Long pos, Operation<Integer> original) {
        return original.call(statement, index, toroidal$fold(pos));
    }

    @WrapMethod(method = "createInsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldInsert(FullDataSourceV2DTO dto, Operation<PreparedStatement> original) {
        return toroidal$withFoldedPos(dto, () -> original.call(dto));
    }

    @WrapMethod(method = "createUpdateStatement(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldUpdate(FullDataSourceV2DTO dto, Operation<PreparedStatement> original) {
        return toroidal$withFoldedPos(dto, () -> original.call(dto));
    }

    @WrapMethod(method = "getAdjByPosAndDirection")
    private FullDataSourceV2DTO toroidal$foldAdjacent(long pos, EDhDirection direction,
            Operation<FullDataSourceV2DTO> original) {
        FullDataSourceV2DTO dto = original.call(toroidal$fold(pos), direction);
        if (dto != null) {
            dto.pos = pos;
        }

        return dto;
    }

    @WrapMethod(method = "setApplyToParent")
    private void toroidal$foldApplyToParent(long pos, boolean applyToParent, Operation<Void> original) {
        original.call(toroidal$fold(pos), applyToParent);
    }

    @WrapMethod(method = "setApplyToChild")
    private void toroidal$foldApplyToChild(long pos, boolean applyToChild, Operation<Void> original) {
        original.call(toroidal$fold(pos), applyToChild);
    }

    @WrapMethod(method = "getColumnGenerationStepForPos")
    private void toroidal$foldGenerationStepPos(long pos, ByteArrayList output, Operation<Void> original) {
        original.call(toroidal$fold(pos), output);
    }

    @WrapMethod(method = "getTimestampForPos")
    private Long toroidal$foldTimestampPos(long pos, Operation<Long> original) {
        return original.call(toroidal$fold(pos));
    }

    @WrapMethod(method = "getDataSizeInBytes")
    private long toroidal$foldDataSizePos(long pos, Operation<Long> original) {
        return original.call(toroidal$fold(pos));
    }

    @WrapMethod(method = "getPositionsToUpdate(IIIZ)Lit/unimi/dsi/fastutil/longs/LongArrayList;")
    private LongArrayList toroidal$foldUpdateTarget(int targetBlockX, int targetBlockZ, int returnCount,
            boolean parentUpdates, Operation<LongArrayList> original) {
        ToroidalShape shape = toroidal$shape();
        if (shape == null) {
            return original.call(targetBlockX, targetBlockZ, returnCount, parentUpdates);
        }

        return original.call(DhFold.foldBlock(shape, Direction.Axis.X, DhKeys.LEAF, targetBlockX),
                DhFold.foldBlock(shape, Direction.Axis.Z, DhKeys.LEAF, targetBlockZ), returnCount, parentUpdates);
    }
}
