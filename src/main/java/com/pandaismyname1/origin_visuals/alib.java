package com.pandaismyname1.origin_visuals;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LazilyParsedNumber;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

@SuppressWarnings("unused")
public class alib {
    public static Vec3 VectorFromJson(JsonElement o) {
        double x = 0,y = 0,z = 0;
        if (o.isJsonArray()) {
            var a = o.getAsJsonArray();
            x = a.get(0).getAsDouble();
            y = a.get(1).getAsDouble();
            z = a.get(2).getAsDouble();
        } else if (o.isJsonObject()) {
            x = o.getAsJsonObject().get("x").getAsDouble();
            y = o.getAsJsonObject().get("y").getAsDouble();
            z = o.getAsJsonObject().get("z").getAsDouble();
        } else if (o.isJsonPrimitive() && o.getAsJsonPrimitive().isNumber()) {
            return longToVec3(o.getAsLong());
        }
        return new Vec3(x,y,z);
    }
    public static Vec3 longToVec3(long value) {
        // Extract x, y, and z components from the long value
        double x = (double) (value & 0xFFFF);
        double y = (double) ((value >> 16) & 0xFFFF);
        double z = (double) ((value >> 32) & 0xFFFF);

        // Return the Vec3 with the extracted components
        return new Vec3(x, y, z);
    }
    public static CompoundTag packBlockStateIntoCompound(BlockState state, CompoundTag tag) {
        for (Property<?> prop : state.getProperties()) {
            String name = prop.getName();
            Comparable<?> value = state.getValue(prop);
            if (value instanceof Number num) {
                if (num instanceof Float f) {
                    tag.putFloat(name, f);
                } else if (num instanceof Double d) {
                    tag.putDouble(name, d);
                } else if (num instanceof Long l) {
                    tag.putLong(name, l);
                } else if (num instanceof Integer i) {
                    tag.putInt(name, i);
                } else if (num instanceof Byte b) {
                    tag.putByte(name, b);
                } else if (num instanceof Short s) {
                    tag.putShort(name, s);
                }
            } else if (value instanceof Boolean bl) {
                tag.putBoolean(name, bl);
            } else if (value instanceof String str) {
                tag.putString(name, str);
            }
        }
        return tag;
    }

    // Checks if left is present and values equal in right.
    public static boolean checkNBTEquals(Tag leftE, Tag rightE) {
        boolean bl = true;
        if (leftE instanceof ListTag left && rightE instanceof  ListTag right) {
            if (left.size() == right.size() && left.size() == 0) {
                System.out.println("was empty");
                return true;
            }
            int i = 0;
            for (Tag e : left.subList(0, left.size())) {
                // It is assumed the values are of the same type- ListTags require this normally!
                if (!e.toString().contentEquals(right.get(i).toString())) {
                    System.out.println("content not equals");
                    return false;
                }
                i++;
            }
        }
        else if (leftE instanceof CompoundTag left && rightE instanceof CompoundTag right) {
            if (left.size() == right.size() && left.size() == 0) {
                System.out.println("was empty");
                return true;
            }
            for (String key : left.getAllKeys()) {
                if (!bl || !right.contains(key)) {
                    System.out.println("key missing");
                    return false;
                }
                Tag e_L = left.get(key);
                Tag e_R = right.get(key);
                int elem_t = left.getTagType(key);
                if (right.getTagType(key) != elem_t) {
                    System.out.println("type mismatch");
                    return false;
                }
                if (left.get(key) instanceof ListTag lL && right.get(key) instanceof ListTag rL) {
                    if (lL.size() != rL.size()) {
                        System.out.println("was empty");
                        return false;
                    }
                    int i = 0;
                    for (Tag e : lL.subList(0, lL.size())) {
                        if (!e.toString().contentEquals(rL.get(i).toString())) {
                            System.out.println("content not equals");
                            return false;
                        }
                        i++;
                    }
                }

                if (e_L instanceof NumericTag numL && e_R instanceof NumericTag numR) {
                    switch (elem_t) {
                        case Tag.TAG_INT -> bl = numL.getAsInt() == numR.getAsInt();
                        case Tag.TAG_SHORT -> bl = numL.getAsShort() == numR.getAsShort();
                        case Tag.TAG_BYTE -> bl = numL.getAsByte() == numR.getAsByte();
                        case Tag.TAG_FLOAT -> bl = numL.getAsFloat() == numR.getAsFloat();
                        case Tag.TAG_DOUBLE -> bl = numL.getAsDouble() == numR.getAsDouble();
                        case Tag.TAG_LONG -> bl = numL.getAsLong() == numR.getAsLong();
                    }
                }
                switch (elem_t) {
                    case Tag.TAG_COMPOUND -> bl = checkNBTEquals(left.getCompound(key), right.getCompound(key));
                    case Tag.TAG_STRING -> bl = e_L.getAsString().contentEquals(e_R.getAsString());
                }
            }
        }
        return bl;
    }

