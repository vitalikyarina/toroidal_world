package com.toroidalworld.engine.gen;

final class CellGrid {
    static final int NO_CELL = -1;

    static final int AXES = 3;

    static final int AXIS_X = 0;

    static final int AXIS_Z = 1;

    static final int AXIS_Y = 2;

    private final int columns;

    private final int layers;

    CellGrid(int columns, int layers) {
        this.columns = columns;
        this.layers = layers;
    }

    int columns() {
        return this.columns;
    }

    int layers() {
        return this.layers;
    }

    int cells() {
        return this.columns * this.columns * this.layers;
    }

    int cell(int localX, int layer, int localZ) {
        return localX + localZ * this.columns + layer * this.columns * this.columns;
    }

    int localX(int cell) {
        return cell % this.columns;
    }

    int localZ(int cell) {
        return cell / this.columns % this.columns;
    }

    int layer(int cell) {
        return cell / (this.columns * this.columns);
    }

    boolean onSide(int cell) {
        int localX = this.localX(cell);
        int localZ = this.localZ(cell);
        return localX == 0 || localZ == 0 || localX == this.columns - 1 || localZ == this.columns - 1;
    }

    int neighbour(int cell, int axis, int step) {
        int localX = this.localX(cell);
        int localZ = this.localZ(cell);
        int layer = this.layer(cell);
        int nextX = axis == AXIS_X ? localX + step : localX;
        int nextZ = axis == AXIS_Z ? localZ + step : localZ;
        int nextLayer = axis == AXIS_Y ? layer + step : layer;
        if (nextX < 0 || nextZ < 0 || nextLayer < 0
                || nextX >= this.columns || nextZ >= this.columns || nextLayer >= this.layers) {
            return NO_CELL;
        }

        return this.cell(nextX, nextLayer, nextZ);
    }
}
