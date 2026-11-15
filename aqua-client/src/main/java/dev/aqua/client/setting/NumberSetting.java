package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;
    private final String suffix;

    public NumberSetting(String name, double def, double min, double max, double step) { this(name, def, min, max, step, ""); }

    public NumberSetting(String name, double def, double min, double max, double step, String suffix) {
        super(name, def);
        this.min = min; this.max = max; this.step = step; this.suffix = suffix;
    }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) c = Math.round(c / step) * step;
        this.value = Math.max(min, Math.min(max, c));
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public int getInt() { return (int) Math.round(value); }
    public float getFloat() { return value.floatValue(); }
    public double percent() { return (value - min) / (max - min); }
    public void setPercent(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }

    public String display() {
        String s = step >= 1 ? String.valueOf(Math.round(value)) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
        return s + suffix;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}
