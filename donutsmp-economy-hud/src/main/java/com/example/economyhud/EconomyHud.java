package com.example.economyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EconomyHud implements ClientModInitializer {
    static boolean enabled = true, graphEnabled = true, automatic = true;
    static int x = 10, y = 10;
    static String balance = "Waiting for /bal...";
    private static BigDecimal current, start;
    private static final List<BigDecimal> history = new ArrayList<>();
    private static final Pattern MONEY_LINE = Pattern.compile("(?i)^\\s*(?:(?:balance|your balance|money|coins)\\s*[:=]?\\s*)?\\$\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*$");
    private static final Pattern LABELED = Pattern.compile("(?i)^\\s*(?:your\\s+)?(?:balance|money)\\s*(?:is|:|=)\\s*\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*$");
    private static KeyMapping settingsKey;

    @Override public void onInitializeClient() {
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.economyhud.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O,
            KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) client.setScreen(new EconomySettingsScreen());
        });
        // Observe server-supplied game messages. Never send commands automatically.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (automatic) readBalance(message.getString());
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("economyhud", "balance"), (graphics, tick) -> {
            if (!enabled || Minecraft.getInstance().options.hideGui) return;
            draw(graphics);
        });
    }

    static void readBalance(String raw) {
        if (raw == null) return;
        String[] lines = raw.replace('§', '\u00a7').split("\\R");
        for (String line : lines) {
            line = line.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "").trim();
            Matcher match = MONEY_LINE.matcher(line);
            if (!match.matches()) match = LABELED.matcher(line);
            if (!match.matches()) continue;
            try {
                setBalance(new BigDecimal(match.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) { }
            break;
        }
    }

    static void setBalance(BigDecimal amount) {
        if (amount.signum() < 0) return;
        if (current != null && current.compareTo(amount) == 0) return;
        if (start == null) start = amount;
        current = amount;
        balance = "$" + String.format("%,.0f", amount);
        history.add(amount.subtract(start));
        if (history.size() > 120) history.remove(0);
    }

    static void resetSession() {
        if (current != null) {
            start = current;
            history.clear();
            history.add(BigDecimal.ZERO);
        }
    }

    private static String compact(BigDecimal amount) {
        BigDecimal abs = amount.abs();
        String[] units = {"", "K", "M", "B", "T", "Q"};
        int i = 0;
        while (abs.compareTo(BigDecimal.valueOf(1000)) >= 0 && i < units.length - 1) {
            abs = abs.divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);
            i++;
        }
        return (amount.signum() >= 0 ? "+" : "-") + "$" + abs.stripTrailingZeros().toPlainString() + units[i];
    }

    static void draw(GuiGraphics g) {
        var font = Minecraft.getInstance().font;
        int w = 208;
        int h = graphEnabled ? 133 : 66;
        g.fill(x, y, x + w, y + h, 0xD91A2135);
        g.fill(x, y, x + w, y + 2, 0xFFAF79C4);
        g.drawString(font, Component.literal("DONUTSMP  /  BALANCE"), x + 9, y + 9, 0xFFE5A3D8, false);
        g.drawString(font, Component.literal(balance), x + 9, y + 25, 0xFF55E99A, true);
        BigDecimal delta = current == null || start == null ? BigDecimal.ZERO : current.subtract(start);
        int color = delta.signum() < 0 ? 0xFFFF657C : 0xFF55E99A;
        g.drawString(font, Component.literal("SESSION  " + (current == null ? "No data" : compact(delta))), x + 9, y + 44, color, false);
        if (graphEnabled) drawGraph(g, x + 9, y + 66, 190, 53);
    }

    private static void drawGraph(GuiGraphics g, int left, int top, int width, int height) {
        g.fill(left, top, left + width, top + height, 0x6A080D1A);
        g.drawString(Minecraft.getInstance().font, Component.literal("PROFIT / LOSS"), left + 3, top + 3, 0xFFB7B9CB, false);
        int chartTop = top + 15, chartHeight = height - 19;
        BigDecimal maxAbs = BigDecimal.ONE;
        for (BigDecimal v : history) if (v.abs().compareTo(maxAbs) > 0) maxAbs = v.abs();
        int zeroY = chartTop + chartHeight / 2;
        g.fill(left + 2, zeroY, left + width - 2, zeroY + 1, 0xFF555F70);
        if (history.isEmpty()) return;
        int prevX = left + 3, prevY = zeroY;
        for (int i = 0; i < history.size(); i++) {
            int px = left + 3 + (history.size() == 1 ? 0 : i * (width - 7) / (history.size() - 1));
            double ratio = history.get(i).divide(maxAbs, 5, java.math.RoundingMode.HALF_UP).doubleValue();
            int py = zeroY - (int)Math.round(ratio * (chartHeight / 2.0 - 2));
            int color = history.get(i).signum() < 0 ? 0xFFFF657C : 0xFF55E99A;
            if (i > 0) {
                int dx = Math.max(1, px - prevX);
                for (int s = 0; s <= dx; s++) {
                    int ix = prevX + s;
                    int iy = prevY + (py - prevY) * s / dx;
                    g.fill(ix, iy, ix + 2, iy + 2, color);
                }
            } else g.fill(px, py, px + 2, py + 2, color);
            prevX = px; prevY = py;
        }
    }
}
