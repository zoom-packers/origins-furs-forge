package com.pandaismyname1.origin_visuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pandaismyname1.origin_visuals.client.FurRenderFeature;
import com.pandaismyname1.origin_visuals.client.IMojModelPart;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OriginFurModel extends GeoModel<OriginFurAnimatable> {
    public static final OriginFurModel NULL_OR_EMPTY_MODEL = new OriginFurModel(JsonParser.parseString("{}").getAsJsonObject());
    Player entity;
    public JsonObject json;
    public OriginFurModel(JsonObject json) {
        this.recompile(json);
    }

    public Vec3 getPositionForBone(String bone) {
        var b = getCachedGeoBone(bone);
        if (b == null) { return Vec3.ZERO;}
        var pos = b.getLocalPosition();
        return new Vec3((float) pos.x, (float) pos.y, (float) pos.z);
    }
    public Vec3 getPositionForBoneD(String bone) {
        var b = getCachedGeoBone(bone);
        if (b == null) { return Vec3.ZERO;}
        var pos = b.getModelPosition();
        return new Vec3((float) pos.x, (float) pos.y, (float) pos.z);
    }

    public GeoBone setWorldPositionForBone(String bone_name, Vec3 dTransform) {
        return setWorldPositionForBone(bone_name, new Vec3(dTransform.x(), dTransform.y(), dTransform.z()));
    }
    public enum VMP {
        leftArm, rightArm, rightSleeve, leftSleeve, rightLeg, leftLeg, rightPants, leftPants, hat, head, body, jacket;

        public static final EnumSet<VMP> ALL_OPTS = EnumSet.allOf(VMP.class);
    }
    public EnumSet<VMP> hiddenParts = EnumSet.noneOf(VMP.class);
    public EnumSet<VMP> getHiddenParts() {
        return hiddenParts;
    }
    private boolean dirty = false;
    public void markDirty() {
        dirty = true;
    }
    public void preprocess(Origin origin, PlayerRenderer playerRenderer, IPlayerMixins playerEntity, ModelRootAccessor model, AbstractClientPlayer abstractClientPlayerEntity) {
        getAnimationProcessor().getRegisteredBones().forEach(coreGeoBone -> {
            coreGeoBone.setHidden(false);
            coreGeoBone.setHidden(coreGeoBone.getName().endsWith("thin_only") && !model.originalFur$isSlim());
            if (coreGeoBone.isHidden()) {
                return;
            }
            coreGeoBone.setHidden(coreGeoBone.getName().endsWith("wide_only") && model.originalFur$isSlim());
            if (coreGeoBone.isHidden()) {return;}
            // TODO PANDA - Check if the player is wearing the elytra
//            coreGeoBone.setHidden(coreGeoBone.getName().contains("elytra_hides") &&
//                    (origin.hasPowerType(PowerTypeRegistry.get(new ResourceLocation("origins:elytra")))
//                            || abstractClientPlayerEntity.getItemBySlot(EquipmentSlot.CHEST).isOf(Items.ELYTRA)));
            if (coreGeoBone.isHidden()) {return;}
            coreGeoBone.setHidden(coreGeoBone.getName().contains("helmet_hides") && !abstractClientPlayerEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
            if (coreGeoBone.isHidden()) {return;}
            coreGeoBone.setHidden(coreGeoBone.getName().contains("chestplate_hides") && !abstractClientPlayerEntity.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
            if (coreGeoBone.isHidden()) {return;}
            coreGeoBone.setHidden(coreGeoBone.getName().contains("leggings_hides") && !abstractClientPlayerEntity.getItemBySlot(EquipmentSlot.LEGS).isEmpty());
            if (coreGeoBone.isHidden()) {return;}
            coreGeoBone.setHidden(coreGeoBone.getName().contains("boots_hides") && !abstractClientPlayerEntity.getItemBySlot(EquipmentSlot.FEET).isEmpty());
            if (coreGeoBone.isHidden()) {return;}
        });
    }
    public void preRender$mixinOnly(Player player) {
        this.entity = player;
        this.currentOverride = this.getPredicateResources(player);
    }
    @SuppressWarnings("SpellCheckingInspection")
    public void parseHiddenParts() {
        var set = EnumSet.noneOf(VMP.class);
        if (json.has("hidden")) {
            var h = json.getAsJsonArray("hidden");
            h.forEach(jsonElement -> {
                switch (jsonElement.getAsString().toLowerCase()) {
                    case "rightsleeve"-> set.add(VMP.rightSleeve);
                    case "leftsleeve"-> set.add(VMP.leftSleeve);
                    case "rightarm"-> set.add(VMP.rightArm);
                    case "leftarm"-> set.add(VMP.leftArm);
                    case "leftpant", "leftpants" -> set.add(VMP.leftPants);
                    case "rightpant", "rightpants" -> set.add(VMP.rightPants);
                    case "rightleg" -> set.add(VMP.rightLeg);
                    case "leftleg" -> set.add(VMP.leftLeg);
                    case "hat", "hair" -> set.add(VMP.hat);
                    case "head" -> set.add(VMP.head);
                    case "body", "torso" -> set.add(VMP.body);
                    case "jacket" -> set.add(VMP.jacket);
                }
            });
        }
        hiddenParts.clear();
        hiddenParts.addAll(set);
    }
    public void recompile(JsonObject json) {
        this.json = json;
        hiddenParts.clear();
        parseHiddenParts();
        boneCache.clear();
//        AzureLibCache.getBakedModels().remove(this.getModelResource(null));
        // Force cache this model! This is so getCachedGeoModel will not throw an exception unless the bone doesn't exist!
//        var bM = AzureLibCache.getBakedModels().put(this.getModelResource(null), this.getBakedModel(this.getModelResource(null)));
//        assert bM != null;
        if (this.json.has("overrides") && this.json.get("overrides").isJsonArray()) {
            GsonHelper.getAsJsonArray(this.json,"overrides").forEach(jsonElement -> {
                var o = ResourceOverride.deserialize(jsonElement.getAsJsonObject());
                overrides.add(o);
            });
            overrides.sort((o1, o2) -> Float.compare(o1.weight, o2.weight));
        }

    }
    public boolean hasRenderingOffsets() {
        return json.has("rendering_offsets");
    }
    public boolean hasSubRenderingOffset(String id) {
        return hasRenderingOffsets() && json.getAsJsonObject("rendering_offsets").has(id);
    }
    public final Vec3 getRightOffset() {
        if (!hasSubRenderingOffset("right")) {
            return Vec3.ZERO;
        }
        return alib.VectorFromJson(json.getAsJsonObject("rendering_offsets").get("right"));
    }
    public final ResourceLocation getOverlayTexture(boolean slim) {
        if (!slim || !json.has("overlay_slim")) {
            if (json.has("overlay")) {
                return ResourceLocation.tryParse(GsonHelper.getAsString(json, "overlay"));
            }
        } else {
            if (json.has("overlay_slim")) {
                return ResourceLocation.tryParse(GsonHelper.getAsString(json, "overlay_slim"));
            }
        }
        return null;
    }
    public final ResourceLocation getEmissiveTexture(boolean slim) {
        if (!slim || !json.has("emissive_overlay_slim")) {
            if (json.has("emissive_overlay")) {
                return ResourceLocation.tryParse(GsonHelper.getAsString(json, "emissive_overlay"));
            }
        } else {

            if (json.has("emissive_overlay_slim")) {
                return ResourceLocation.tryParse(GsonHelper.getAsString(json, "emissive_overlay_slim"));
            }
        }
        return null;
    }
    public final Vec3 getLeftOffset() {
        if (!hasSubRenderingOffset("left")) {
            return Vec3.ZERO;
        }
        return alib.VectorFromJson(json.getAsJsonObject("rendering_offsets").get("left"));
    }
    public final boolean isPlayerModelInvisible() {return GsonHelper.getAsBoolean(json, "playerInvisible", false);}
    protected final LinkedHashMap<String,Stack<Vec3>> posStack = new LinkedHashMap<>();
    protected final LinkedHashMap<String,Stack<Vec3>> rotStack = new LinkedHashMap<>();
    protected final LinkedHashMap<String,Stack<Vec3>> sclStack = new LinkedHashMap<>();
    public final void pushPos(String bone_name, Vec3 pos) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        getStackFor(bone_name, posStack).push(new Vec3(bone.getPosX(), bone.getPosY(), bone.getPosZ()));
        bone.setPosX((float) pos.x);
        bone.setPosY((float) pos.y);
        bone.setPosZ((float) pos.z);
    }
    public final void pushRot(String bone_name, Vec3 rot) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        getStackFor(bone_name, rotStack).push(new Vec3(bone.getRotX(), bone.getRotY(), bone.getRotZ()));
        bone.setRotX((float) rot.x);
        bone.setRotY((float) rot.y);
        bone.setRotZ((float) rot.z);
    }
    public final void pushScl(String bone_name, Vec3 scale) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        getStackFor(bone_name, sclStack).push(new Vec3(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()));
        bone.setScaleX((float) scale.x);
        bone.setScaleY((float) scale.y);
        bone.setScaleZ((float) scale.z);
    }
    public final void popPos(String bone_name) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        var stk = getStackFor(bone_name, posStack);
        if (stk.isEmpty()) {return;}
        var pos = stk.pop();
        bone.setModelPosition(new Vector3d(pos.x, pos.y, pos.z));

    }
    public final void popRot(String bone_name) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        var stk = getStackFor(bone_name, rotStack);
        if (stk.isEmpty()) {return;}
        var pos = stk.pop();
        bone.setRotX((float) pos.x);
        bone.setRotY((float) pos.y);
        bone.setRotZ((float) pos.z);
    }
    public final void popScl(String bone_name) {
        var bone = this.getCachedGeoBone(bone_name);
        if (bone == null) {return;}
        var stk = getStackFor(bone_name, sclStack);
        if (stk.isEmpty()) {return;}
        var pos = stk.pop();
        bone.setScaleX((float) pos.x);
        bone.setScaleY((float) pos.y);
        bone.setScaleZ((float) pos.z);
    }
    public final GeoBone getCachedGeoBone(String bone_name) {
        long hash = alib.getHash64(bone_name);
        if (boneCache.containsKey(hash)) {
            return boneCache.get(hash);
        }
        GeoBone b = this.getBone(bone_name).orElse(null);
        if (b == null) {
            return null;
        }
        boneCache.putAndMoveToFirst(hash, b);
        return b;
    }
    private Stack<Vec3> getStackFor(String bone_name, LinkedHashMap<String, Stack<Vec3>> stackMap) {
        if (!stackMap.containsKey(bone_name)) {
            posStack.put(bone_name, new Stack<>());
        }
        return posStack.get(bone_name);
    }
    public Long2ReferenceLinkedOpenHashMap<GeoBone> boneCache =  new Long2ReferenceLinkedOpenHashMap<>();
    public final GeoBone resetBone(String bone_name) {
        setPositionForBone(bone_name, new Vec3(0,0,0));
        setRotationForBone(bone_name, new Vec3(0,0,0));
        setModelPositionForBone(bone_name, Vec3.ZERO);
        return setScaleForBone(bone_name, new Vec3(1,1,1));
    }

    public final GeoBone setPositionForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
