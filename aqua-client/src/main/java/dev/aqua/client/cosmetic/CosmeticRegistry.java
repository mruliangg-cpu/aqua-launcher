package dev.aqua.client.cosmetic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aqua.client.AquaClient;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Каталог косметики Aqua (общий с лаунчером): assets/aquaclient/cosmetics.json.
 * По 20 вещей в каждом слоте; у 3D-вещей есть группы кубиков с анимациями.
 */
public final class CosmeticRegistry {
    public record Box(float x1, float y1, float z1, float x2, float y2, float z2, int color, boolean glow, boolean flat) {}
    /** HD-пластина с текстурой (крылья): ширина w (знак = сторона), высота y1..y2. */
    public record Sprite(net.minecraft.util.Identifier texture, float w, float y1, float y2, boolean glow) {}
    public record Group(float ox, float oy, float oz, List<String> anims, List<Box> boxes, Sprite sprite) {}
    public record Item(String id, Slot slot, String ru, String en, String part, List<Group> groups) {}

    private static final Map<Slot, List<Item>> BY_SLOT = new EnumMap<>(Slot.class);
    private static final Map<String, Item> BY_ID = new HashMap<>();
    private static final List<String> ICONS = new ArrayList<>();

    private CosmeticRegistry() {}

    static {
        try (var in = CosmeticRegistry.class.getResourceAsStream("/assets/aquaclient/cosmetics.json")) {
            JsonObject root = new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8), JsonObject.class);
            JsonObject items = root.getAsJsonObject("items");
            for (Slot s : Slot.values()) {
                List<Item> list = new ArrayList<>();
                JsonArray arr = items.has(s.key) ? items.getAsJsonArray(s.key) : new JsonArray();
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    List<Group> groups = new ArrayList<>();
                    if (o.has("g")) for (JsonElement ge : o.getAsJsonArray("g")) {
                        JsonObject g = ge.getAsJsonObject();
                        JsonArray org = g.getAsJsonArray("o");
                        List<String> anims = new ArrayList<>();
                        for (JsonElement a : g.getAsJsonArray("a")) anims.add(a.getAsString());
                        List<Box> boxes = new ArrayList<>();
                        for (JsonElement be : g.getAsJsonArray("b")) {
                            JsonArray b = be.getAsJsonArray();
                            int flags = b.size() > 7 ? b.get(7).getAsInt() : 0;
                            boxes.add(new Box(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                                    b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat(), (int) b.get(6).getAsLong(), (flags & 1) != 0, (flags & 2) != 0));
                        }
                        Sprite sprite = null;
                        if (g.has("sp")) {
                            JsonObject sp = g.getAsJsonObject("sp");
                            sprite = new Sprite(dev.aqua.client.compat.V.id("aquaclient", "textures/wings/" + sp.get("s").getAsString() + ".png"),
                                    sp.get("w").getAsFloat(), sp.get("y1").getAsFloat(), sp.get("y2").getAsFloat(), sp.has("gl") && sp.get("gl").getAsInt() == 1);
                        }
                        groups.add(new Group(org.get(0).getAsFloat(), org.get(1).getAsFloat(), org.get(2).getAsFloat(), anims, boxes, sprite));
                    }
                    Item it = new Item(o.get("id").getAsString(), s, o.get("ru").getAsString(), o.get("en").getAsString(),
                            o.has("part") ? o.get("part").getAsString() : "", groups);
                    list.add(it);
                    BY_ID.put(s.key + ":" + it.id(), it);
                }
                BY_SLOT.put(s, list);
            }
            for (JsonElement e : root.getAsJsonArray("icons")) ICONS.add(e.getAsString());
        } catch (Exception e) {
            AquaClient.LOG.error("Не удалось загрузить каталог косметики", e);
            for (Slot s : Slot.values()) BY_SLOT.put(s, List.of(new Item("none", s, "Нет", "None", "", List.of())));
        }
    }

    public static List<Item> of(Slot s) { return BY_SLOT.getOrDefault(s, List.of()); }
    public static String[] ids(Slot s) { return of(s).stream().map(Item::id).toArray(String[]::new); }
    public static Item get(Slot s, String id) { return BY_ID.get(s.key + ":" + id); }

    public static String name(Slot s, String id, boolean ru) {
        Item it = get(s, id);
        return it == null ? id : ru ? it.ru() : it.en();
    }

    /** Символ шрифта aquaclient:icons для значка у ника. */
    public static String glyph(String icon) {
        int i = ICONS.indexOf(icon);
        return String.valueOf((char) (0xE000 + Math.max(0, i)));
    }
}
