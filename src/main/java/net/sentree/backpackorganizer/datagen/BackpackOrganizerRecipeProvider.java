package net.sentree.backpackorganizer.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.sentree.backpackorganizer.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class BackpackOrganizerRecipeProvider extends FabricRecipeProvider {
    public BackpackOrganizerRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.STORAGEMANAGER)
                .pattern("EEE")
                .pattern("PSP")
                .pattern("OOO")
                .input('E', Items.ENDER_EYE)
                .input('P', Items.ENDER_PEARL)
                .input('S', Items.STONE)
                .input('O', Items.OBSIDIAN)
                .criterion(hasItem(Items.OBSIDIAN), conditionsFromItem(Items.OBSIDIAN))
                .offerTo(exporter);
    }
}