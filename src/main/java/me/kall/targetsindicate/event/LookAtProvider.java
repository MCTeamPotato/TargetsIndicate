package me.kall.targetsindicate.event;

import me.kall.targetsindicate.TargetsIndicate;
import me.kall.targetsindicate.config.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = TargetsIndicate.MOD_ID, value = Dist.CLIENT)
public final class LookAtProvider {
    private static int id = -1;

    private LookAtProvider() {}

    public static int get() {
        return id;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.START)) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;

        if (player == null || level == null || !RenderConfig.ENABLED.get()) {
            id = -1;
            return;
        }

        LivingEntity result = findLookedAtEntity(level, player);
        id = result != null ? result.getId() : -1;
    }

    private static LivingEntity findLookedAtEntity(@NotNull Level level, @NotNull LocalPlayer player) {
        double maxDistance = RenderConfig.HOVER_MAX_DISTANCE.get();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 reachEnd = eyePos.add(look.scale(maxDistance));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0);

        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(player, searchBox, e -> e instanceof LivingEntity le && le.isAlive() && le.isPickable())) {
            LivingEntity living = (LivingEntity) entity;
            AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = hitBox.clip(eyePos, reachEnd);
            if (hit.isEmpty()) continue;

            double distSqr = eyePos.distanceToSqr(hit.get());
            if (distSqr >= bestDistSqr) continue;

            if (isBlocked(level, player, eyePos, hit.get())) continue;

            best = living;
            bestDistSqr = distSqr;
        }

        return best;
    }

    private static boolean isBlocked(@NotNull Level level, LocalPlayer player, Vec3 from, Vec3 to) {
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.MISS) return false;
        return from.distanceToSqr(blockHit.getLocation()) < from.distanceToSqr(to) - 1.0E-3;
    }
}