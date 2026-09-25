package com.fjgoncalves.occultismjars.integration;

import java.util.List;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.SpiritWorkerBlockEntity;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.CrystallizeRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JeiCompat implements IModPlugin {

    // same uid + class as Occultism's own categories, so JEI treats them as the same
    private static final RecipeType<RecipeHolder<CrushingRecipe>> OCCULTISM_CRUSHING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("occultism", "crushing"));
    private static final RecipeType<RecipeHolder<CrystallizeRecipe>> OCCULTISM_CRYSTALLIZE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("occultism", "crystallize"));

    // shown as catalysts; the cooking ones are left out so the blocks don't read as furnaces
    private static final List<RecipeType<?>> CATALYST_JOBS = List.of(OCCULTISM_CRUSHING, OCCULTISM_CRYSTALLIZE);

    // everything a crusher, smelter or crystallizer spirit can do, for the + button
    private static final List<RecipeType<?>> SPIRIT_JOBS = List.of(
            OCCULTISM_CRUSHING,
            OCCULTISM_CRYSTALLIZE,
            RecipeTypes.SMELTING,
            RecipeTypes.BLASTING,
            RecipeTypes.SMOKING,
            RecipeTypes.CAMPFIRE_COOKING);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(OccultismJars.MODID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RecipeType<?> type : CATALYST_JOBS) {
            registration.addRecipeCatalyst(new ItemStack(OccultismJars.SPIRIT_JAR_ITEM.get()), type);
            registration.addRecipeCatalyst(new ItemStack(OccultismJars.HOLOGRAPHIC_BASE_ITEM.get()), type);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        for (RecipeType<?> type : SPIRIT_JOBS) {
            addTransfer(registration, type);
        }
    }

    // recipe input -> slot 0, pull from the player inventory range
    private static <R> void addTransfer(IRecipeTransferRegistration registration, RecipeType<R> type) {
        registration.addRecipeTransferHandler(SpiritWorkerMenu.class, OccultismJars.SPIRIT_WORKER_MENU.get(),
                type, SpiritWorkerBlockEntity.INPUT_SLOT, 1, SpiritWorkerBlockEntity.SLOT_COUNT, 36);
    }
}
