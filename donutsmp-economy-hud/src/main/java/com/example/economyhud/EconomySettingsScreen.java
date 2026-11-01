package com.example.economyhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;

public class EconomySettingsScreen extends Screen {
    private EditBox balanceBox;
    public EconomySettingsScreen() { super(Component.literal("Economy HUD Settings")); }
    @Override protected void init() {
        int cx = width / 2;
        balanceBox = new EditBox(font, cx - 100, height / 2 - 60, 200, 20, Component.literal("Manual balance (optional)"));
        balanceBox.setMaxLength(64);
        balanceBox.setValue("");
        balanceBox.setHint(Component.literal("Manual balance, e.g. 117550323393"));
        addRenderableWidget(balanceBox);
        addRenderableWidget(Button.builder(Component.literal("HUD: " + (EconomyHud.enabled ? "ON" : "OFF")), b -> {
            EconomyHud.enabled = !EconomyHud.enabled;
            b.setMessage(Component.literal("HUD: " + (EconomyHud.enabled ? "ON" : "OFF")));
        }).bounds(cx - 100, height / 2 - 34, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Graph: " + (EconomyHud.graphEnabled ? "ON" : "OFF")), b -> {
            EconomyHud.graphEnabled = !EconomyHud.graphEnabled;
            b.setMessage(Component.literal("Graph: " + (EconomyHud.graphEnabled ? "ON" : "OFF")));
        }).bounds(cx + 2, height / 2 - 34, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Auto-read: " + (EconomyHud.automatic ? "ON" : "OFF")), b -> {
            EconomyHud.automatic = !EconomyHud.automatic;
            b.setMessage(Component.literal("Auto-read: " + (EconomyHud.automatic ? "ON" : "OFF")));
        }).bounds(cx - 100, height / 2 - 8, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset session baseline"), b -> EconomyHud.resetSession())
            .bounds(cx - 100, height / 2 + 18, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply manual balance / Close"), b -> {
            String value = balanceBox.getValue().replace("$", "").replace(",", "").trim();
            if (!value.isEmpty()) {
                try { EconomyHud.setBalance(new BigDecimal(value)); } catch (NumberFormatException ignored) { }
            }
            onClose();
        }).bounds(cx - 100, height / 2 + 44, 200, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, height / 2 - 89, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Reads server game messages; no commands sent"), width / 2, height / 2 + 76, 0xFFAAAAAA);
    }
}
