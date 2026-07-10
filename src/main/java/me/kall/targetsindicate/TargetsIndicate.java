package me.kall.targetsindicate;

import me.kall.targetsindicate.config.RenderConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

@Mod(value = TargetsIndicate.MOD_ID, dist = Dist.CLIENT)
public final class TargetsIndicate {
    public static final String MOD_ID = "targetsindicate";

    public TargetsIndicate(@NotNull ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, RenderConfig.CONFIG);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parentScreen) -> new ConfigurationScreen(container, parentScreen));
    }
}
