package dev.aqua.client.cosmetic;

public enum Slot {
    CAPE("cape", "Плащ", "Cape"),
    HAT("hat", "Шляпа", "Hat"),
    FACE("face", "Лицо", "Face"),
    WINGS("wings", "Крылья", "Wings"),
    BACK("back", "Спина", "Back"),
    PET("pet", "Питомец", "Pet"),
    AURA("aura", "Аура", "Aura"),
    ICON("icon", "Значок у ника", "Name icon");

    public final String key, ru, en;

    Slot(String key, String ru, String en) { this.key = key; this.ru = ru; this.en = en; }
}
