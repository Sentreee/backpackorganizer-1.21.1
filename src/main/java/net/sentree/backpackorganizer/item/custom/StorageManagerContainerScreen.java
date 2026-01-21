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
import net.sentree.backpackorganizer.client.gui.TabButtonWidget;
import net.sentree.backpackorganizer.item.ModItems;
import net.sentree.backpackorganizer.network.OpenStorageTabPayload;

@Environment(EnvType.CLIENT)
public class StorageManagerContainerScreen extends HandledScreen<StorageManagerContainerScreenHandler> {

    private static final Identifier TEX = Identifier.of("minecraft", "textures/gui/container/generic_54.png");

    public StorageManagerContainerScreen(StorageManagerContainerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;

        int rows = handler.getRows();
        this.backgroundHeight = 114 + rows * 18;      // vanilla chest height formula
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int slots = this.handler.getManagerInv().size(); // manager slots (3, 5, etc.)
        int totalTabs = 1 + slots; // manager tab (-1) + each slot (0..slots-1)

        int tabW = 28;
        int tabH = 28;
        int gap = 2;

        // Position to the right of the GUI
        int baseX = this.x + this.backgroundWidth + 4;
        int baseY = this.y + 6;

        // With 5 slots => 6 tabs. Use 3 rows per column (2 columns) like we discussed.
        // For other sizes, still works decently.
        int rowsPerCol = 5;
        int cols = (totalTabs + rowsPerCol - 1) / rowsPerCol;

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
                        if (tabIndex < 0) return new ItemStack(ModItems.STORAGEMANAGER);
                        return this.handler.getManagerInv().getStack(tabIndex);
                    },
                    // tooltip
                    () -> {
                        if (tabIndex < 0) return Text.translatable("screen.backpackorganizer.storagemanager");

                        ItemStack s = this.handler.getManagerInv().getStack(tabIndex);
                        if (s.isEmpty()) return Text.translatable("screen.backpackorganizer.tab.empty");
                        return s.getName();
                    },
                    // click action
                    clicked -> ClientPlayNetworking.send(OpenStorageTabPayload.of(this.handler.getHand(), clicked))
            );

            // Selected: highlight the slot currently being edited in this container screen
            btn.setSelected(tabIndex == this.handler.getManagerSlot());

            this.addDrawableChild(btn);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        int rows = handler.getRows();

        // top (container) part
        context.drawTexture(TEX, x, y, 0, 0, backgroundWidth, 17 + rows * 18);
        // bottom (player inventory) part
        context.drawTexture(TEX, x, y + 17 + rows * 18, 0, 126, backgroundWidth, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
