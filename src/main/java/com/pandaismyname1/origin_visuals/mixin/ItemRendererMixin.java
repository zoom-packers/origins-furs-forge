package com.pandaismyname1.origin_visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pandaismyname1.origin_visuals.IPlayerMixins;
import dev.kosmx.playerAnim.api.TransformType;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemRendererMixin <T extends LivingEntity, M extends EntityModel<T> & ArmedModel> {
    void renderItemMixin(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean arm, PoseStack matrices, MultiBufferSource vertexStack, int something, CallbackInfo ci) {
        if (entity instanceof Player cPE && entity instanceof IPlayerMixins iPE) {
            var m = iPE.originalFur$getCurrentModel();
            if (m == null) {
                return;
            }
            Vec3 o = Vec3.ZERO;
            if (arm == true) {
                o = m.getLeftOffset();
            } else {
                o = m.getRightOffset();

            }
            matrices.translate(o.x, o.y, o.z);
        }

    }
}