package dev.aqua.client.module;

public enum Category {
    HUD("HUD", "▣"),
    COMBAT("Бой", "⚔"),
    RENDER("Визуал", "◈"),
    MOVEMENT("Движение", "➤"),
    COSMETIC("Косметика", "✿"),
    MISC("Разное", "✦"),
    PERFORMANCE("FPS", "⚡");

    public final String title, icon;

    Category(String title, String icon) { this.title = title; this.icon = icon; }
}
