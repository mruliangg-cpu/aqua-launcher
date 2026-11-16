package dev.aqua.client.cosmetic;

import java.util.List;
import java.util.Map;

/**
 * Правила своего титула над ником (такие же в лаунчере и на Aqua Network).
 * До 12 символов: буквы, цифры, пробел и _ - ! ? .  OWNER, ADMIN, MODER в любом виде — нельзя.
 */
public final class TitleRules {
    private TitleRules() {}

    public static final List<String> COLORS = List.of("gold", "aqua", "blue", "green", "red", "pink", "purple", "white", "rainbow");
    private static final Map<String, Integer> HEX = Map.of("gold", 0xFFC83D, "aqua", 0x3FE0FF, "blue", 0x4D8DFF, "green", 0x5CFF8A,
            "red", 0xFF5566, "pink", 0xFF7AD9, "purple", 0xB57CFF, "white", 0xFFFFFF);
    private static final String CYR = "АВЕЁКМНОРСТХУДИШЛГЗПЧБ", CYR_LAT = "ABEEKMHOPCTXYDNWLRENAB";
    private static final String LEET = "0134567 89!L", LEET_LAT = "OIEASGT BGII";
    private static final String LAT = "ABEKMHOPCTXY0ЁND", LAT_CYR = "АВЕКМНОРСТХУОЕИД";
    private static final List<String> BAN_LAT = List.of("OWNER", "OWNR", "ADMIN", "ADMN", "MODER", "MODR");
    private static final List<String> BAN_EXACT = List.of("MOD", "MODS", "ADM", "OWN");
    private static final List<String> BAN_CYR = List.of("АДМИН", "МОДЕР", "ВЛАДЕЛ", "ОВНЕР", "ОУНЕР", "ОВНР");

    /** Чистый титул или null, если пустой/запрещённый. */
    public static String clean(String in) {
        if (in == null) return null;
        String t = in.replaceAll("\\s+", " ").trim();
        if (t.isEmpty() || t.length() > 12) return null;
        if (!t.matches("[A-Za-zА-Яа-яЁё0-9 _\\-!?.]+")) return null;
        String up = t.toUpperCase();
        StringBuilder lat = new StringBuilder(), cyr = new StringBuilder();
        for (char c : up.toCharArray()) {
            int i = CYR.indexOf(c);
            char l = i >= 0 ? CYR_LAT.charAt(i) : c;
            int j = LEET.indexOf(l);
            if (j >= 0 && l != ' ') l = LEET_LAT.charAt(j);
            if (l >= 'A' && l <= 'Z') lat.append(l);
            int k = LAT.indexOf(c);
            char r = k >= 0 ? LAT_CYR.charAt(k) : c;
            if (r >= 'А' && r <= 'Я') cyr.append(r);
        }
        String L = lat.toString().replace("VV", "W"), C = cyr.toString();
        for (String w : BAN_LAT) if (L.contains(w)) return null;
        if (BAN_EXACT.contains(L)) return null;
        for (String w : BAN_CYR) if (C.contains(w)) return null;
        return t;
    }

    /** Допустимый цвет: имя из списка или свой «#RRGGBB». Иначе — золото. */
    public static String color(String c) {
        if (c == null) return "gold";
        if (c.matches("#[0-9A-Fa-f]{6}")) return c.toUpperCase();
        return COLORS.contains(c) ? c : "gold";
    }

    public static boolean isCustom(String c) { return c != null && c.matches("#[0-9A-Fa-f]{6}"); }

    /** Цвет титула (радуга переливается, свой цвет — как указан). */
    public static int rgb(String c) {
        if (isCustom(c)) return Integer.parseInt(c.substring(1), 16);
        if ("rainbow".equals(c)) return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f, 0.6f, 1f) & 0xFFFFFF;
        return HEX.getOrDefault(color(c), 0xFFC83D);
    }
}
