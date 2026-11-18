package dev.aqua.pvpbot;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Aqua PvP Bot - PvP practice bots for Minecraft. Made by Ulian Voskoboynikov. */
public class AquaPvPBot implements ModInitializer {
    public static final String MOD_ID = "aquapvpbot";
    public static final Logger LOG = LoggerFactory.getLogger("Aqua PvP Bot");

    @Override
    public void onInitialize() {
        LOG.info("Aqua PvP Bot loaded - /spawnbot <easy|normal|hard|endless|impossible>, /kit <sword|diamond|op|nethpot|crystal|mace>");
    }
}
