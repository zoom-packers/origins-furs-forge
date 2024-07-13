package com.pandaismyname1.origin_visuals.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;

public interface IMojModelPart {
    default Vec3 originfurs$getPosition() {return Vec3.ZERO;}
    default Vec3 originfurs$getRotation() {return Vec3.ZERO;}
    default Vec3 originfurs$getScale() {return Vec3.ZERO;}
    default Vec3 originfurs$getPositionF() {return Vec3.ZERO;}
    default Vec3 originfurs$getRotationF() {return Vec3.ZERO;}
    default Vec3 originfurs$getScaleF() {return Vec3.ZERO;}
    default ModelPart originfurs$getHolderPart() {return null;}
}
