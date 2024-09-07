package com.pandaismyname1.origin_visuals;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.io.IOException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OriginVisuals.MODID)
public class OriginVisuals {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "origin_visuals";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Resource> FUR_RESOURCES = DeferredRegister.create(OriginalFurClient.FursDynamicRegistries.FUR_RESOURCES_REGISTRY, MODID);
    private static final DeferredRegister<OriginalFurClient.OriginFur> FURS = DeferredRegister.create(OriginalFurClient.FursDynamicRegistries.FURS_REGISTRY, MODID);


    public OriginVisuals() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in

        FURS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);


        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        FURS.register("default", () -> new OriginalFurClient.OriginFur(JsonParser.parseString("{}").getAsJsonObject()));
        LOGGER.info("FURS >> {}", FURS.getEntries());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        private static int ticks = 0;

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
//            OriginalFurClient.reload(Minecraft.getInstance().getResourceManager());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void reloadComplete(CalioDynamicRegistryEvent.LoadComplete event) {
            OriginalFurClient.reload(Minecraft.getInstance().getResourceManager());
        }

        @SubscribeEvent
        public static void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            OriginalFurClient.reload(Minecraft.getInstance().getResourceManager());
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event)
        {
            if (event.side.isClient()) {
                ticks++;
                if (ticks % 1000 == 0) {
                    OriginalFurClient.reload(Minecraft.getInstance().getResourceManager());
                    ticks = 0;
                }
            }
        }
    }
}
