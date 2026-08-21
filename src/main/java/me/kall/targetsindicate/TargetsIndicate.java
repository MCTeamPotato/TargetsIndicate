package me.kall.targetsindicate;

import me.kall.targetsindicate.config.RenderConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;

@Mod(TargetsIndicate.MOD_ID)
public final class TargetsIndicate {
    public static final String MOD_ID = "targetsindicate";

    public TargetsIndicate(@NotNull FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, RenderConfig.CONFIG);
    }
}
