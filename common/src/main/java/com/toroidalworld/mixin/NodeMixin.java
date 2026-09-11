package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.NavigationShifter;

import net.minecraft.world.level.pathfinder.Node;

@Mixin(Node.class)
public class NodeMixin implements NavigationShifter {
    @Mutable
    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int y;

    @Mutable
    @Shadow
    @Final
    public int z;

    @Mutable
    @Shadow
    @Final
    private int hash;

    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        this.x += shiftX;
        this.z += shiftZ;
        this.hash = Node.createHash(this.x, this.y, this.z);
    }
}
