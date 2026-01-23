package net.sentree.backpackorganizer.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;

public class ModRecipeSerializers {
    private ModRecipeSerializers() {}

    public static RecipeSerializer<UpgradeToCopperStorageManagerRecipe> UPGRADE_TO_COPPER;
    public static RecipeSerializer<UpgradeToIronStorageManagerRecipe> UPGRADE_TO_IRON;
    public static RecipeSerializer<UpgradeToDiamondStorageManagerRecipe> UPGRADE_TO_DIAMOND;

    public static void register() {
        if (UPGRADE_TO_COPPER != null) return;
        UPGRADE_TO_COPPER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(Backpackorganizer.MOD_ID, "upgrade_to_copper_storage_manager"),
                new SpecialRecipeSerializer<>(UpgradeToCopperStorageManagerRecipe::new)
        );

        if (UPGRADE_TO_IRON != null) return;
        UPGRADE_TO_IRON = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(Backpackorganizer.MOD_ID, "upgrade_to_iron_storage_manager"),
                new SpecialRecipeSerializer<>(UpgradeToIronStorageManagerRecipe::new)
        );

        if (UPGRADE_TO_DIAMOND != null) return;
        UPGRADE_TO_DIAMOND = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(Backpackorganizer.MOD_ID, "upgrade_to_diamond_storage_manager"),
                new SpecialRecipeSerializer<>(UpgradeToDiamondStorageManagerRecipe::new)
        );
    }
}
