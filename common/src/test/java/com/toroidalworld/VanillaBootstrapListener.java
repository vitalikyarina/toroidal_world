package com.toroidalworld;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import com.toroidalworld.platform.LoaderlessPlatform;
import com.toroidalworld.platform.Platforms;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class VanillaBootstrapListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Platforms.set(new LoaderlessPlatform());
    }
}
