package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhProbes;
import com.toroidalworld.compat.distanthorizons.DhRepoLevel;
import com.toroidalworld.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;
import com.seibel.distanthorizons.core.sql.repo.AbstractDhRepo;

@Mixin(AbstractDhRepo.class)
public class AbstractDhRepoMixin implements DhRepoLevel {
    @Unique
    private IDhLevel toroidal$level;

    @Unique
    private Boolean toroidal$shapeSeen;

    @Override
    public void toroidal$bindLevel(IDhLevel level) {
        this.toroidal$level = level;
    }

    @Override
    public IDhLevel toroidal$level() {
        return this.toroidal$level;
    }

    @Override
    public ToroidalShape toroidal$shape() {
        ToroidalShape shape = DhShapes.ofFoldedKeys(this.toroidal$level);
        boolean present = shape != null;
        if (this.toroidal$shapeSeen == null || this.toroidal$shapeSeen != present) {
            this.toroidal$shapeSeen = present;
            DhProbes.repoShape(this, this.toroidal$level, present);
        }

        return shape;
    }

    @WrapMethod(method = "getByKey(Ljava/lang/Object;)Lcom/seibel/distanthorizons/core/sql/dto/IBaseDTO;")
    private IBaseDTO<?> toroidal$answerTheAskedKey(Object key, Operation<IBaseDTO<?>> original) {
        IBaseDTO<?> dto = original.call(key);
        if (dto != null && this.toroidal$shape() != null) {
            DhKeys.reseat(dto, key);
        }

        return dto;
    }
}
