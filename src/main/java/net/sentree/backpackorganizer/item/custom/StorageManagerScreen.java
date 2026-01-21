package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;
import net.sentree.backpackorganizer.client.gui.TabButtonWidget;
import net.sentree.backpackorganizer.item.ModItems;
import net.sentree.backpackorganizer.network.OpenStorageTabPayload;

@Environment(EnvType.CLIENT)
public class StorageManagerScreen extends HandledScreen<StorageManagerScreenHandler> {

    private static final Identifier BASIC_TEX =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager.png");

    private static final Identifier COPPER_TEX =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_copper.png");

    private static final Identifier IRON_TEX =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_iron.png");

    private static final Identifier DIAMOND_TEX =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_diamond.png");

    public StorageManagerScreen(StorageManagerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);

        // Your custom GUI pngs should match these.
        // If your copper texture is a different height, change this.
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    private Identifier getTexture() {
        ItemStack manager = this.handler.getManagerStack();
        if (manager.isOf(ModItems.STORAGEMANAGER_COPPER)) return COPPER_TEX;
        if (manager.isOf(ModItems.STORAGEMANAGER_IRON)) return IRON_TEX;
        if (manager.isOf(ModItems.STORAGEMANAGER_DIAMOND)) return DIAMOND_TEX;
        return BASIC_TEX;
    }

    @Override
    protected void init() {
        super.init();

        int slots = this.handler.getOrganizerInv().size(); // 3 basic, 5 copper
        int totalTabs = 1 + slots; // manager tab + item tabs

        int tabW = 28, tabH = 28, gap = 2;
        int baseX = this.x + this.backgroundWidth + 4;
        int baseY = this.y + 6;

        int rowsPerCol = 5;

        for (int idx = 0; idx < totalTabs; idx++) {
            final int tabIndex = (idx == 0) ? -1 : (idx - 1);

            int col = idx / rowsPerCol;
            int row = idx % rowsPerCol;

            int x = baseX + col * (tabW + gap);
            int y = baseY + row * (tabH + gap);

            TabButtonWidget btn = new TabButtonWidget(
                    x, y, tabW, tabH,
                    tabIndex,
                    // icon
                    () -> {
                        if (tabIndex < 0) return this.handler.getManagerStack().copy();
                        return this.handler.getOrganizerInv().getStack(tabIndex);
                    },
                    // tooltip
                    () -> {
                        if (tabIndex < 0) return this.handler.getManagerStack().getName();
                        ItemStack s = this.handler.getOrganizerInv().getStack(tabIndex);
                        return s.isEmpty()
                                ? Text.translatable("screen.backpackorganizer.tab.empty")
                                : s.getName();
                    },
                    // click
                    clicked -> ClientPlayNetworking.send(OpenStorageTabPayload.of(this.handler.getHand(), clicked))
            );

            // On the manager screen, the selected tab is the manager tab (-1)
            btn.setSelected(tabIndex < 0);
            this.addDrawableChild(btn);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(getTexture(), this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