    public static Tag json2NBT(JsonElement jsonElement) {
        Tag tag = null;
        if (jsonElement.isJsonArray()) {
            tag = new ListTag();
            for (var _e : jsonElement.getAsJsonArray()) {

                if (_e.isJsonPrimitive()) {
                    JsonPrimitive primitive = _e.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        Number num = primitive.getAsNumber();
                        if (primitive.getAsNumber() instanceof LazilyParsedNumber lPN) {
                            // Sometimes, it won't parse properly.
                            num = parseGsonLazilyParsedNumber(lPN);
                        }
                        if (num instanceof Integer i) {
                            ((ListTag) tag).add(IntTag.valueOf(i));
                        } else if (num instanceof Float i) {
                            ((ListTag) tag).add(FloatTag.valueOf(i));
                        } else if (num instanceof Double i) {
                            ((ListTag) tag).add(DoubleTag.valueOf(i));
                        } else if (num instanceof Short i) {
                            ((ListTag) tag).add(ShortTag.valueOf(i));
                        }
                    } else if (primitive.isBoolean()) {
                        ((ListTag) tag).add(ByteTag.valueOf(primitive.getAsBoolean()));
                    } else if (primitive.isString()) {
                        ((ListTag) tag).add(StringTag.valueOf(primitive.getAsString()));
                    }
                    // Add more conversions as needed for other primitive types
                } else if (_e.isJsonObject() || _e.isJsonArray()) {
                    Tag nestedCompound = json2NBT(_e);
                    ((ListTag) tag).add(nestedCompound);
                }
            }
        } else if (jsonElement instanceof JsonObject jsonObject) {
            tag= new CompoundTag();
            for (var entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                JsonElement _e = entry.getValue();

                if (_e.isJsonPrimitive()) {
                    JsonPrimitive primitive = _e.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        Number num = primitive.getAsNumber();
                        if (primitive.getAsNumber() instanceof LazilyParsedNumber lPN) {
                            // Sometimes, it won't parse properly.
                            num = parseGsonLazilyParsedNumber(lPN);
                        }
                        if (num instanceof Integer i) {
                            ((CompoundTag) tag).putInt(key, i);
                        } else if (num instanceof Float i) {
                            ((CompoundTag) tag).putFloat(key, i);
                        } else if (num instanceof Double i) {
                            ((CompoundTag) tag).putDouble(key, i);
                        } else if (num instanceof Short i) {
                            ((CompoundTag) tag).putShort(key, i);
                        } else {
                            System.out.println("Found unexpected primitive: " + alib.getPrivateMixinField(primitive, "value").getClass().getTypeName());
                        }
                    } else if (primitive.isBoolean()) {
                        ((CompoundTag) tag).putBoolean(key, primitive.getAsBoolean());
                    } else if (primitive.isString()) {
                        ((CompoundTag) tag).putString(key, primitive.getAsString());
                    } else {
                        System.out.println("Found unexpected primitive: " + alib.getPrivateMixinField(primitive, "value").getClass().getTypeName());
                    }
                } else if (_e.isJsonObject() || _e.isJsonArray()) {
                    Tag nestedCompound = json2NBT(_e);
                    ((CompoundTag) tag).put(key, nestedCompound);
                }
            }
        }
        return tag;
    }
    private static Number parseGsonLazilyParsedNumber(LazilyParsedNumber number) {
        String value = getPrivateMixinField(number, "value");
        Number n = null;
        try {
            n = Integer.parseInt(value);
        } catch (Exception _0i) {
            try {
                n = Float.parseFloat(value);
            } catch (Exception _1f) {
                try {
                    n = Double.parseDouble(value);
                } catch (Exception _2d) {
                    try {
                        n = Short.parseShort(value);
                    } catch (Exception _3s) {
                        n = Long.parseLong(value);
                    }
                }
            }

        }
        return n;
    }
    public static <F,T> F getMixinField(T mixinType, String fieldName) {
        try {
            Field f = mixinType.getClass().getField(fieldName);
            //noinspection unchecked
            return (F) f.get(mixinType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static <F,T> F setMixinField(T mixinType, String fieldName, F value) {
        try {
            Field f = mixinType.getClass().getField(fieldName);
            f.set(mixinType, value);
            //noinspection unchecked
            return (F)f.get(mixinType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static <F,T> F setPrivateMixinField(T mixinType, String fieldName, F value) {
        try {
            Field f = mixinType.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);

            f.set(mixinType, value);
            //noinspection unchecked
            return (F)f.get(mixinType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static <F,T> F getPrivateMixinField(T mixinType, String fieldName) {
        try {
            Field f = mixinType.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            //noinspection unchecked
            return (F) f.get(mixinType);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable> T parseInputString(String input, String format) {
        assert format.startsWith("%");
        String result = String.format(input,format);
        return switch (format.charAt(format.length() - 1)) {
            case 'd' -> (T)(Object)Double.parseDouble(result);
            case 'f' -> (T)(Object)Float.parseFloat(result);
            case 'i' -> (T)(Object)Integer.parseInt(result);
            case 'b' -> (T)(Object)Boolean.parseBoolean(result);
            case 'o' -> (T)(Object)Integer.parseInt(result,3);
            // assume result is hex. Returns a string!!!
            case 'h' -> (T)result;
            default -> throw new IllegalStateException("Unexpected value: " + format.charAt(format.length() - 1));
        };
    }
    public static <T, R> R runMixinMethod(T mixinType, String methodName, Object ... args) {
        try {
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }
            Method f;
            if (args.length > 0) {
                f = mixinType.getClass().getMethod(methodName, argTypes);
            } else {
                f = mixinType.getClass().getMethod(methodName);
            }
            //noinspection unchecked
            return (R)f.invoke(mixinType, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, R> R runConstructor(Class mixinType, Object ... args) {
        try {
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }
            Constructor f;
            if (args.length > 0) {
                f = mixinType.getConstructor(argTypes);
            } else {
                f = mixinType.getConstructor();
            }
            //noinspection unchecked
            return (R)f.newInstance(args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, R> R runPrivateConstructor(Class mixinType, Object ... args) {
        try {
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
                System.out.println(argTypes[i]);
            }
            Constructor f;
            if (args.length > 0) {
                f = mixinType.getDeclaredConstructor(argTypes);
            } else {
                f = mixinType.getDeclaredConstructor();
            }
            f.setAccessible(true);
            return (R)f.newInstance(args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T, R> R runPrivateMixinMethod(T mixinType, String methodName, Object ... args) {
        try {
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }
            Method f;
            if (args.length > 0) {
                f = mixinType.getClass().getDeclaredMethod(methodName, argTypes);
            } else {
                f = mixinType.getClass().getDeclaredMethod(methodName);
            }
            f.setAccessible(true);
            //noinspection unchecked
            return (R)f.invoke(mixinType, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    public static float getRandomFloat(Random random, float minValue, float maxValue) {
        return minValue + random.nextFloat() * (maxValue - minValue);
    }
    public static <T extends Entity> List<T> getEntitiesOfTypeInRange(Level world, BlockPos pos, double range, EntityType<T> type) {
        Predicate<Entity> filter = entity -> entity.getType() == type && entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= range * range;
        var intRange = (int) range;
        var boundingBoxLower = pos.offset(-intRange, -intRange, -intRange);
        var boundingBoxUpper = pos.offset(intRange, intRange, intRange);
        var boundingBox = new AABB(boundingBoxLower, boundingBoxUpper);
        return world.getEntities(type, boundingBox, filter);
    }
    public static boolean isEntityNearBlock(Entity e, int radius, Block... blocks) {
        // Get the entity's position
        BlockPos entityPos = e.getOnPos();

        // Check if any nearby block is a target block type
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = entityPos.offset(x, y, z);
                    Block block = e.level().getBlockState(pos).getBlock();
                    for (Block b : blocks) {
                        System.out.println("check");
                        if (block == b) {
                            return true;
                        }
                    }
                }
            }
        }

        // No nearby block is a target block type
        return false;
    }
    /**
     * Calculate a 64 bits hash by combining CRC32 with Adler32.
     *
     * @param bytes a byte array
     * @return a hash number
     */
    public static long getHash64(byte[] bytes) {

        CRC32 crc32 = new CRC32();
        Adler32 adl32 = new Adler32();

        crc32.update(bytes);
        adl32.update(bytes);

        long crc = crc32.getValue();
        long adl = adl32.getValue();
        return ((crc << 32) | adl) + crc << 8;
    }
    public static boolean stackCustomModelDataEquals(@NotNull ItemStack stack, int data) {
        if (!stack.getOrCreateTag().contains("CustomModelData")) {return false;}
        return stack.getOrCreateTag().getInt("CustomModelData") == data;
    }
    public static long getHash64(String s) {
        return getHash64(s.getBytes(StandardCharsets.UTF_8));
    }
    public static long bitenable(long var, long nbit) {
        return (var) |= (1L <<(nbit));
    }
    public static long bitdisable(long var, long nbit) {
        return (var) &= (1L <<(nbit));
    }
    public static long bitflip(long var, long nbit) {
        return (var) ^= (1L <<(nbit));
    }
    public static boolean getbit(long var, long nbit) {
        return ((var) & (1L <<(nbit))) == 1;
    }
    public static boolean getbitmask(long var, long mask) {
        return (var & mask) == mask;
    }
    public static long setbit(long var, long nbit, boolean value) {
        if (value) {
            return bitenable(var, nbit);
        } else {
            return bitdisable(var,nbit);
        }
    }
    public static boolean isBlockIn(BlockState source, TagKey<Block> tag) {
        return source.is(tag);
    }
    public static BlockPos getBlockPosFromArray(long[] a) {
        if (a.length < 3) {return BlockPos.ZERO;}
        return new BlockPos((int)a[0], (int)a[1], (int)a[2]);
    }
    public static long[] getBlockPosAsArray(BlockPos d) {
        if (d == null) {return new long[]{0, 0, 0};}
        return new long[]{d.getX(), d.getY(), d.getZ()};
    }
    public static double[] getVec3AsArray(Vec3 d) {
        if (d == null) {return new double[]{0d, 0d, 0d};}
        return new double[]{d.x(), d.y(), d.z()};
    }
    public static Class<?> getFunctionTemplateClass(Object object, int index) {
        Type genericInterface = object.getClass().getGenericInterfaces()[0];
        if (genericInterface instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (index >= 0 && index < typeArguments.length) {
                return (Class<?>) typeArguments[index];
            }
        }
        throw new IllegalArgumentException("Index out of bounds or template not found.");
    }
    public static Pair<Integer, Integer> XYPosFromOffset(int w, int offset) {
        assert w != 0;
        int x;
        int y;
        x = offset % w;    // % is the "modulo operator", the remainder of i / width;
        assert offset != 0;
        y = offset / w;    // where "/" is an integer division
        return Pair.of(x,y);
    }
    public static double lerp(double a, double b, float f) {
        return a + f * (b - a);
    }
    public static int mixRGB (int a, int b, float ratio) {
        if (ratio > 1f) {
            ratio = 1f;
        } else if (ratio < 0f) {
            ratio = 0f;
        }
        float iRatio = 1.0f - ratio;

        int aA = (a >> 24 & 0xff);
        int aR = ((a & 0xff0000) >> 16);
        int aG = ((a & 0xff00) >> 8);
        int aB = (a & 0xff);

        int bA = (b >> 24 & 0xff);
        int bR = ((b & 0xff0000) >> 16);
        int bG = ((b & 0xff00) >> 8);
        int bB = (b & 0xff);

        int A = (int)((aA * iRatio) + (bA * ratio));
        int R = (int)((aR * iRatio) + (bR * ratio));
        int G = (int)((aG * iRatio) + (bG * ratio));
        int B = (int)((aB * iRatio) + (bB * ratio));

        return A << 24 | R << 16 | G << 8 | B;
    }
}
