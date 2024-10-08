package com.pandaismyname1.origin_visuals.mixin;

import com.pandaismyname1.origin_visuals.client.IGeoBone;
import mod.azure.azurelib.cache.object.GeoBone;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GeoBone.class)
public class GeoBoneMixin implements IGeoBone {
    @Shadow private boolean hidden;
    @Shadow @Final private Boolean dontRender;
    @Unique
    boolean orif$dHidden = false;
    @Override
    public boolean originfurs$isHiddenByDefault() {
        return IGeoBone.super.originfurs$isHiddenByDefault();
    }
    @Inject(method="<init>", at=@At("TAIL"))
    void initMixin(GeoBone parent, String name, Boolean mirror, Double inflate, Boolean dontRender, Boolean reset, CallbackInfo ci) {
        orif$dHidden = this.hidden;
    }

}
