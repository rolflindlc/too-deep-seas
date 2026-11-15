package com.rolflin.toodeepseas.client;

import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.menu.MesaPescadorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Ventana de la mesa del pescador. Se dibuja con rectángulos (sin textura): la caña en el centro,
 * accesorios arriba y abajo, cebos a los lados y, a la derecha, las estadísticas resultantes en vivo.
 */
public class MesaPescadorScreen extends AbstractContainerScreen<MesaPescadorMenu> {
    private static final int PANEL_X = 68, PANEL_Y = 17, PANEL_W = 101, PANEL_H = 83;
    private static final int LINE = 9;

    public MesaPescadorScreen(MesaPescadorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MesaPescadorMenu.WIDTH;
        this.imageHeight = MesaPescadorMenu.HEIGHT;
        this.inventoryLabelY = MesaPescadorMenu.INV_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // Marco de la ventana, al estilo vanilla
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + MesaPescadorMenu.INV_Y + row * 18, 0xFF8B8B8B);
        }
        for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + MesaPescadorMenu.INV_Y + 58, 0xFF8B8B8B);

        RodTier tier = menu.tier();
        slot(g, x + MesaPescadorMenu.ROD_X, y + MesaPescadorMenu.ROD_Y, 0xFF7B8FA8);
        for (int i = 0; i < MesaPescadorMenu.ACC_COUNT; i++) {
            int sx = x + MesaPescadorMenu.ACC_POS[i][0], sy = y + MesaPescadorMenu.ACC_POS[i][1];
            if (tier != null && i < tier.accessorySlots) slot(g, sx, sy, 0xFF8B8B8B);
            else locked(g, sx, sy);
        }
        for (int i = 0; i < MesaPescadorMenu.BAIT_COUNT; i++) {
            int sx = x + MesaPescadorMenu.BAIT_POS[i][0], sy = y + MesaPescadorMenu.BAIT_POS[i][1];
            if (tier != null && i < tier.baitSlots) slot(g, sx, sy, 0xFF8FA07A);
            else locked(g, sx, sy);
        }

        // Panel de estadísticas
        int px = x + PANEL_X, py = y + PANEL_Y;
        g.fill(px - 1, py - 1, px + PANEL_W, py + PANEL_H, 0xFF373737);
        g.fill(px, py, px + PANEL_W + 1, py + PANEL_H + 1, 0xFFFFFFFF);
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF1F2A36);
        ItemStack rod = menu.rod();
        int ty = py + 3;
        if (tier == null) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.toodeepseas.sin_cana"), PANEL_W - 6)) {
                g.drawString(font, line, px + 3, ty, 0xFF9FB4C8, false);
                ty += LINE;
            }
        } else {
            int used = 0;
            for (int i = 0; i < MesaPescadorMenu.ACC_COUNT; i++) {
                if (menu.getSlot(MesaPescadorMenu.ACC_START + i).hasItem()) used++;
            }
            g.drawString(font, Component.translatable("gui.toodeepseas.ranuras", used, tier.accessorySlots, "I".repeat(tier.maxAccessoryLevel())), px + 3, ty, 0xFFFFD75E, false);
            ty += LINE;
            List<Component> stats = RodTooltip.statLines(rod);
            for (int i = 0; i < stats.size() && i < 8; i++) {
                g.drawString(font, stats.get(i), px + 3, ty, i < 4 ? 0xFFFFFFFF : 0xFF7FE0C0, false);
                ty += LINE;
            }
        }
    }

    /** Casilla hundida de 18×18 alrededor de la posición del objeto (16×16). */
    private static void slot(GuiGraphics g, int x, int y, int inner) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, inner);
    }

    /** Ranura que esta caña no tiene (o no hay caña): solo la silueta. */
    private static void locked(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFB4B4B4);
        g.fill(x, y, x + 16, y + 16, 0xFFBDBDBD);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (menu.getCarried().isEmpty() && hovered != null && !hovered.hasItem() && hovered.index < MesaPescadorMenu.GEAR_END) {
            String key = hovered.index == MesaPescadorMenu.ROD ? "gui.toodeepseas.ranura_cana"
                    : hovered.index < MesaPescadorMenu.BAIT_START ? "gui.toodeepseas.ranura_accesorio" : "gui.toodeepseas.ranura_cebo";
            g.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }
}
