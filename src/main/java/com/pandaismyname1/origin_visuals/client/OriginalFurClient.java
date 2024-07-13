package com.pandaismyname1.origin_visuals.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pandaismyname1.origin_visuals.IPlayerMixins;
import com.pandaismyname1.origin_visuals.OriginFurAnimatable;
import com.pandaismyname1.origin_visuals.OriginFurModel;
import com.pandaismyname1.origin_visuals.OriginVisuals;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import mod.azure.azurelib.cache.object.*;
import mod.azure.azurelib.renderer.GeoObjectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class OriginalFurClient {


    public class FursDynamicRegistries {
        public static final ResourceKey<Registry<OriginFur>> FURS_REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation(OriginVisuals.MODID, "furs"));
        public static final ResourceKey<Registry<Resource>> FUR_RESOURCES_REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation(OriginVisuals.MODID, "fur_resources"));
    }


    public static class ItemRendererFeatureAnim extends dev.kosmx.playerAnim.api.layered.PlayerAnimationFrame {
        Player player;
        ItemRendererFeatureAnim(Player player) {
            super();
            this.player = player;
        }
        private int time = 0;
        @Override
        public void tick(){
            time++;
        }

        @Override
        public void setupAnim(float v) {
            if (player != null && player instanceof IPlayerMixins iPE) {
                var m = iPE.originalFur$getCurrentModel();
                if (m == null) {
                    return;
                }
                var lP = m.getLeftOffset();
                var rP = m.getRightOffset();
//                leftItem.pos = new Vec3f((float) lP.x, (float) lP.y, (float) lP.z);
//                rightItem.pos = new Vec3f((float) rP.x, (float) rP.y, (float) rP.z);

            }
        }

    }
    public static class OriginFur extends GeoObjectRenderer<OriginFurAnimatable> {
        public Origin currentAssociatedOrigin = Origin.EMPTY;
        public static final OriginFur NULL_OR_DEFAULT_FUR = new OriginFur(JsonParser.parseString("{}").getAsJsonObject());
        public void renderBone(String name, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, int packedLight) {
            poseStack.pushPose();
            var b = this.getGeoModel().getBone(name).orElse(null);
            if (b == null) {return;}
            if (buffer == null) {buffer = bufferSource.getBuffer(renderType);}
            var cubes = b.getCubes();
            int packedOverlay = this.getPackedOverlay(animatable, 0.0F, Minecraft.getInstance().getPartialTick());
            for (var child_bones : b.getChildBones()) {
                cubes.addAll(child_bones.getCubes());
            }
            @Nullable VertexConsumer finalBuffer = buffer;
            cubes.forEach(geoCube -> {
                renderRecursively(poseStack, this.animatable, b, renderType, bufferSource, finalBuffer, false, Minecraft.getInstance().getPartialTick(), packedLight, packedOverlay, 1, 1, 1, 1);
            });
            poseStack.popPose();
        }

        public void setPlayer(Player e) {
            this.animatable.setPlayer(e);
        }

        public OriginFur(JsonObject json) {
            super(new OriginFurModel(json));
            this.animatable = new OriginFurAnimatable();
            // TODO was this commented in original????
//            this.addRenderLayer(new AutoGlowingGeoLayer<>(this) {
//                @Override
//                public GeoModel<OriginFurAnimatable> getGeoModel() {
//                    return OriginFur.this.getGeoModel();
//                }
//
//                @Override
//                protected RenderLayer getRenderType(OriginFurAnimatable animatable) {
//                    return RenderLayer.getEntityCutout(getTextureResource(animatable));
//                }
//
//                @Override
//                protected ResourceLocation getTextureResource(OriginFurAnimatable animatable) {
//                    OriginFurModel gM = (OriginFurModel) OriginFur.this.getGeoModel();
//                    return gM.getFullbrightTextureResource(animatable);
//                }
//            });
        }


    }
    public static boolean isRenderingInWorld = false;

    public static LinkedHashMap<ResourceLocation, OriginFur> FUR_REGISTRY = new LinkedHashMap<>();
    public static LinkedHashMap<ResourceLocation, Resource> FUR_RESOURCES = new LinkedHashMap<>();

    public static void reload(ResourceManager manager) {
        var resources = manager.listResources("furs", identifier -> identifier.getPath().endsWith(".json"));
        var namespaces = manager.getNamespaces();
        for (var res : resources.keySet()) {
            String itemName = res.toString().substring(res.toString().indexOf('/') + 1, res.toString().lastIndexOf('.'));
            ResourceLocation id = new ResourceLocation("origins", itemName);
            var p = itemName.split("\\.", 2);
            if (p.length > 1) {
                id = new ResourceLocation(p[0], p[1]);
            }
            assert id != null;
            id = new ResourceLocation(id.getNamespace(), id.getPath().replace('/', '.').replace('\\', '.'));
            if (!res.getNamespace().contentEquals("origin_visuals")) {
                FUR_REGISTRY.remove(id);
                FUR_RESOURCES.remove(id);
            }
            if (FUR_REGISTRY.containsKey(id)) {
                OriginFurModel m = (OriginFurModel) FUR_REGISTRY.get(id).getGeoModel();
                try {
                    m.recompile(JsonParser.parseString(new String(resources.get(res).open().readAllBytes())).getAsJsonObject());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                FUR_RESOURCES.put(id, resources.get(res));
            }
        }
        var entries = new ArrayList<>(OriginsAPI.getOriginsRegistry().entrySet());
        for (var entry : entries) {
            var oID = entry.getKey();
            ResourceLocation id = oID.location();
            var fur = OriginalFurClient.FUR_RESOURCES.getOrDefault(id, null);
            if (fur == null) {
                fur = OriginalFurClient.FUR_RESOURCES.getOrDefault(id, null);
            }
            if (fur == null) {
                OriginalFurClient.FUR_REGISTRY.put(id, new OriginFur(JsonParser.parseString("{}").getAsJsonObject()));
            } else {
                try {
                    OriginalFurClient.FUR_REGISTRY.put(id, new  OriginFur(JsonParser.parseString(new String(fur.open().readAllBytes())).getAsJsonObject()));
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public void onInitializeClient() {
        if (ModList.get().isLoaded("player-animator") || ModList.get().isLoaded("playeranimator")) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(new ResourceLocation("originfurs", "item_renderer"), 9999, ItemRendererFeatureAnim::new);
        }
//        WorldRenderEvents.END.register(context -> isRenderingInWorld = false);
//        WorldRenderEvents.START.register(context -> isRenderingInWorld = true);
//        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
//            @Override
//            public ResourceLocation getFabricId() {
//                return new ResourceLocation("originalfur", "furs");
//            }
//
//            final String r_M = "\\/([A-Za-z0-9_.-]+)\\.json";
//        });
    }
}
