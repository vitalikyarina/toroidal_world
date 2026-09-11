package com.toroidalworld.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.NavigationShifter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

@Mixin(Path.class)
public class PathMixin implements NavigationShifter {
    @Shadow
    @Final
    private List<Node> nodes;

    @Mutable
    @Shadow
    @Final
    private BlockPos target;

    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        for (Node node : this.nodes) {
            ((NavigationShifter) node).toroidal$shiftBy(shiftX, shiftZ);
        }

        this.target = this.target.offset(shiftX, 0, shiftZ);
    }
}
