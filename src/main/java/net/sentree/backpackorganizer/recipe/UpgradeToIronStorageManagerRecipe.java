package net.sentree.backpackorganizer.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import net.sentree.backpackorganizer.item.ModItems;

public class UpgradeToIronStorageManagerRecipe extends SpecialCraftingRecipe {
    public UpgradeToIronStorageManagerRecipe(CraftingRecipeCategory category) {
        super(category);
    }


    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getWidth() < 3 || input.getHeight() < 3) return false;


        // CBC
        // C^C
        // CCC
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                ItemStack s = input.getStackInSlot(x, y);

                boolean in3x3 = x < 3 && y < 3;
                if (!in3x3) {
                    if (!s.isEmpty()) return false;
                    continue;
                }

                boolean ok;
                if (x == 1 && y == 0) ok = s.isOf(Items.IRON_BLOCK);
                else if (x == 1 && y == 1) ok = s.isOf(ModItems.STORAGEMANAGER_COPPER);
                else ok = s.isOf(Items.IRON_INGOT);

                if (!ok) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        ItemStack base = input.getStackInSlot(1, 1);
        if (!base.isOf(ModItems.STORAGEMANAGER_COPPER)) return ItemStack.EMPTY;

        //This is the “keep my stored items / components” line
        return base.copyComponentsToNewStack(ModItems.STORAGEMANAGER_IRON, 1);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.UPGRADE_TO_IRON;
    }
}