//        b.setModelPosition(new Vector3d(pos.x, pos.y, pos.z));
        b.setPosX((float)pos.x);
        b.setPosY((float)pos.y);
        b.setPosZ((float)pos.z);
        return (GeoBone) b;
    }
    public final GeoBone setPositionForBone(GeoBone b, Vec3 pos) {
        if (b == null) {
            return null;
        }
//        b.setModelPosition(new Vector3d(pos.x, pos.y, pos.z));
        b.setPosX((float)pos.x);
        b.setPosY((float)pos.y);
        b.setPosZ((float)pos.z);
        return (GeoBone) b;
    }
    public final <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
    void renderBone(String bone_name, ModelPart part, PoseStack PoseStack, MultiBufferSource MultiBufferSource, int light,
                    RenderType layer, OriginalFurClient.OriginFur geoModel) {
        Vec3 pos = ((IMojModelPart)(Object)part).originfurs$getPosition().scale(-1/16f);
        Vec3 rot = ((IMojModelPart)(Object)part).originfurs$getRotation();
        setPositionForBone(bone_name, pos);
        setRotationForBone(bone_name, rot);
        geoModel.renderBone(bone_name, PoseStack, MultiBufferSource, layer, null, light);
    }
    public final <T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
    void renderBone(String bone_name, ModelPart part, PoseStack PoseStack, MultiBufferSource MultiBufferSource, int light,
                    RenderType layer, OriginalFurClient.OriginFur geoModel, Vec3 pos, Vec3 rot) {
        setPositionForBone(bone_name, pos);
        setRotationForBone(bone_name, rot);
        geoModel.renderBone(bone_name, PoseStack, MultiBufferSource, layer, null, light);
    }
    public final GeoBone setPositionForBone(String bone_name, Vec3 pos, float div) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setModelPosition(new Vector3d(pos.x / div, pos.y / div, pos.z / div));
