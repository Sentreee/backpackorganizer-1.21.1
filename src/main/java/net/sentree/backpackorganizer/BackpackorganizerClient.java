package net.sentree.backpackorganizer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.sentree.backpackorganizer.item.custom.StorageManagerScreen;
import net.sentree.backpackorganizer.item.custom.StorageManagerScreenHandler;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class BackpackorganizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.STORAGEMANAGER, StorageManagerScreen::new);
    }
}
