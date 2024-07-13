package com.pandaismyname1.origin_visuals.mixin;

import com.pandaismyname1.origin_visuals.client.IMojModelPart;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public abstract class Mojang_ModelPartMixin implements IMojModelPart {
    @Shadow public float x;

    @Shadow public float y;

    @Shadow public float z;

    @Shadow public abstract PartPose getInitialPose();

    @Shadow public float xScale;

    @Shadow public float yScale;

    @Shadow public float zScale;

    @Shadow public float xRot;

    @Shadow public float yRot;

    @Shadow public float zRot;

    @Shadow public abstract ModelPart getChild(String name);

    @Override
    public ModelPart originfurs$getHolderPart() {
        return this.getChild("holder");
    }

    @Override
    public Vec3 originfurs$getPosition() {
        var t = getInitialPose();
        return new Vec3(t.x / 16, t.y / 16, t.z / 16);
    }
    @Inject(method="<init>", at=@At("TAIL"))
    void createHolderMixin(List<ModelPart.Cube> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {

    }
    @Override
    public Vec3 originfurs$getScale() {
        return new Vec3(xScale, yScale, zScale);
    }

    @Override
    public Vec3 originfurs$getRotation() {
        return new Vec3(xRot, yRot, zRot);
    }
}
