package com.pandaismyname1.origin_visuals.mixin.client;


import com.mojang.blaze3d.vertex.PoseStack;
import com.pandaismyname1.origin_visuals.IPlayerMixins;
import com.pandaismyname1.origin_visuals.ModelRootAccessor;
import com.pandaismyname1.origin_visuals.OriginFurModel;
import com.pandaismyname1.origin_visuals.client.FurRenderFeature;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static net.minecraft.world.entity.EquipmentSlot.*;

@Pseudo
@Mixin(value = PlayerRenderer.class, priority = 99999)
public class PlayerRendererMixin {

    @Pseudo
    @Mixin(value = PlayerModel.class, priority = 99999)
    public static abstract class PlayerModel$RootModel$Mixin implements ModelRootAccessor, IPlayerMixins {
        @Shadow
        @Final
        private boolean slim;


        @Unique
        ModelPart root;
        @Unique
        float elytraPitch = 0;
        @Unique
        boolean justStartedFlying = false;

        @Override
        public boolean originalFur$justUsedElytra() {
            return justStartedFlying;
        }

        @Override
        public float originalFur$elytraPitch() {
            return elytraPitch;
        }

        @Override
        public void originalFur$setElytraPitch(float f) {
            elytraPitch = f;
        }

        @Override
        public void originalFur$setJustUsedElytra(boolean b) {
            justStartedFlying = b;
        }

        @Inject(method = "<init>", at = @At("TAIL"))
        void initMixin(ModelPart root, boolean thinArms, CallbackInfo ci) {
            this.root = root;

        }

        @Override
        public ModelPart originalFur$getRoot() {
            return root;
        }

        @Unique
        boolean proc_slim = false;

        @Override
        public boolean originalFur$hasProcessedSlim() {
            return proc_slim;
        }

        @Override
        public void originalFur$setProcessedSlim(boolean state) {
            proc_slim = state;
        }

        @Override
        public boolean originalFur$isSlim() {
            return slim;
        }
    }

    @Pseudo
    @Mixin(value = LivingEntityRenderer.class, priority = 99999)
    public static abstract class LivingEntityRendererMixin$HidePlayerModelIfNeeded<T extends LivingEntity, M extends EntityModel<T>> implements IPlayerMixins {
        @Shadow
        @Final
        protected List<RenderLayer<T, M>> layers;

        @Inject(method = "<init>", at = @At(value = "TAIL"))
        void initMixin(EntityRendererProvider.Context ctx, EntityModel model, float shadowRadius, CallbackInfo ci) {
            if (model instanceof PlayerModel<?>) {
                //noinspection unchecked,rawtypes
                addLayer(new FurRenderFeature<>((LivingEntityRenderer) (Object) this));
            }
        }

        @Shadow
        public abstract M getModel();

        @Shadow
        protected M model;

        @Shadow
        protected abstract boolean isBodyVisible(T entity);

        @Unique
        private int getOverlayMixin(LivingEntity entity, float whiteOverlayProgress) {
            return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
        }

        @Shadow
        protected abstract float getWhiteOverlayProgress(T entity, float tickDelta);

        @Shadow
        protected abstract boolean addLayer(RenderLayer<T, M> feature);

        @Unique
        boolean isInvisible = false;

        @Override
        public boolean originalFur$isPlayerInvisible() {
            return isInvisible;
        }

