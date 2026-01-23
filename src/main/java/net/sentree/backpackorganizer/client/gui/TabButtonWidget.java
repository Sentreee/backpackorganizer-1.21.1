package net.sentree.backpackorganizer.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class TabButtonWidget extends ClickableWidget {

    private static final Identifier TAB_TEX =
            Identifier.of("backpackorganizer", "textures/gui/widgets/tab.png");

    private final int tabIndex; // -1 manager, 0..N-1 item slots
    private final Supplier<ItemStack> icon;
    private final Supplier<Text> tooltip;
    private final IntConsumer onPress;
    private boolean selected;

    public TabButtonWidget(int x, int y, int w, int h, int tabIndex,
                           Supplier<ItemStack> icon,
                           Supplier<Text> tooltip,
                           IntConsumer onPress) {
        super(x, y, w, h, Text.empty());
        this.tabIndex = tabIndex;
        this.icon = icon;
        this.tooltip = tooltip;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int v = 0; // normal
        if (selected) v = 56;               // 2 * 28
        else if (this.isHovered()) v = 28;  // 1 * 28

        context.drawTexture(TAB_TEX, getX(), getY(), 0, v, width, height, 28, 84);

        ItemStack s = icon.get();
        if (!s.isEmpty()) {
            context.drawItem(s, getX() + 6, getY() + 6);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.accept(tabIndex);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, tooltip.get());
        builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage"));
    }
}
