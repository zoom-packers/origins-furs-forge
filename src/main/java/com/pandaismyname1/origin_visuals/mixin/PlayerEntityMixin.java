package com.pandaismyname1.origin_visuals.mixin;

import com.pandaismyname1.origin_visuals.IPlayerMixins;
import com.pandaismyname1.origin_visuals.OriginFurModel;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.Origin;
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
            OriginFurModel m = originalFur$getCurrentModel();
            // TODO IT's Breaking at originFur$getCurrentModel
            if (m == null) {return;}
            if (!m.hasCustomElytraTexture()) {
                return;
            }
            var eT = m.getElytraTexture();
            cir.setReturnValue(eT);
            cir.cancel();
        }
    }
    @Override
    public OriginalFurClient.OriginFur originalFur$getCurrentFur() {
        var cO = originalFur$currentOrigins();
        if (cO.length == 0) {return OriginalFurClient.OriginFur.NULL_OR_DEFAULT_FUR;}
        try {
            var origin = cO[0];
            ResourceLocation id = getOriginResourceLocation(origin);
            // TODO: replace this hack with a loop to iterate over a player's active origins (sometimes origins-classes is at idx 0.)
            if (id.getNamespace().equalsIgnoreCase("origins-classes")){
                for (Origin value : cO) {
                    origin = value;

                    if (getOriginResourceLocation(origin).getNamespace().equalsIgnoreCase("origins-classes")) {
                        continue;
                    }

                    id = getOriginResourceLocation(origin);
                }
                if (id.getNamespace().equalsIgnoreCase("origins-classes")){
                    System.out.println("[Origin Furs] No Origin was found in entity mixin: " + id + ". This should NEVER happen! Report this to the devs!");
                    System.out.println(OriginalFurClient.FUR_REGISTRY.keySet());
                    System.out.println("[Origin Furs] Listing all origins attached to player..");
                    Arrays.stream(cO).forEach(System.out::println);
                    System.out.println("[Origin Furs] Listed all registered furs. Please include the previous line!");
                    System.out.println("[Origin Furs] Please copy all mods, and this log file and create an issue:");
                    System.out.println("[Origin Furs] https://github.com/avetharun/OriginalFur/issues/new");
                    return OriginalFurClient.OriginFur.NULL_OR_DEFAULT_FUR;
                }
            }
            id = new ResourceLocation(id.getNamespace(), id.getPath().replace('/', '.').replace('\\', '.'));
            var opt = OriginalFurClient.FUR_REGISTRY.get(id);
            if (opt == null) {
                opt = OriginalFurClient.FUR_REGISTRY.get(new ResourceLocation("origins", id.getPath()));
                if (opt == null) {
                    System.out.println("[Origin Furs] Fur was null in entity mixin: " + id + ". This should NEVER happen! Report this to the devs!");
                    System.out.println(OriginalFurClient.FUR_REGISTRY.keySet());
                    System.out.println("[Origin Furs] Listed all registered furs. Please include the previous line!");
                    System.out.println("[Origin Furs] Please copy all mods, and this log file and create an issue:");
                    System.out.println("[Origin Furs] https://github.com/avetharun/OriginalFur/issues/new");
                    return null;
                }
            }
            opt.currentAssociatedOrigin = origin;
            return opt;
        } catch (IndexOutOfBoundsException IOBE) {
            System.err.println("[Origin Furs] Something very wrong happened!");
            System.err.println(OriginalFurClient.FUR_REGISTRY.keySet());
            System.err.println(Arrays.toString(originalFur$currentOrigins()));
            System.err.println("[Origin Furs] Listed all registered furs. Please include the previous line!");
            System.err.println("[Origin Furs] Please copy all mods, and this log file and create an issue:");
            System.err.println("[Origin Furs] https://github.com/avetharun/OriginalFur/issues/new");
            throw new RuntimeException(IOBE.fillInStackTrace().toString());
        }
    }

    @Inject(method="actuallyHurt", at=@At("TAIL"))
    void applyDamageMixin(DamageSource source, float amount, CallbackInfo ci){
        Player e = (Player)(Object)this;
        OriginFurModel m = originalFur$getCurrentModel();
        if (m == null) {return;}
        var r = m.getHurtSoundResource();
        if (r.equals(new ResourceLocation("null"))) {return;}
        var sE = ForgeRegistries.SOUND_EVENTS.getValue(r);
        if (sE == null) {return;}
        e.level().playSound(null, e.blockPosition(), sE, SoundSource.PLAYERS);

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
