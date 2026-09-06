package com.toroidalworld.accessors;

public interface ClimateFieldMark {
    default boolean toroidal$climateField() {
        return false;
    }

    default void toroidal$markClimateField() {
    }
}
