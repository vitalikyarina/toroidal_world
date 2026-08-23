package com.toroidalworld.compat.create;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;

public final class FoldedLinkable implements IRedstoneLinkable {
    private final IRedstoneLinkable delegate;
    private final BlockPos location;

    public FoldedLinkable(IRedstoneLinkable delegate, BlockPos location) {
        this.delegate = delegate;
        this.location = location;
    }

    @Override
    public BlockPos getLocation() {
        return location;
    }

    @Override
    public int getTransmittedStrength() {
        return delegate.getTransmittedStrength();
    }

    @Override
    public void setReceivedStrength(int power) {
        delegate.setReceivedStrength(power);
    }

    @Override
    public boolean isListening() {
        return delegate.isListening();
    }

    @Override
    public boolean isAlive() {
        return delegate.isAlive();
    }

    @Override
    public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
        return delegate.getNetworkKey();
    }
}
