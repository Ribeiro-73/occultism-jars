package com.fjgoncalves.occultismjars.integration;

import java.util.List;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.SpiritWorkerBlockEntity;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.CrystallizeRecipe;
import com.klikli_dev.occultism.crafting.recipe.SpiritFireRecipe;
import com.klikli_dev.occultism.crafting.recipe.SpiritTradeRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JeiCompat implements IModPlugin {

    // same uid + class as Occultism's own categories, so JEI treats them as the same
    private static final IRecipeType<RecipeHolder<CrushingRecipe>> OCCULTISM_CRUSHING = occultism("crushing");
    private static final IRecipeType<RecipeHolder<CrystallizeRecipe>> OCCULTISM_CRYSTALLIZE = occultism("crystallize");
    private static final IRecipeType<RecipeHolder<SpiritTradeRecipe>> OCCULTISM_SPIRIT_TRADE = occultism("spirit_trade");
    private static final IRecipeType<RecipeHolder<SpiritFireRecipe>> OCCULTISM_SPIRIT_FIRE = occultism("spirit_fire");

    // shown as catalysts; the cooking ones are left out so the blocks don't read as furnaces
    private static final List<IRecipeType<?>> CATALYST_JOBS = List.of(OCCULTISM_CRUSHING, OCCULTISM_CRYSTALLIZE);

    // everything a working spirit can do, for the + button
    private static final List<IRecipeType<?>> SPIRIT_JOBS = List.of(
            OCCULTISM_CRUSHING,
            OCCULTISM_CRYSTALLIZE,
            OCCULTISM_SPIRIT_TRADE,
            RecipeTypes.SMELTING,
            RecipeTypes.BLASTING,
            RecipeTypes.SMOKING,
            RecipeTypes.CAMPFIRE_COOKING);

    @SuppressWarnings("unchecked")
    private static <R extends Recipe<?>> IRecipeType<RecipeHolder<R>> occultism(String name) {
        Class<? extends RecipeHolder<R>> holder = (Class<? extends RecipeHolder<R>>) (Object) RecipeHolder.class;
        return IRecipeType.create(Identifier.fromNamespaceAndPath("occultism", name), holder);
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(OccultismJars.MODID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (IRecipeType<?> type : CATALYST_JOBS) {
            registration.addCraftingStation(type, OccultismJars.SPIRIT_JAR_ITEM.get(), OccultismJars.HOLOGRAPHIC_BASE_ITEM.get());
        }
        // traders have no tiers, so only the base can run them
        registration.addCraftingStation(OCCULTISM_SPIRIT_TRADE, OccultismJars.HOLOGRAPHIC_BASE_ITEM.get());
        registration.addCraftingStation(OCCULTISM_SPIRIT_FIRE, OccultismJars.SPIRIT_FIRE_CHAMBER_ITEM.get());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        for (IRecipeType<?> type : SPIRIT_JOBS) {
            addTransfer(registration, type);
        }
    }

    // recipe input -> slot 0, pull from the player inventory range
    private static <R> void addTransfer(IRecipeTransferRegistration registration, IRecipeType<R> type) {
        registration.addRecipeTransferHandler(SpiritWorkerMenu.class, OccultismJars.SPIRIT_WORKER_MENU.get(),
                type, SpiritWorkerBlockEntity.INPUT_SLOT, 1, SpiritWorkerBlockEntity.SLOT_COUNT, 36);
    }
}
