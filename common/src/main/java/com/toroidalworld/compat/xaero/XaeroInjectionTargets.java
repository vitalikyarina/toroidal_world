package com.toroidalworld.compat.xaero;

public final class XaeroInjectionTargets {
    public static final String MAP_PROCESSOR_GET_LEAF_MAP_REGION =
            "Lxaero/map/MapProcessor;getLeafMapRegion(IIIZ)Lxaero/map/region/MapRegion;";
    public static final String MAP_RENDER_HELPER_RENDER_DYNAMIC_HIGHLIGHT =
            "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V";

    private XaeroInjectionTargets() {
    }
}
