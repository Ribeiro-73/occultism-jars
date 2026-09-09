package com.fjgoncalves.occultismjars;

import com.fjgoncalves.occultismjars.block.CrusherJarBlock;
import com.fjgoncalves.occultismjars.blockentity.CrusherJarBlockEntity;
import com.fjgoncalves.occultismjars.item.CrusherJarItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(OccultismJars.MODID)
public final class OccultismJars {

    public static final String MODID = "occultismjars";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<CrusherJarBlock> CRUSHER_JAR = BLOCKS.registerBlock(
            "crusher_jar",
            CrusherJarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion());

    public static final DeferredItem<CrusherJarItem> CRUSHER_JAR_ITEM =
            ITEMS.registerItem("crusher_jar", props -> new CrusherJarItem(CRUSHER_JAR.get(), props));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherJarBlockEntity>> CRUSHER_JAR_BE =
            BLOCK_ENTITIES.register("crusher_jar",
                    () -> BlockEntityType.Builder.of(CrusherJarBlockEntity::new, CRUSHER_JAR.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(CRUSHER_JAR_ITEM.get()))
                    .displayItems((params, output) -> output.accept(CRUSHER_JAR_ITEM.get()))
                    .build());

    public OccultismJars(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