        @Inject(method = "render", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                shift = At.Shift.BEFORE))
        private void renderPreProcessMixin(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
            if (livingEntity instanceof Player abstractClientPlayerEntity) {
                isInvisible = false;
                var originCapability = OriginsAPI.ORIGIN_CONTAINER;
                var playerCapability = abstractClientPlayerEntity.getCapability(originCapability).orElse(null);
                if (playerCapability == null) {
                    return;
                }
                for (var layer : OriginsAPI.getLayersRegistry()) {
                    var origin = playerCapability.getOrigin(layer);
                    if (origin == null) {
                        return;
                    }
                    Minecraft.getInstance().getProfiler().push("originalfurs:" + origin.location().getPath());
                    ResourceLocation id = origin.location();
                    // TODO Adapt for multiple furs
                    var furs = ((IPlayerMixins) abstractClientPlayerEntity).originalFur$getCurrentFur();
                    for (var fur : furs) {
                        if (fur == null) {
                            continue;
                        }
                        OriginFurModel m_Model = (OriginFurModel) fur.getGeoModel();

                        m_Model.preRender$mixinOnly(abstractClientPlayerEntity);
                        if (m_Model.isPlayerModelInvisible()) {
                            isInvisible = true;
                            matrixStack.translate(0, 9999, 0);
                        } else {
                            isInvisible = false;
                        }

                        if (!isInvisible) {
                            var p = m_Model.getHiddenParts();
                            var model = (PlayerModel<?>) this.getModel();
                            model.hat.skipDraw = model.hat.skipDraw || p.contains(OriginFurModel.VMP.hat);
                            model.head.skipDraw = model.head.skipDraw || p.contains(OriginFurModel.VMP.head);
                            model.body.skipDraw = model.body.skipDraw || p.contains(OriginFurModel.VMP.body);
                            model.jacket.skipDraw = model.jacket.skipDraw || p.contains(OriginFurModel.VMP.jacket);
                            model.leftArm.skipDraw = model.leftArm.skipDraw || p.contains(OriginFurModel.VMP.leftArm);
                            model.leftSleeve.skipDraw = model.leftSleeve.skipDraw || p.contains(OriginFurModel.VMP.leftSleeve);
                            model.rightArm.skipDraw = model.rightArm.skipDraw || p.contains(OriginFurModel.VMP.rightArm);
                            model.rightSleeve.skipDraw = model.rightSleeve.skipDraw || p.contains(OriginFurModel.VMP.rightSleeve);
                            model.leftLeg.skipDraw = model.leftLeg.skipDraw || p.contains(OriginFurModel.VMP.leftLeg);
                            model.leftPants.skipDraw = model.leftPants.skipDraw || p.contains(OriginFurModel.VMP.leftPants);
                            model.rightLeg.skipDraw = model.rightLeg.skipDraw || p.contains(OriginFurModel.VMP.rightLeg);
                            model.rightPants.skipDraw = model.rightPants.skipDraw || p.contains(OriginFurModel.VMP.rightPants);


                            var armorLayers = this.layers.stream().filter((layer1) -> layer1 instanceof HumanoidArmorLayer).toArray();
                            for (var l : armorLayers) {
                                var humanoidArmorLayer = (HumanoidArmorLayer) l;
                                if (humanoidArmorLayer.innerModel != null) {
                                    humanoidArmorLayer.innerModel.hat.skipDraw = model.hat.skipDraw || p.contains(OriginFurModel.VMP.hat);
                                    humanoidArmorLayer.innerModel.head.skipDraw = model.head.skipDraw || p.contains(OriginFurModel.VMP.head);
                                    humanoidArmorLayer.innerModel.body.skipDraw = model.body.skipDraw || p.contains(OriginFurModel.VMP.body);
                                    humanoidArmorLayer.innerModel.leftArm.skipDraw = model.leftArm.skipDraw || p.contains(OriginFurModel.VMP.leftArm);
                                    humanoidArmorLayer.innerModel.rightArm.skipDraw = model.rightArm.skipDraw || p.contains(OriginFurModel.VMP.rightArm);
                                    humanoidArmorLayer.innerModel.leftLeg.skipDraw = model.leftLeg.skipDraw || p.contains(OriginFurModel.VMP.leftLeg);
                                    humanoidArmorLayer.innerModel.rightLeg.skipDraw = model.rightLeg.skipDraw || p.contains(OriginFurModel.VMP.rightLeg);
                                }
                                if (humanoidArmorLayer.outerModel != null) {
                                    humanoidArmorLayer.outerModel.hat.skipDraw = model.hat.skipDraw || p.contains(OriginFurModel.VMP.hat);
                                    humanoidArmorLayer.outerModel.head.skipDraw = model.head.skipDraw || p.contains(OriginFurModel.VMP.head);
                                    humanoidArmorLayer.outerModel.body.skipDraw = model.body.skipDraw || p.contains(OriginFurModel.VMP.body);
                                    humanoidArmorLayer.outerModel.leftArm.skipDraw = model.leftArm.skipDraw || p.contains(OriginFurModel.VMP.leftArm);
                                    humanoidArmorLayer.outerModel.rightArm.skipDraw = model.rightArm.skipDraw || p.contains(OriginFurModel.VMP.rightArm);
                                    humanoidArmorLayer.outerModel.leftLeg.skipDraw = model.leftLeg.skipDraw || p.contains(OriginFurModel.VMP.leftLeg);
                                    humanoidArmorLayer.outerModel.rightLeg.skipDraw = model.rightLeg.skipDraw || p.contains(OriginFurModel.VMP.rightLeg);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Inject(method = "render", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                shift = At.Shift.AFTER
        ))
        private void renderPostProcessMixin(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
            if (isInvisible) {
                matrixStack.translate(0, -9999, 0);
            }
        }

        @Inject(method = "render", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
                shift = At.Shift.AFTER
        ))
        private void renderOverlayTexture(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
            if (!isInvisible && livingEntity instanceof Player aCPE) {
                var originCapability = OriginsAPI.ORIGIN_CONTAINER;
                var playerCapability = aCPE.getCapability(originCapability).orElse(null);
                if (playerCapability == null) {
                    return;
                }
                int p = getOverlayMixin(livingEntity, this.getWhiteOverlayProgress(livingEntity, g));
                for (var layer : OriginsAPI.getLayersRegistry()) {
                    var origin = playerCapability.getOrigin(layer);
                    if (origin == null) {
                        return;
                    }
                    Minecraft.getInstance().getProfiler().push("originalfurs:" + origin.location().getPath());
                    ResourceLocation id = origin.location();
                    var furs = ((IPlayerMixins) aCPE).originalFur$getCurrentFur();
                    for (var fur : furs) {
                        if (fur == null) {
                            return;
                        }
                        var model = (ModelRootAccessor) (PlayerModel<?>) this.getModel();
                        OriginFurModel m_Model = (OriginFurModel) fur.getGeoModel();
                        var overlayTexture = m_Model.getOverlayTexture(model.originalFur$isSlim());
                        var emissiveTexture = m_Model.getEmissiveTexture(model.originalFur$isSlim());
                        boolean bl = this.isBodyVisible(livingEntity);
                        boolean bl2 = !bl && !livingEntity.isInvisibleTo(Minecraft.getInstance().player);
                        if (overlayTexture != null) {
                            RenderType l = null;
                            if (OriginalFurClient.isRenderingInWorld && ModList.get().isLoaded("oculus")) {
                                l = RenderType.entityCutoutNoCullZOffset(overlayTexture);
                            } else {
                                l = RenderType.entityCutout(overlayTexture);
                            }
                            this.model.renderToBuffer(matrixStack, vertexConsumerProvider.getBuffer(l), i, p, 1, 1, 1, bl2 ? 0.15F : 1.0F);
                        }
                        if (emissiveTexture != null) {

                            RenderType l = RenderType.entityTranslucentEmissive(emissiveTexture);
                            this.model.renderToBuffer(matrixStack, vertexConsumerProvider.getBuffer(l), i, p, 1, 1, 1, bl2 ? 0.15F : 1.0F);
                        }
                    }
                    var m = (PlayerModel<?>) this.getModel();
                    m.hat.skipDraw = false;
                    m.head.skipDraw = false;
                    m.body.skipDraw = false;
                    m.jacket.skipDraw = false;
                    m.leftArm.skipDraw = false;
                    m.leftSleeve.skipDraw = false;
                    m.rightArm.skipDraw = false;
                    m.rightSleeve.skipDraw = false;
                    m.leftLeg.skipDraw = false;
                    m.leftPants.skipDraw = false;
                    m.rightLeg.skipDraw = false;
                    m.rightPants.skipDraw = false;
                }
            }
        }
    }
}
