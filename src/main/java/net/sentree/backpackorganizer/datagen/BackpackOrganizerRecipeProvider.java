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

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.STORAGEMANAGER_COPPER)
                .pattern("CBC")
                .pattern("C^C")
                .pattern("CCC")
                .input('^', ModItems.STORAGEMANAGER)
                .input('C', Items.COPPER_INGOT)
                .input('B', Items.COPPER_BLOCK)
                .criterion(hasItem(ModItems.STORAGEMANAGER), conditionsFromItem(ModItems.STORAGEMANAGER))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.STORAGEMANAGER_IRON)
                .pattern("IBI")
                .pattern("I^I")
                .pattern("III")
                .input('^', ModItems.STORAGEMANAGER_COPPER)
                .input('I', Items.IRON_INGOT)
                .input('B', Items.IRON_BLOCK)
                .criterion(hasItem(ModItems.STORAGEMANAGER_COPPER), conditionsFromItem(ModItems.STORAGEMANAGER_COPPER))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.STORAGEMANAGER_DIAMOND)
                .pattern("DBD")
                .pattern("D^D")
                .pattern("DDD")
                .input('^', ModItems.STORAGEMANAGER_IRON)
                .input('D', Items.DIAMOND)
                .input('B', Items.DIAMOND_BLOCK)
                .criterion(hasItem(ModItems.STORAGEMANAGER_IRON), conditionsFromItem(ModItems.STORAGEMANAGER_IRON))
                .offerTo(exporter);
    }
}