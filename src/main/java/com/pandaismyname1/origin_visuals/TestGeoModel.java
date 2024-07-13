package com.pandaismyname1.origin_visuals;

import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class TestGeoModel extends GeoModel {
    private final ResourceLocation modelLocation = new ResourceLocation(OriginVisuals.MODID,"geo/test.geo.json");
    private final ResourceLocation textureLocation = new ResourceLocation(OriginVisuals.MODID,"textures/entity/test.png");
    @Override
    public ResourceLocation getModelResource(GeoAnimatable geoAnimatable) {
        return modelLocation;
    }

    @Override
    public ResourceLocation getTextureResource(GeoAnimatable geoAnimatable) {
        return textureLocation;
    }

    @Override
    public ResourceLocation getAnimationResource(GeoAnimatable geoAnimatable) {
        return null;
    }
}
