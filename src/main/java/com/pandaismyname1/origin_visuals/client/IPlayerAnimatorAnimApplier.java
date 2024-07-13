package com.pandaismyname1.origin_visuals.client;


import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IPlayerAnimatorAnimApplier {
    default @Nullable IAnimation getTop() {return null;}
    default float getDelta() {return 0f;}
    default Vec3 getPositionForBone(String name) {return Vec3.ZERO;}
}
