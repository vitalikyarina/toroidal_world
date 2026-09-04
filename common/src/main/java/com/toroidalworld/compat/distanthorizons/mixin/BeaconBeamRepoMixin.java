package com.toroidalworld.compat.distanthorizons.mixin;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;

import net.minecraft.core.Direction;

@Mixin(BeaconBeamRepo.class)
public class BeaconBeamRepoMixin {
    @Unique
    private ToroidalShape toroidal$shape() {
        return ((DhRepoLevel) this).toroidal$shape();
    }

    @Unique
    private DhBlockPos toroidal$fold(DhBlockPos pos) {
        ToroidalShape shape = toroidal$shape();
        return shape == null ? pos : DhKeys.foldBlock(shape, pos);
    }

    @Unique
    private PreparedStatement toroidal$withFoldedPos(BeaconBeamDTO dto, Supplier<PreparedStatement> statement) {
        DhBlockPos raw = dto.blockPos;
        dto.blockPos = toroidal$fold(raw);
        try {
            return statement.get();
        } finally {
            dto.blockPos = raw;
        }
    }

    @WrapMethod(method = "setPreparedStatementWhereClause(Ljava/sql/PreparedStatement;ILcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos;)I")
    private int toroidal$foldWhereKey(PreparedStatement statement, int index, DhBlockPos pos,
            Operation<Integer> original) {
        return original.call(statement, index, toroidal$fold(pos));
    }

    @WrapMethod(method = "createInsertStatement(Lcom/seibel/distanthorizons/core/sql/dto/BeaconBeamDTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldInsert(BeaconBeamDTO dto, Operation<PreparedStatement> original) {
        return toroidal$withFoldedPos(dto, () -> original.call(dto));
    }

    @WrapMethod(method = "createUpdateStatement(Lcom/seibel/distanthorizons/core/sql/dto/BeaconBeamDTO;)Ljava/sql/PreparedStatement;")
    private PreparedStatement toroidal$foldUpdate(BeaconBeamDTO dto, Operation<PreparedStatement> original) {
        return toroidal$withFoldedPos(dto, () -> original.call(dto));
    }

    @WrapMethod(method = "getAllBeamsInBlockPosRange")
    private ArrayList<BeaconBeamDTO> toroidal$foldRange(int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ,
            Operation<ArrayList<BeaconBeamDTO>> original) {
        ToroidalShape shape = toroidal$shape();
        if (shape == null) {
            return original.call(minBlockX, maxBlockX, minBlockZ, maxBlockZ);
        }

        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        ArrayList<BeaconBeamDTO> beams = new ArrayList<>();
        for (int lapX : x.laps(minBlockX, maxBlockX)) {
            for (int lapZ : z.laps(minBlockZ, maxBlockZ)) {
                int offsetX = x.offset(lapX);
                int offsetZ = z.offset(lapZ);
                ArrayList<BeaconBeamDTO> lap = original.call(
                        x.clipMin(minBlockX - offsetX), x.clipMax(maxBlockX - offsetX),
                        z.clipMin(minBlockZ - offsetZ), z.clipMax(maxBlockZ - offsetZ));
                for (BeaconBeamDTO beam : lap) {
                    if (offsetX != 0 || offsetZ != 0) {
                        beam.blockPos = new DhBlockPos(beam.blockPos.getX() + offsetX, beam.blockPos.getY(),
                                beam.blockPos.getZ() + offsetZ);
                    }

                    beams.add(beam);
                }
            }
        }

        return beams;
    }
}
