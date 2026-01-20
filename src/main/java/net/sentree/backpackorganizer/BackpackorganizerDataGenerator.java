package net.sentree.backpackorganizer;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.sentree.backpackorganizer.datagen.BackpackOrganizerModelProvider;
import net.sentree.backpackorganizer.datagen.BackpackOrganizerRecipeProvider;

public class BackpackorganizerDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(BackpackOrganizerModelProvider::new);
		pack.addProvider(BackpackOrganizerRecipeProvider::new);
	}
}
