package me.kall.targetsindicate.event;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.kall.targetsindicate.TargetsIndicate;
import me.kall.targetsindicate.config.RenderConfig;
import me.kall.targetsindicate.data.TargetState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = TargetsIndicate.MOD_ID, value = Dist.CLIENT)
public class DamageDetectEvent {
    public static final Int2ObjectOpenHashMap<TargetState> TARGET_STATES = new Int2ObjectOpenHashMap<>();
    public static final IntArrayList ORDER = new IntArrayList();

    private static final Int2FloatOpenHashMap WATCH_HEALTH = new Int2FloatOpenHashMap();
    private static final Int2LongOpenHashMap WATCH_SINCE = new Int2LongOpenHashMap();

    private static final long WATCH_TIMEOUT_MS = 8000L;
    private static final float HEALTH_EPSILON = 0.01F;

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.isClientSide()) return;
        if (minecraft.player == null || event.getEntity() != minecraft.player) return;

        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;

        int id = living.getId();
        WATCH_HEALTH.putIfAbsent(id, living.getHealth());
        WATCH_SINCE.put(id, System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void nextFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null || !RenderConfig.ENABLED.get()) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();

        detect(minecraft.level, now);
        expire(now, RenderConfig.ENTRY_LIFETIME.get().longValue() * 1000L);
    }

    private static void clear() {
        WATCH_HEALTH.clear();
        WATCH_SINCE.clear();
        for (TargetState state : TARGET_STATES.values()) {
            state.alive = false;
        }
    }

    private static void detect(Level level, long now) {
        IntArrayList toRemove = new IntArrayList();
        for (int id : new IntArrayList(WATCH_HEALTH.keySet())) {
            if (canRemove(id, level, now)) {
                toRemove.add(id);
            }
        }

        for (int i = 0; i < toRemove.size(); i++) {
            int id = toRemove.getInt(i);
            WATCH_HEALTH.remove(id);
            WATCH_SINCE.remove(id);
        }
    }

    private static boolean canRemove(int id, @NotNull Level level, long now) {
        Entity entity = level.getEntity(id);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            markDead(id);
            return true;
        }
        trackDamage(living, id, now);
        long since = WATCH_SINCE.getOrDefault(id, now);
        return now - since > WATCH_TIMEOUT_MS;
    }

    private static void markDead(int id) {
        TargetState state = TARGET_STATES.get(id);
        if (state != null && state.alive) {
            state.health = 0F;
            state.deathTime = System.currentTimeMillis();
            state.lastHitTimeMillis = Long.MAX_VALUE;
        }
    }

    private static void trackDamage(@NotNull LivingEntity living, int id, long now) {
        float last = WATCH_HEALTH.get(id);
        float current = living.getHealth();

        if (current < last - HEALTH_EPSILON) {
            updateState(living, now);
        } else if (Math.abs(current - last) > HEALTH_EPSILON) {
            TargetState state = TARGET_STATES.get(id);
            if (state != null) state.health = current;
        }
        WATCH_HEALTH.put(id, current);
    }

    private static void updateState(@NotNull LivingEntity living, long now) {
        int id = living.getId();
        TargetState state = TARGET_STATES.get(id);
        if (state == null) {
            state = new TargetState();
            state.entityId = id;
            TARGET_STATES.put(id, state);
        }
        state.name = living.getDisplayName().getString();
        state.maxHealth = living.getMaxHealth();
        state.health = living.getHealth();
        state.lastHitTimeMillis = now;
        state.alive = true;

        ORDER.rem(id);
        ORDER.add(0, id);
        while (ORDER.size() > Math.max(RenderConfig.MAX_ENTRIES.get() * 3, 12)) {
            ORDER.removeInt(ORDER.size() - 1);
        }
        WATCH_SINCE.put(id, now);
    }

    private static void expire(long now, long lifetimeMs) {
        for (TargetState state : TARGET_STATES.values()) {
            if (state.alive && now - state.lastHitTimeMillis > lifetimeMs) {
                state.alive = false;
            }
        }
    }
}