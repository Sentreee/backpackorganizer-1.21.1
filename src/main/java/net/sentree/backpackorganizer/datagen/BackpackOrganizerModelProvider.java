package net.sentree.backpackorganizer.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.sentree.backpackorganizer.item.ModItems;

public class BackpackOrganizerModelProvider extends FabricModelProvider {

    public BackpackOrganizerModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // No blocks
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.STORAGEMANAGER, Models.GENERATED);
        itemModelGenerator.register(ModItems.STORAGEMANAGER_COPPER, Models.GENERATED);
    }
}
