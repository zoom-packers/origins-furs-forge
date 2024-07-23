package com.pandaismyname1.origin_visuals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pandaismyname1.origin_visuals.IPlayerMixins;
import com.pandaismyname1.origin_visuals.ModelRootAccessor;
import com.pandaismyname1.origin_visuals.OriginFurModel;


import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.common.capabilities.OriginContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FurRenderFeature <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public FurRenderFeature(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Unique
    private int getOverlayMixin(LivingEntity entity, float whiteOverlayProgress) {
        return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }
    public static class ModelTransformation {
        public Vec3 position, rotation;
        public ModelTransformation(Vec3 pos, Vec3 rot) {
            this.position = new Vec3(pos.x(), pos.y(), pos.z());
            this.rotation = new Vec3(rot.x(), rot.y(), rot.z());
        }
        public ModelTransformation(IAnimation anim, String bone_name) {
            Vec3f pos = anim.get3DTransform(bone_name, TransformType.POSITION, Minecraft.getInstance().getPartialTick(), new Vec3f(0,0,0));
            Vec3f rot = anim.get3DTransform(bone_name, TransformType.ROTATION, Minecraft.getInstance().getPartialTick(), new Vec3f(0,0,0));
            this.position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            this.rotation = new Vec3(rot.getX(), rot.getY(), rot.getZ());
        }
        public ModelTransformation invert(boolean x, boolean y, boolean z) {
            this.rotation = this.rotation.multiply(x ? -1 : 1, y ? -1 : 1, z ? -1 : 1);
            return this;
        }
        public ModelTransformation invert(boolean i) {
            this.rotation = this.rotation.scale(i ? -1 : 1);
            return this;
        }
    }
    @Override
    public void render(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity instanceof LocalPlayer abstractClientPlayerEntity) {
            if (abstractClientPlayerEntity.isInvisible() || abstractClientPlayerEntity.isSpectator()) {return;}
            var iPEM = (IPlayerMixins) abstractClientPlayerEntity;
            OriginalFurClient.OriginFur fur = iPEM.originalFur$getCurrentFur();
            if (fur == null){return;}
            Origin o = fur.currentAssociatedOrigin;
            if (o == null) {return;}
            var eR = (PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(abstractClientPlayerEntity);
            var eRA = (IPlayerMixins) eR;
            var acc = (ModelRootAccessor)eR.getModel();
            var a = fur.getAnimatable();
            OriginFurModel m = (OriginFurModel) fur.getGeoModel();
            Origin finalO = o;
            m.getAnimationProcessor().getRegisteredBones().forEach(coreGeoBone -> {
                if (((IGeoBone)coreGeoBone).originfurs$isHiddenByDefault()) {
                    return;
                }
                m.preprocess(finalO, eR, eRA, acc, abstractClientPlayerEntity);
            });
            fur.setPlayer(abstractClientPlayerEntity);
            var lAP = eR.getModel().leftArmPose;
            var rAP = eR.getModel().rightArmPose;
            for (int i = 0; i < 2; i++) {
                matrixStack.pushPose();
                matrixStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(180)));
//                matrixStack.multiply(new Quaternionf().rotateY((-abstractClientPlayerEntity.yaw(tickDelta)) * MathHelper.RADIANS_PER_DEGREE));
                matrixStack.translate(0, -1.51f, 0);
                Minecraft.getInstance().getProfiler().push("copy_mojmap");
//
                m.resetBone("bipedHead");
                m.resetBone("bipedBody");
                m.resetBone("bipedLeftArm");
                m.resetBone("bipedRightArm");
                m.resetBone("bipedLeftLeg");
                m.resetBone("bipedRightLeg");

                m.setRotationForBone("bipedHead", ((IMojModelPart)(Object)eR.getModel().head).originfurs$getRotation());
                m.translatePositionForBone("bipedHead", ((IMojModelPart)(Object)eR.getModel().head).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedBody", ((IMojModelPart)(Object)eR.getModel().body).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedLeftArm", ((IMojModelPart)(Object)eR.getModel().leftArm).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedRightArm", ((IMojModelPart)(Object)eR.getModel().rightArm).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedLeftLeg", ((IMojModelPart)(Object)eR.getModel().rightLeg).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedRightLeg", ((IMojModelPart)(Object)eR.getModel().leftLeg).originfurs$getPosition().scale(-16f));
                m.translatePositionForBone("bipedLeftArm", new Vec3(5,2,0));
                m.translatePositionForBone("bipedRightArm", new Vec3(-5,2,0));
//                m.translatePositionForBone("bipedLeftLeg", new Vec3(-1.9999,11.98,0.02));
                m.translatePositionForBone("bipedLeftLeg", new Vec3(-2,12,0));
                m.translatePositionForBone("bipedRightLeg", new Vec3(2,12,0));

                var isCrouching = abstractClientPlayerEntity.isCrouching();
                if (isCrouching) {
                    m.translatePositionForBone("bipedHead", new Vec3(0, -4, 0));
                    m.translatePositionForBone("bipedBody", new Vec3(0, -3, 0));
                    m.translatePositionForBone("bipedLeftArm", new Vec3(0, -3, 0));
                    m.translatePositionForBone("bipedRightArm", new Vec3(0, -3, 0));
                }

                matrixStack.translate(-0.5, -0.5, -0.5);
                m.setRotationForBone("bipedBody", ((IMojModelPart)(Object)eR.getModel().body).originfurs$getRotation());
                m.invertRotForPart("bipedBody", false, true, false);
                m.setRotationForBone("bipedLeftArm", ((IMojModelPart)(Object)eR.getModel().leftArm).originfurs$getRotation());
                m.setRotationForBone("bipedRightArm", ((IMojModelPart)(Object)eR.getModel().rightArm).originfurs$getRotation());
                m.setRotationForBone("bipedLeftLeg", ((IMojModelPart)(Object)eR.getModel().rightLeg).originfurs$getRotation());
                m.setRotationForBone("bipedRightLeg", ((IMojModelPart)(Object)eR.getModel().leftLeg).originfurs$getRotation());
                m.invertRotForPart("bipedHead", false, true, true);
                m.invertRotForPart("bipedRightArm", false, true, true);
                m.invertRotForPart("bipedLeftArm", false, true, true);
                m.invertRotForPart("bipedRightLeg", false, true, true);
                m.invertRotForPart("bipedLeftLeg", false, true, true);
                Minecraft.getInstance().getProfiler().push("render");
                if (i == 0) {
                    fur.render(matrixStack, a, vertexConsumerProvider, RenderType.entityTranslucent(m.getTextureResource(a)), null, light);
                } else {
                    fur.render(matrixStack, a, vertexConsumerProvider, RenderType.entityTranslucentEmissive(m.getFullbrightTextureResource(a)), null, Integer.MAX_VALUE - 1);
                }

                Minecraft.getInstance().getProfiler().pop();
                Minecraft.getInstance().getProfiler().pop();
//                m.popScl("bipedLeftLeg");
//                m.popScl("bipedRightLeg");
                matrixStack.popPose();
            }
        }
        Minecraft.getInstance().getProfiler().pop();
    }
}
