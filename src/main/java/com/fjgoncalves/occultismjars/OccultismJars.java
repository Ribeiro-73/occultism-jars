package com.fjgoncalves.occultismjars;

import com.fjgoncalves.occultismjars.block.HolographicBaseBlock;
import com.fjgoncalves.occultismjars.block.SpiritFireChamberBlock;
import com.fjgoncalves.occultismjars.block.SpiritJarBlock;
import com.fjgoncalves.occultismjars.blockentity.HolographicBaseBlockEntity;
import com.fjgoncalves.occultismjars.blockentity.SpiritFireChamberBlockEntity;
import com.fjgoncalves.occultismjars.blockentity.SpiritJarBlockEntity;
import com.fjgoncalves.occultismjars.item.HolographicBaseItem;
import com.fjgoncalves.occultismjars.item.SpiritFireChamberItem;
import com.fjgoncalves.occultismjars.item.SpiritJarItem;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.common.item.DummyTooltipItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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

    public static final DeferredBlock<SpiritJarBlock> SPIRIT_JAR = BLOCKS.registerBlock(
            "spirit_jar",
            SpiritJarBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion());

    public static final DeferredItem<SpiritJarItem> SPIRIT_JAR_ITEM =
            ITEMS.registerItem("spirit_jar", props -> new SpiritJarItem(SPIRIT_JAR.get(), props),
                    Item.Properties::useBlockDescriptionPrefix);

    public static final DeferredItem<DummyTooltipItem> RITUAL_DUMMY_CRAFT_SPIRIT_JAR =
            ITEMS.registerItem("ritual_dummy/craft_spirit_jar", DummyTooltipItem::new);

    public static final DeferredBlock<HolographicBaseBlock> HOLOGRAPHIC_BASE = BLOCKS.registerBlock(
            "holographic_base",
            HolographicBaseBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredItem<HolographicBaseItem> HOLOGRAPHIC_BASE_ITEM =
            ITEMS.registerItem("holographic_base", props -> new HolographicBaseItem(HOLOGRAPHIC_BASE.get(), props),
                    Item.Properties::useBlockDescriptionPrefix);

    public static final DeferredItem<DummyTooltipItem> RITUAL_DUMMY_CRAFT_HOLOGRAPHIC_BASE =
            ITEMS.registerItem("ritual_dummy/craft_holographic_base", DummyTooltipItem::new);

    public static final DeferredBlock<SpiritFireChamberBlock> SPIRIT_FIRE_CHAMBER = BLOCKS.registerBlock(
            "spirit_fire_chamber",
            SpiritFireChamberBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> SpiritFireChamberBlock.isLit(state) ? 12 : 0));

    public static final DeferredItem<SpiritFireChamberItem> SPIRIT_FIRE_CHAMBER_ITEM =
            ITEMS.registerItem("spirit_fire_chamber", props -> new SpiritFireChamberItem(SPIRIT_FIRE_CHAMBER.get(), props),
                    Item.Properties::useBlockDescriptionPrefix);

    public static final DeferredItem<DummyTooltipItem> RITUAL_DUMMY_CRAFT_SPIRIT_FIRE_CHAMBER =
            ITEMS.registerItem("ritual_dummy/craft_spirit_fire_chamber", DummyTooltipItem::new);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiritJarBlockEntity>> SPIRIT_JAR_BE =
            BLOCK_ENTITIES.register("spirit_jar",
                    () -> new BlockEntityType<>(SpiritJarBlockEntity::new, SPIRIT_JAR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HolographicBaseBlockEntity>> HOLOGRAPHIC_BASE_BE =
            BLOCK_ENTITIES.register("holographic_base",
                    () -> new BlockEntityType<>(HolographicBaseBlockEntity::new, HOLOGRAPHIC_BASE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpiritFireChamberBlockEntity>> SPIRIT_FIRE_CHAMBER_BE =
            BLOCK_ENTITIES.register("spirit_fire_chamber",
                    () -> new BlockEntityType<>(SpiritFireChamberBlockEntity::new, SPIRIT_FIRE_CHAMBER.get()));

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SpiritWorkerMenu>> SPIRIT_WORKER_MENU =
            MENUS.register("spirit_worker", () -> IMenuTypeExtension.create(SpiritWorkerMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(SPIRIT_JAR_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(SPIRIT_JAR_ITEM.get());
                        output.accept(HOLOGRAPHIC_BASE_ITEM.get());
                        output.accept(SPIRIT_FIRE_CHAMBER_ITEM.get());
                    })
                    .build());

    public OccultismJars(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
