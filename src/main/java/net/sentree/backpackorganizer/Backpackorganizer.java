package net.sentree.backpackorganizer;

import net.fabricmc.api.ModInitializer;

import net.sentree.backpackorganizer.item.ModItemGroups;
import net.sentree.backpackorganizer.item.ModItems;
import net.sentree.backpackorganizer.network.ModNetworking;
import net.sentree.backpackorganizer.recipe.ModRecipeSerializers;
import net.sentree.backpackorganizer.util.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backpackorganizer implements ModInitializer {
	public static final String MOD_ID = "backpackorganizer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();

		ModItems.registerModItems();

		ModScreenHandlers.register();

		ModNetworking.register();

		ModRecipeSerializers.register();
	}
}