package com.pandaismyname1.origin_visuals.mixin;

import com.google.gson.JsonParser;
import com.pandaismyname1.origin_visuals.OriginEvents;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;

@Pseudo
@Mixin(OriginRegisters.class)
public class OriginsRegistryMixin {
//
//    @Mixin(ModPacketsS2C.class)
//    public static class OriginListMixin$ORIF{
//        @Inject(locals= LocalCapture.CAPTURE_FAILHARD,
//                method="lambda$receiveOriginList$4", at=@At(value="INVOKE", target = "Lio/github/apace100/origins/origin/OriginRegistry;register(Lnet/minecraft/util/ResourceLocation;Lio/github/apace100/origins/origin/Origin;)Lio/github/apace100/origins/origin/Origin;", shift = At.Shift.AFTER))
//        private static void onRecievedOriginsMixin(ResourceLocation[] ids, SerializableData.Instance[] origins, CallbackInfo ci, int i) throws IOException {
//            var manager = Minecraft.getInstance().getResourceManager();
//            String path = "furs";
//            ResourceLocation id = new ResourceLocation(ids[i].getNamespace(), ids[i].getPath());
//            id = new ResourceLocation(id.getNamespace(), id.getPath().replace('/', '.').replace('\\', '.'));
//            if (!ids[i].getNamespace().contentEquals("origins")) {
//                var id_tmp = id;
//                id = ids[i];
//                System.out.println(id);
//                if (!OriginalFurClient.FUR_RESOURCES.containsKey(id)) {
//                    id = id_tmp;
//                }
//            }
//            var fur = OriginalFurClient.FUR_RESOURCES.getOrDefault(id, null);
//            if (fur == null) {
//                OriginalFurClient.FUR_REGISTRY.put(id, new OriginalFurClient.OriginFur(JsonParser.parseString("{}").getAsJsonObject()));
//            } else {
//                OriginalFurClient.FUR_REGISTRY.put(id, new  OriginalFurClient.OriginFur(JsonParser.parseString(new String(fur.getInputStream().readAllBytes())).getAsJsonObject()));
//            }
//        }
//        @Inject(method="lambda$receiveOriginList$4", at=@At(value="HEAD"))
//        private static void onRecievedOriginsDefineMissingMixin(ResourceLocation[] ids, SerializableData.Instance[] origins, CallbackInfo ci) throws IOException {
//            var manager = Minecraft.getInstance().getResourceManager();
//            String path = "furs";
//            ResourceLocation id = new ResourceLocation("origins", "empty");
//            var fur = OriginalFurClient.FUR_RESOURCES.getOrDefault(id, null);
//            if (fur == null) {
//                OriginalFurClient.FUR_REGISTRY.put(id, new OriginalFurClient.OriginFur(JsonParser.parseString("{}").getAsJsonObject()));
//            } else {
//                OriginalFurClient.FUR_REGISTRY.put(id, new  OriginalFurClient.OriginFur(JsonParser.parseString(new String(fur.getInputStream().readAllBytes())).getAsJsonObject()));
//            }
//        }
//    }
//    @Inject(method="register(Lnet/minecraft/util/ResourceLocation;Lio/github/apace100/origins/origin/Origin;)Lio/github/apace100/origins/origin/Origin;", at=@At("RETURN"))
//    private static void registerMixin(ResourceLocation id, Origin origin, CallbackInfoReturnable<Origin> cir){
//        OriginEvents.ORIGIN_REGISTRY_ADDED_EVENT.invoker().onOriginAddedToRegistry(origin,id);
//    }
}
