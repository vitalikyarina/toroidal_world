package com.toroidalworld.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.api.v1.ToroidalShape;
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
    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILjava/lang/Long;)I")
    private int toroidal$foldWhereKey(PreparedStatement statement, int index, Long pos, Operation<Integer> original) {
        return original.call(statement, index, DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos));
    }

    @WrapMethod(method = "createInsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldInsert(FullDataSourceV2DTO dto, Operation<PreparedStatement> original) {
        return DhKeys.withFoldedKey(DhRepoLevel.shapeOf(this), dto, () -> original.call(dto));
    }

    @WrapMethod(method = "createUpdateStatement(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldUpdate(FullDataSourceV2DTO dto, Operation<PreparedStatement> original) {
        return DhKeys.withFoldedKey(DhRepoLevel.shapeOf(this), dto, () -> original.call(dto));
    }

    @WrapMethod(method = "getAdjByPosAndDirection")
    private FullDataSourceV2DTO toroidal$foldAdjacent(long pos, EDhDirection direction,
            Operation<FullDataSourceV2DTO> original) {
        FullDataSourceV2DTO dto = original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos), direction);
        if (dto != null) {
            dto.pos = pos;
        }

        return dto;
    }

    @WrapMethod(method = "setApplyToParent")
    private void toroidal$foldApplyToParent(long pos, boolean applyToParent, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos), applyToParent);
    }

    @WrapMethod(method = "setApplyToChild")
    private void toroidal$foldApplyToChild(long pos, boolean applyToChild, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos), applyToChild);
    }

    @WrapMethod(method = "getColumnGenerationStepForPos")
    private void toroidal$foldGenerationStepPos(long pos, ByteArrayList output, Operation<Void> original) {
        original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos), output);
    }

    @WrapMethod(method = "getTimestampForPos")
    private Long toroidal$foldTimestampPos(long pos, Operation<Long> original) {
        return original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos));
    }

    @WrapMethod(method = "getDataSizeInBytes")
    private long toroidal$foldDataSizePos(long pos, Operation<Long> original) {
        return original.call(DhKeys.foldSection(DhRepoLevel.shapeOf(this), pos));
    }

    @WrapMethod(method = "getPositionsToUpdate(IIIZ)Lit/unimi/dsi/fastutil/longs/LongArrayList;")
    private LongArrayList toroidal$foldUpdateTarget(int targetBlockX, int targetBlockZ, int returnCount,
            boolean parentUpdates, Operation<LongArrayList> original) {
        ToroidalShape shape = DhRepoLevel.shapeOf(this);
        if (shape == null) {
            return original.call(targetBlockX, targetBlockZ, returnCount, parentUpdates);
        }

        return original.call(DhFold.foldBlock(shape, Direction.Axis.X, DhKeys.LEAF, targetBlockX),
                DhFold.foldBlock(shape, Direction.Axis.Z, DhKeys.LEAF, targetBlockZ), returnCount, parentUpdates);
    }
}
