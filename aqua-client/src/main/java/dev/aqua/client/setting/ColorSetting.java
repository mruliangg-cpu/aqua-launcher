package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aqua.client.util.ColorUtil;

/** Цвет ARGB с поддержкой «хромы» (переливающийся цвет). */
public class ColorSetting extends Setting<Integer> {
    private boolean chroma;
    private float chromaSpeed = 1f;

    public ColorSetting(String name, int argb) { super(name, argb); }

    public boolean isChroma() { return chroma; }
    public void setChroma(boolean chroma) { this.chroma = chroma; }

    /** Итоговый цвет с учётом хромы. offset сдвигает оттенок (для «волны»). */
    public int color(float offset) {
        if (!chroma) return value;
        float hue = ((System.currentTimeMillis() % (long) (4000 / chromaSpeed)) / (4000f / chromaSpeed) + offset) % 1f;
        float[] hsb = ColorUtil.toHsb(value);
        return ColorUtil.fromHsb(hue, Math.max(0.5f, hsb[1]), Math.max(0.8f, hsb[2]), ColorUtil.alpha(value));
    }

    public int color() { return color(0); }

    @Override public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("argb", value);
        o.addProperty("chroma", chroma);
        return o;
    }

    @Override public void fromJson(JsonElement e) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("argb")) value = o.get("argb").getAsInt();
            if (o.has("chroma")) chroma = o.get("chroma").getAsBoolean();
        } else if (e.isJsonPrimitive()) value = e.getAsInt();
    }
}
