package com.fjgoncalves.occultismjars.integration;

import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.blockentity.SpiritJarBlockEntity;
import com.fjgoncalves.occultismjars.menu.SpiritJarMenu;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JeiCompat implements IModPlugin {

    private static final RecipeType<RecipeHolder<CrushingRecipe>> OCCULTISM_CRUSHING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("occultism", "crushing"));

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(OccultismJars.MODID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(OccultismJars.SPIRIT_JAR_ITEM.get()), OCCULTISM_CRUSHING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // recipe input -> slot 0, pull from the player inventory range
        registration.addRecipeTransferHandler(SpiritJarMenu.class, OccultismJars.SPIRIT_JAR_MENU.get(),
                OCCULTISM_CRUSHING, SpiritJarBlockEntity.INPUT_SLOT, 1, SpiritJarBlockEntity.SLOT_COUNT, 36);
    }
}
