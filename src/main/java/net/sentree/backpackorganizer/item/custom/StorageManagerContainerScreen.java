package net.sentree.backpackorganizer.item.custom;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sentree.backpackorganizer.Backpackorganizer;
import net.sentree.backpackorganizer.client.gui.TabButtonWidget;
import net.sentree.backpackorganizer.network.OpenStorageTabPayload;
import net.sentree.backpackorganizer.network.SetContainerScrollPayload;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class StorageManagerContainerScreen extends HandledScreen<StorageManagerContainerScreenHandler> {

    private static final Identifier TEX_VANILLA =
            Identifier.of("minecraft", "textures/gui/container/generic_54.png"); // 256x256

    private static final Identifier TEX_GOLD =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_gold_backpack.png"); // 256x288

    private static final Identifier TEX_DIAMOND =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_diamond_backpack.png"); // 256x352

    private static final Identifier TEX_NETHERITE =
            Identifier.of(Backpackorganizer.MOD_ID, "textures/gui/container/storagemanager_netherite_backpack.png"); // 256x352

    private static final int VANILLA_TEX_W = 256;
    private static final int VANILLA_TEX_H = 256;

    private static final int CUSTOM_TEX_W = 256;

    // ===== Container-row scrolling (inside the grid) =====
    private int scrollRow = 0;
    private boolean draggingScroll = false;

    // ===== Whole-GUI vertical scrolling (when GUI doesn't fit) =====
    private int guiBaseY;
    private int guiScrollPx = 0;
    private int maxGuiScrollPx = 0;

    // Keep tabs so we can reposition them when the whole GUI scrolls
    private final List<TabButtonWidget> tabButtons = new ArrayList<>();

    public StorageManagerContainerScreen(StorageManagerContainerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);

        if (handler.usesCustomTexture()) {
            this.backgroundWidth = CUSTOM_TEX_W;

            // Gold is shorter; Diamond/Netherite are taller
            if (handler.getLayoutKind() == StorageManagerContainerScreenHandler.LayoutKind.GOLD_9x9) {
                this.backgroundHeight = 288;
                this.playerInventoryTitleY = 182; // 194 - 12
            } else if (handler.getLayoutKind() == StorageManagerContainerScreenHandler.LayoutKind.DIAMOND_12x9) {
                this.backgroundHeight = 352;
                this.playerInventoryTitleY = 188; // 200 - 12
            } else {
                // Netherite (12x10) and generic 12-col
                this.backgroundHeight = 352;
                this.playerInventoryTitleY = 206; // 218 - 12
            }
        } else {
            this.backgroundWidth = 176;
            int visibleRows = handler.getVisibleRows();
            this.backgroundHeight = 114 + visibleRows * 18;
            this.playerInventoryTitleY = this.backgroundHeight - 94;
        }
    }

    private Identifier pickTexture() {
        if (!handler.usesCustomTexture()) return TEX_VANILLA;

        return switch (handler.getLayoutKind()) {
            case GOLD_9x9 -> TEX_GOLD;
            case DIAMOND_12x9 -> TEX_DIAMOND;
            case NETHERITE_12x10 -> TEX_NETHERITE;
            default -> TEX_VANILLA;
        };
    }

    @Override
    protected void init() {
        super.init();

        this.guiBaseY = this.y;
        this.guiScrollPx = 0;
        this.maxGuiScrollPx = Math.max(0, (this.backgroundHeight + 16) - this.height);

        buildTabs();
        applyGuiScroll();

        this.scrollRow = 0;
        if (handler.getMaxScrollRow() > 0) {
            ClientPlayNetworking.send(SetContainerScrollPayload.of(0));
        }
        handler.setClientScrollRow(0);
    }

    private void buildTabs() {
        tabButtons.clear();

        int slots = this.handler.getOrganizerInv().size();
        int totalTabs = 1 + slots;

        int tabW = 28, tabH = 28, gap = 2;
        int rowsPerCol = 4;

        for (int idx = 0; idx < totalTabs; idx++) {
            final int tabIndex = (idx == 0) ? -1 : (idx - 1);

            TabButtonWidget btn = new TabButtonWidget(
                    0, 0, tabW, tabH,
                    tabIndex,
                    () -> {
                        if (tabIndex < 0) return this.handler.getManagerStack().copy();
                        return this.handler.getOrganizerInv().getStack(tabIndex);
                    },
                    () -> {
                        if (tabIndex < 0) return this.handler.getManagerStack().getName();
                        ItemStack s = this.handler.getOrganizerInv().getStack(tabIndex);
                        return s.isEmpty() ? Text.translatable("screen.backpackorganizer.tab.empty") : s.getName();
                    },
                    clicked -> ClientPlayNetworking.send(OpenStorageTabPayload.of(this.handler.getHand(), clicked))
            );

            btn.setSelected(false);
            this.addDrawableChild(btn);
            tabButtons.add(btn);
        }

        layoutTabs();
    }

    private void layoutTabs() {
        int tabW = 28, tabH = 28, gap = 2;
        int rowsPerCol = 4;

        int baseX = this.x + this.backgroundWidth + 4;
        int baseY = this.y + 6;

        for (int idx = 0; idx < tabButtons.size(); idx++) {
            int col = idx / rowsPerCol;
            int row = idx % rowsPerCol;

            int tx = baseX + col * (tabW + gap);
            int ty = baseY + row * (tabH + gap);

            TabButtonWidget btn = tabButtons.get(idx);
            btn.setX(tx);
            btn.setY(ty);
        }
    }

    private void applyGuiScroll() {
        this.y = this.guiBaseY - this.guiScrollPx;
        layoutTabs();
    }

    private int scrollBarX() {
        return this.x + this.backgroundWidth - 12;
    }

    private int scrollBarY() {
        return this.y + (handler.usesCustomTexture() ? handler.getGridTopBorderY() : 18);
    }

    private int scrollBarHeight() {
        return handler.getVisibleRows() * 18;
    }

    private boolean isInScrollBar(double mouseX, double mouseY) {
        int sx = scrollBarX();
        int sy = scrollBarY();
        int sh = scrollBarHeight();
        return mouseX >= sx && mouseX < sx + 12 && mouseY >= sy && mouseY < sy + sh;
    }

    private int getMaxScrollRow() {
        return handler.getMaxScrollRow();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = (int) Math.signum(verticalAmount);

        // If GUI doesn't fit, prefer scrolling the whole GUI unless Shift is held or the user is aiming at the container scrollbar.
        if (maxGuiScrollPx > 0 && !Screen.hasShiftDown() && !isInScrollBar(mouseX, mouseY)) {
            guiScrollPx = Math.max(0, Math.min(maxGuiScrollPx, guiScrollPx - delta * 18));
            applyGuiScroll();
            return true;
        }

        // Otherwise scroll container rows (if any)
        int max = getMaxScrollRow();
        if (max <= 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        scrollRow = Math.max(0, Math.min(max, scrollRow - delta));
        ClientPlayNetworking.send(SetContainerScrollPayload.of(scrollRow));
        handler.setClientScrollRow(scrollRow);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMaxScrollRow() > 0 && isInScrollBar(mouseX, mouseY)) {
            draggingScroll = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScroll && getMaxScrollRow() > 0) {
            int sy = scrollBarY();
            int sh = scrollBarHeight();

            double t = (mouseY - sy) / (double) sh;
            t = Math.max(0.0, Math.min(1.0, t));

            scrollRow = (int) Math.round(t * getMaxScrollRow());
            ClientPlayNetworking.send(SetContainerScrollPayload.of(scrollRow));
            handler.setClientScrollRow(scrollRow);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        Identifier tex = pickTexture();
        int x = this.x;
        int y = this.y;

        if (handler.usesCustomTexture()) {
            int h = this.backgroundHeight;
            context.drawTexture(tex, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, CUSTOM_TEX_W, h);
        } else {
            int rows = handler.getVisibleRows();
            context.drawTexture(tex, x, y, 0, 0, backgroundWidth, 17 + rows * 18, VANILLA_TEX_W, VANILLA_TEX_H);
            context.drawTexture(tex, x, y + 17 + rows * 18, 0, 126, backgroundWidth, 96, VANILLA_TEX_W, VANILLA_TEX_H);
        }

        int max = getMaxScrollRow();
        if (max > 0) {
            int sx = scrollBarX();
            int sy = scrollBarY();
            int sh = scrollBarHeight();

            context.fill(sx + 4, sy, sx + 8, sy + sh, 0xFF555555);

            int knobH = Math.max(12, (int) (sh * (handler.getVisibleRows() / (float) handler.getTotalRows())));
            int knobY = sy + (int) ((sh - knobH) * (scrollRow / (float) max));
            context.fill(sx + 2, knobY, sx + 10, knobY + knobH, 0xFFAAAAAA);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
