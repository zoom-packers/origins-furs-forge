package com.pandaismyname1.origin_visuals.mixin;

import com.pandaismyname1.origin_visuals.IPlayerMixins;
import com.pandaismyname1.origin_visuals.OriginFurModel;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Pseudo
@Mixin(Player.class)
public abstract class PlayerEntityMixin implements IPlayerMixins {
    @Shadow public abstract <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing);

    @Mixin(RemotePlayer.class)
    public static class OtherClientPlayerEntityMixin implements IPlayerMixins{

        @Unique
        boolean betterCombat$isSwinging = false;
        @Override
        public void betterCombat$setSwinging(boolean value) {
            betterCombat$isSwinging = value;
        }
        @Override
        public boolean betterCombat$isSwinging() {
            return betterCombat$isSwinging;
        }

    }

    @Pseudo
    @Mixin(AbstractClientPlayer.class)
    public static class ChangeElytraTextureMixin implements IPlayerMixins{
        @Inject(method="getElytraTextureLocation", at=@At("JUMP"), cancellable = true)
        void getElytraTextureMixin(CallbackInfoReturnable<ResourceLocation> cir) {
            var origins = originalFur$getCurrentFur();
            for (var origin : origins) {
                if (origin == null) {
                    continue;
                }
                OriginFurModel m = originalFur$getCurrentModel(origin);
                if (m == null) {return;}
                if (!m.hasCustomElytraTexture()) {
                    continue;
                }
                var eT = m.getElytraTexture();
                cir.setReturnValue(eT);
            }
            cir.cancel();
        }
    }
    @Override
    public List<OriginalFurClient.OriginFur> originalFur$getCurrentFur() {
        var currentOrigins = originalFur$currentOrigins();
        if (currentOrigins.length == 0) {
            return new ArrayList<>();
        }
        var result = new ArrayList<OriginalFurClient.OriginFur>();
        var layersSet = OriginsAPI.getLayersRegistry().entrySet();
        for (var entry : layersSet) {
            var layer = entry.getValue();
            if (layer == null) {
                continue;
            }
            var layerId = entry.getKey();
            Origin origin = findOriginForLayer(layer, currentOrigins);
            if (origin == null) {
                continue;
            }
            var originId = OriginsAPI.getOriginsRegistry().getKey(origin);
            if (layerId.location().equals(new ResourceLocation("origins-classes", "class"))) {
                var classId = new ResourceLocation("origins", "origins-classes." + originId.getPath());
                var fur = OriginalFurClient.CLASSES_FUR_REGISTRY.getOrDefault(classId, null);
                if (fur == null) {
                    fur = OriginalFurClient.CLASSES_FUR_REGISTRY.getOrDefault(layerId.location(), null);
                }
                if (fur != null) {
                    result.add(fur);
                }
            } else if (layerId.location().equals(new ResourceLocation("origins", "origin"))) {
                var fur = OriginalFurClient.FUR_REGISTRY.getOrDefault(originId, null);
                if (fur == null) {
                    fur = OriginalFurClient.FUR_REGISTRY.getOrDefault(layerId.location(), null);
                }
                if (fur != null) {
                    result.add(fur);
                }
            }
        }
        return result;
    }

    private static Origin findOriginForLayer(OriginLayer layer, Origin[] currentOrigins) {
        var origins = layer.origins();
        for (var holder : origins) {
            var tempOrigin = holder.get();
            for (var origin : currentOrigins) {
                if (origin.getName().equals(tempOrigin.getName())) {
                    return origin;
                }
            }
        }
        return null;
    }

    @Inject(method="actuallyHurt", at=@At("TAIL"))
    void applyDamageMixin(DamageSource source, float amount, CallbackInfo ci){
        Player e = (Player)(Object)this;
        var origins = originalFur$getCurrentFur();
        for (var origin : origins) {
            if (origin == null) {
                continue;
            }
            OriginFurModel m = originalFur$getCurrentModel(origin);
            if (m == null) {continue;}
            var r = m.getHurtSoundResource();
            if (r.equals(new ResourceLocation("null"))) {return;}
            var sE = ForgeRegistries.SOUND_EVENTS.getValue(r);
            if (sE == null) {continue;}
            e.level().playSound(null, e.blockPosition(), sE, SoundSource.PLAYERS);
        }

    }
    @Override
    public Origin[] originalFur$currentOrigins() {
        var capabilty = OriginsAPI.ORIGIN_CONTAINER;
        var playerCapabilty = this.getCapability(capabilty, null).orElse(null);
        if (playerCapabilty == null) {
            return new Origin[0];
        }
        var resourceKeysCollection = playerCapabilty.getOrigins().values();
        var v = new ArrayList<Origin>();
        var originsRegistry = OriginsAPI.getOriginsRegistry();
        for (var value : resourceKeysCollection) {
            var origin = originsRegistry.get(value);
            if (origin != null) {
                v.add(origin);
            }
        }
        return v.toArray(new Origin[0]);
    }

    public ResourceLocation getOriginResourceLocation(Origin origin) {
        var registry = OriginsAPI.getOriginsRegistry();
        var id = registry.getKey(origin);
        return id;
    }
}
