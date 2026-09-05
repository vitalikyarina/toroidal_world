package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;

class SableConstraintJoinTest {
    private static final Vector3dc FIRST = new Vector3d(1.0, 2.0, 3.0);
    private static final Vector3dc SECOND = new Vector3d(4.0, 5.0, 6.0);
    private static final Vector3dc SEATED = new Vector3d(-511.0, 2.0, 3.0);
    private static final Quaterniondc ORIENTATION = new Quaterniond().rotateY(0.5);
    private static final Quaterniondc OTHER_ORIENTATION = new Quaterniond().rotateX(0.25);
    private static final Vector3dc NORMAL = new Vector3d(0.0, 1.0, 0.0);
    private static final Vector3dc OTHER_NORMAL = new Vector3d(1.0, 0.0, 0.0);
    private static final Set<ConstraintJointAxis> LOCKED = Set.of(ConstraintJointAxis.ANGULAR_X);
    private static final SableConstraintJoin.Anchors ANCHORS = new SableConstraintJoin.Anchors(FIRST, SECOND);

    @Test
    void fixedConfigurationRewritesOneAnchorAndKeepsTheRest() {
        FixedConstraintConfiguration config = new FixedConstraintConfiguration(FIRST, SECOND, ORIENTATION);

        assertEquals(ANCHORS, SableConstraintJoin.anchorsOf(config));
        assertEquals(new FixedConstraintConfiguration(SEATED, SECOND, ORIENTATION),
                SableConstraintJoin.withAnchor(config, true, SEATED));
        assertEquals(new FixedConstraintConfiguration(FIRST, SEATED, ORIENTATION),
                SableConstraintJoin.withAnchor(config, false, SEATED));
    }

    @Test
    void freeConfigurationRewritesOneAnchorAndKeepsTheRest() {
        FreeConstraintConfiguration config = new FreeConstraintConfiguration(FIRST, SECOND, ORIENTATION);

        assertEquals(ANCHORS, SableConstraintJoin.anchorsOf(config));
        assertEquals(new FreeConstraintConfiguration(SEATED, SECOND, ORIENTATION),
                SableConstraintJoin.withAnchor(config, true, SEATED));
        assertEquals(new FreeConstraintConfiguration(FIRST, SEATED, ORIENTATION),
                SableConstraintJoin.withAnchor(config, false, SEATED));
    }

    @Test
    void rotaryConfigurationRewritesOneAnchorAndKeepsTheNormals() {
        RotaryConstraintConfiguration config = new RotaryConstraintConfiguration(FIRST, SECOND, NORMAL, OTHER_NORMAL);

        assertEquals(ANCHORS, SableConstraintJoin.anchorsOf(config));
        assertEquals(new RotaryConstraintConfiguration(SEATED, SECOND, NORMAL, OTHER_NORMAL),
                SableConstraintJoin.withAnchor(config, true, SEATED));
        assertEquals(new RotaryConstraintConfiguration(FIRST, SEATED, NORMAL, OTHER_NORMAL),
                SableConstraintJoin.withAnchor(config, false, SEATED));
    }

    @Test
    void genericConfigurationRewritesOneAnchorAndKeepsBothOrientationsAndTheLock() {
        GenericConstraintConfiguration config = new GenericConstraintConfiguration(FIRST, SECOND, ORIENTATION,
                OTHER_ORIENTATION, LOCKED);

        assertEquals(ANCHORS, SableConstraintJoin.anchorsOf(config));
        assertEquals(new GenericConstraintConfiguration(SEATED, SECOND, ORIENTATION, OTHER_ORIENTATION, LOCKED),
                SableConstraintJoin.withAnchor(config, true, SEATED));
        assertEquals(new GenericConstraintConfiguration(FIRST, SEATED, ORIENTATION, OTHER_ORIENTATION, LOCKED),
                SableConstraintJoin.withAnchor(config, false, SEATED));
    }
}
