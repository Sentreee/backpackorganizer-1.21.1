package net.sentree.backpackorganizer.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class TabButtonWidget extends ClickableWidget {

    // 28x84 texture: 3 frames stacked vertically: normal(0), hover(28), selected(56)
    private static final Identifier TAB_TEX =
            Identifier.of("backpackorganizer", "textures/gui/widgets/tab.png");

    // -1 = manager tab, 0..(n-1) = stored item tabs
    private final int tabIndex;
    private final Supplier<ItemStack> icon;
    private final Supplier<Text> tooltipText;
    private final IntConsumer onPress;

    private boolean selected;

    public TabButtonWidget(int x, int y, int w, int h, int tabIndex,
                           Supplier<ItemStack> icon,
                           Supplier<Text> tooltipText,
                           IntConsumer onPress) {
        super(x, y, w, h, Text.empty());
        this.tabIndex = tabIndex;
        this.icon = icon;
        this.tooltipText = tooltipText;
        this.onPress = onPress;

        // Vanilla tooltip system (shows on hover)
        setTooltip(Tooltip.of(tooltipText.get()));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Pick frame
        int v = 0;
        if (selected) v = 56;
        else if (this.isHovered()) v = 28;

        // Draw textured background
        context.drawTexture(TAB_TEX, getX(), getY(), 0, v, width, height, 28, 84);

        // Draw icon
        ItemStack s = icon.get();
        if (!s.isEmpty()) {
            context.drawItem(s, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        }

        // Keep tooltip text updated (in case the slot item changes)
        if (this.isHovered()) {
            Tooltip tip = Tooltip.of(tooltipText.get());
            if (this.getTooltip() == null || !this.getTooltip().equals(tip)) {
                setTooltip(tip);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.accept(tabIndex);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, tooltipText.get());
        builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage"));
    }
}
