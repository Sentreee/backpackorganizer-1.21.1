package net.sentree.backpackorganizer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.sentree.backpackorganizer.item.basic.StorageManagerContainerScreen;
import net.sentree.backpackorganizer.item.basic.StorageManagerContainerScreenHandler;
import net.sentree.backpackorganizer.item.basic.StorageManagerScreen;
import net.sentree.backpackorganizer.util.ModScreenHandlers;

public class BackpackorganizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.STORAGEMANAGER, StorageManagerScreen::new);
        HandledScreens.register(ModScreenHandlers.STORAGEMANAGER_CONTAINER, StorageManagerContainerScreen::new);
    }
}
