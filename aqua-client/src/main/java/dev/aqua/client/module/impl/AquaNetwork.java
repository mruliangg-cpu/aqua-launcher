package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.net.AquaNet;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.StringSetting;

/** Сеть игроков Aqua Launcher: значок черепахи у ника и в TAB, чужая косметика и скины. */
public class AquaNetwork extends Module {
    private final StringSetting url = add(new StringSetting("Сервер", "", 200));
    private final BoolSetting icons = add(new BoolSetting("Значок у ника", true));
    private final BoolSetting others = add(new BoolSetting("Показывать косметику других", true));
    private final BoolSetting share = add(new BoolSetting("Делиться моим скином", true));

    public AquaNetwork() { super("Aqua Network", "Значок черепахи и косметика игроков Aqua Launcher", Category.COSMETIC); }

    public String url() { return url.get(); }
    public boolean icons() { return icons.isOn(); }
    public boolean showOthers() { return others.isOn(); }
    public boolean shareSkin() { return share.isOn(); }
    public void setShareSkin(boolean v) { share.set(v); }

    @Override public void onTick() { AquaNet.tick(); }
    @Override protected void onDisable() { AquaNet.clear(); }
}
