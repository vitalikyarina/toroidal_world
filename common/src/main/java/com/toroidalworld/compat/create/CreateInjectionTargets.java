package com.toroidalworld.compat.create;

public final class CreateInjectionTargets {
    public static final String CARRIAGE_POSITION_ANCHOR =
            "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;"
                    + "positionAnchor:Lnet/minecraft/world/phys/Vec3;";

    public static final String SCHEMATIC_PRINTER_LOAD_SCHEMATIC =
            "Lcom/simibubi/create/content/schematics/SchematicPrinter;loadSchematic"
                    + "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Z)V";

    public static final String TRACK_EDGE_GET_POSITION =
            "Lcom/simibubi/create/content/trains/graph/TrackEdge;getPosition"
                    + "(Lcom/simibubi/create/content/trains/graph/TrackGraph;D)Lnet/minecraft/world/phys/Vec3;";

    public static final String TRACK_NODE_LOCATION_GET_LOCATION =
            "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;";

    public static final String TRAIN_MAP_MANAGER_RENDER_AND_PICK =
            "Lcom/simibubi/create/compat/trainmap/TrainMapManager;renderAndPick(Lnet/minecraft/client/gui/GuiGraphics;"
                    + "IIZLnet/minecraft/client/renderer/Rect2i;)Ljava/util/List;";

    public static final String TRAVELLING_POINT_GET_POSITION =
            "Lcom/simibubi/create/content/trains/entity/TravellingPoint;getPosition"
                    + "(Lcom/simibubi/create/content/trains/graph/TrackGraph;)Lnet/minecraft/world/phys/Vec3;";

    private CreateInjectionTargets() {
    }
}