//        b.setPosX((float)pos.x);
//        b.setPosY((float)pos.y);
//        b.setPosZ((float)pos.z);
        return (GeoBone) b;
    }
    public final GeoBone setPivotForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setPivotX((float)pos.x);
        b.setPivotY((float)pos.y);
        b.setPivotZ((float)pos.z);
        return (GeoBone) b;
    }
    public final GeoBone setModelPositionForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setModelPosition(new Vector3d(pos.x, pos.y, pos.z));
        return (GeoBone) b;
    }
    public final GeoBone translatePositionForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        var posOut = new Vec3(pos.x + b.getPosX(), (float)pos.y + b.getPosY(),(float)pos.z + b.getPosZ());
        return this.setPositionForBone(bone_name, posOut);
    }
    public final GeoBone translateModelPositionForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        Vector3d o = new Vector3d(pos.x + b.getPosX(), pos.z + b.getPosZ(), pos.z + b.getPosZ());
        b.setModelPosition(o);
        return (GeoBone) b;
    }
    public final GeoBone translateRotationForBone(String bone_name, Vec3 pos) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setRotX((float)pos.x + b.getRotX());
        b.setRotY((float)pos.y + b.getRotY());
        b.setRotZ((float)pos.z + b.getRotZ());
        return (GeoBone) b;
    }
    public final GeoBone setRotationForBone(String bone_name, Vec3 rot) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setRotX((float)rot.x);
        b.setRotY((float)rot.y);
        b.setRotZ((float)rot.z);
        return (GeoBone) b;
    }
    public final GeoBone setRotationForBone(String bone_name, Vec3 rot, boolean iX, boolean iY, boolean iZ) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setRotX((float)rot.x * (iX ? -1 : 1));
        b.setRotY((float)rot.y * (iY ? -1 : 1));
        b.setRotZ((float)rot.z * (iZ ? -1 : 1));
        return (GeoBone) b;
    }
    public final GeoBone setScaleForBone(String bone_name, Vec3 scale) {
        var b = this.getCachedGeoBone(bone_name);
        if (b == null) {
            return null;
        }
        b.setScaleX((float)scale.x);
        b.setScaleY((float)scale.y);
        b.setScaleZ((float)scale.z);
        return (GeoBone) b;
    }
    public final GeoBone setTransformationForBone(String bone_name, Vec3 pos, Vec3 scale, Vec3 eulers){
        setModelPositionForBone(bone_name, pos);
        setRotationForBone(bone_name, eulers);
        return setScaleForBone(bone_name,scale);

    }
    public final GeoBone copyFromMojangModelPart(String bone_name, ModelPart part) {
        Vec3 pos = new Vec3(part.x, part.y, part.z);
        Vec3 scale = new Vec3(part.xScale, part.yScale, part.zScale);
        Vec3 rott = new Vec3(-part.x, -part.y, -part.z);
        return setTransformationForBone(bone_name, pos, scale, rott);
    }
    public final GeoBone copyFromModelTransformation(String bone_name, FurRenderFeature.ModelTransformation part) {
        return setTransformationForBone(bone_name, part.position, new Vec3(1,1,1), part.rotation);
    }
    public final GeoBone copyFromMojangModelPart(String bone_name, ModelPart part, boolean inverted) {
        Vec3 pos = new Vec3(part.x, part.y, part.z);
        Vec3 scale = new Vec3(part.xScale, part.yScale, part.zScale);
        Vec3 rott = new Vec3(-part.x, -part.y, -part.z);
        if (inverted) {
            rott = new Vec3(part.x, part.y, part.z);
        }
        return setTransformationForBone(bone_name, pos, scale, rott);
    }
    public final GeoBone copyRotFromMojangModelPart(String bone_name, ModelPart part, boolean inverted) {
        Vec3 rott = new Vec3(-part.x, -part.y, -part.z);
        if (inverted) {
            rott = new Vec3(part.x, part.y, part.z);
        }
        return setRotationForBone(bone_name,rott);
    }
    public final GeoBone copyRotFromMojangModelPart(String bone_name, ModelPart part, boolean invertedX, boolean invertedY, boolean invertedZ) {
        Vec3 rott = new Vec3(-part.getInitialPose().x, -part.getInitialPose().y, -part.getInitialPose().z);
        return setRotationForBone(bone_name,rott, invertedX, invertedY, invertedZ);
    }
    public final GeoBone invertRotForPart(String bone_name, boolean x, boolean y, boolean z) {
        var b = getCachedGeoBone(bone_name);
        if (b == null) {return null;}
        var r =b.getRotationVector().mul(x ? -1 : 1, y ? -1 : 1, z ? -1 : 1);
        b.setRotX((float) r.x);
        b.setRotY((float) r.y);
        b.setRotZ((float) r.z);
        return b;
    }
    public final GeoBone copyRotFromMojangModelPart(String bone_name, ModelPart part) {
        Vec3 rott = new Vec3(-part.x, -part.y, -part.z);
        return setRotationForBone(bone_name,rott);
    }
    public final GeoBone copyPosFromMojangModelPart(String bone_name, ModelPart part) {
        var t = part.getInitialPose();
        Vec3 rott = new Vec3(t.x / 16.0f, t.y / 16.0f, t.z / 16.0f);
        return setWorldPositionForBone(bone_name,rott);

    }
    public final GeoBone copyPivotFromMojangModelPart(String bone_name, ModelPart part) {
        var t = part.getInitialPose();
        Vec3 rott = new Vec3(t.x, t.y, t.z);
        return setPivotForBone(bone_name,rott);

    }
    public final GeoBone copyScaleFromMojangModelPart(String bone_name, ModelPart part) {
        Vec3 rott = new Vec3(part.xScale, part.yScale, part.zScale);
        return setPositionForBone(bone_name,rott);
    }
    //                public GeoModel setTransformationForBone(String bone_name, Vec3 pos, Vec3 scale, Quaterniond quaternion){}
    private static class ResourceOverride {
        public Tag requirements;
        public ResourceLocation textureResource = ResourceLocation.tryParse("origin_visuals:textures/missing.png");
        public ResourceLocation modelResource = ResourceLocation.tryParse("origin_visuals:geo/missing.geo.json");
        public ResourceLocation animationResource = ResourceLocation.tryParse("origin_visuals:animations/missing.animation.json");
        public float weight;
        private static ResourceOverride deserializeBase(JsonObject object, ResourceOverride r) {
            r.requirements = alib.json2NBT(object.get("condition"));
            r.weight = GsonHelper.getAsFloat(object, "weight", 1f);
            return r;
        }
        public static ResourceOverride deserialize(JsonObject object) {
            var r = deserializeBase(object, new ResourceOverride());
            r.textureResource = ResourceLocation.tryParse(GsonHelper.getAsString(object, "texture", "origin_visuals:textures/missing.png"));
            r.modelResource = ResourceLocation.tryParse(GsonHelper.getAsString(object, "model", "origin_visuals:geo/missing.geo.json"));
            r.animationResource = ResourceLocation.tryParse(GsonHelper.getAsString(object, "animation", "origin_visuals:animations/missing.animation.json"));
            return r;
        }
    }
    public List<ResourceOverride> overrides = new ArrayList<>();
    ResourceLocation getTextureResource_P(Player entity) {
        AtomicReference<ResourceLocation> override = new AtomicReference<>();
        if (!overrides.isEmpty()) {
            AtomicBoolean _continue = new AtomicBoolean(true);
            var nbt = entity.saveWithoutId(new CompoundTag());
            entity.addAdditionalSaveData(nbt);
            overrides.forEach(m_override -> {
                if (!_continue.get()){return;}
                if (alib.checkNBTEquals(m_override.requirements, nbt)){
                    _continue.set(false);
                    override.set(m_override.textureResource);
                }
            });
        }
        return override.get();
    }
    ResourceLocation getModelResource_P(Player entity) {
        AtomicReference<ResourceLocation> override = new AtomicReference<>();
        if (!overrides.isEmpty()) {
            AtomicBoolean _continue = new AtomicBoolean(true);
            var nbt = entity.saveWithoutId(new CompoundTag());
            entity.addAdditionalSaveData(nbt);
            overrides.forEach(m_override -> {
                if (!_continue.get()){return;}
                if (alib.checkNBTEquals(m_override.requirements, nbt)){
                    _continue.set(false);
                    override.set(m_override.modelResource);
                }
            });
        }
        return override.get();
    }
    ResourceLocation getAnimationResource_P(Player entity) {
        AtomicReference<ResourceLocation> override = new AtomicReference<>();
        if (!overrides.isEmpty()) {
            AtomicBoolean _continue = new AtomicBoolean(true);
            var nbt = entity.saveWithoutId(new CompoundTag());
            entity.addAdditionalSaveData(nbt);
            overrides.forEach(m_override -> {
                if (!_continue.get()){return;}
                if (alib.checkNBTEquals(m_override.requirements, nbt)){
                    _continue.set(false);
                    override.set(m_override.animationResource);
                }
            });
        }
        return override.get();
    }
    ResourceLocation mRLast = null;
    public ResourceOverride getPredicateResources(Player entity){
//        var mR = getModelResource_P(entity);
//        var tR = getTextureResource_P(entity);
//        var aR = getAnimationResource_P(entity);
//        if (mRLast != mR) {
//            boneCache.clear();
//            for (var bone : this.getAnimationProcessor().getRegisteredBones()) {
//                System.out.println(bone.getName());
//                boneCache.put(alib.getHash64(bone.getName()), (GeoBone) bone);
//            }
//            mRLast = mR;
//        }
//        currentOverride.modelResource = mR != null && mR.compareTo(dMR(json)) != 0 ? mR : dMR(json);
//        currentOverride.textureResource = tR != null && tR.compareTo(dTR(json)) != 0 ? tR : dTR(json);
//        currentOverride.animationResource = aR != null && aR.compareTo(dAR(json)) != 0 ? aR : dAR(json);

//
        return currentOverride;
    }
    ResourceOverride currentOverride = new ResourceOverride();
    public static ResourceLocation dMR(JsonObject json) {
        return ResourceLocation.tryParse(GsonHelper.getAsString(json, "model", "origin_visuals:geo/missing.geo.json"));
    }
    public static ResourceLocation dTR(JsonObject json) {
        return ResourceLocation.tryParse(GsonHelper.getAsString(json, "texture", "origin_visuals:textures/missing.png"));
    }
    public static ResourceLocation dAR(JsonObject json) {
        return ResourceLocation.tryParse(GsonHelper.getAsString(json, "animation", "origin_visuals:animations/missing.animation.json"));
    }
    @Override
    public ResourceLocation getModelResource(OriginFurAnimatable geoAnimatable) {
        return dMR(json);
    }
    @Override
    public ResourceLocation getTextureResource(OriginFurAnimatable geoAnimatable) {
        return dTR(json);
    }
    @Override
    public ResourceLocation getAnimationResource(OriginFurAnimatable geoAnimatable) {
        return dAR(json);
    }
    public ResourceLocation getFullbrightTextureResource(OriginFurAnimatable geoAnimatable) {
        var id = ResourceLocation.tryParse(GsonHelper.getAsString(json, "fullbrightTexture", "origin_visuals:textures/missing.png"));
        return id;
    }
    public boolean hasCustomElytraTexture() {
        return json.has("elytraTexture");
    }
    public ResourceLocation getElytraTexture() {
        return ResourceLocation.tryParse(GsonHelper.getAsString(json, "elytraTexture", "textures/entity/elytra.png"));
    }
    public ResourceLocation getHurtSoundResource() {
        return ResourceLocation.tryParse(GsonHelper.getAsString(json, "hurtSound", "null"));
    }
}
