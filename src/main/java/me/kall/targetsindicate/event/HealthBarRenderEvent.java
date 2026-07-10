package me.kall.targetsindicate.event;

import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.kall.targetsindicate.TargetsIndicate;
import me.kall.targetsindicate.config.RenderConfig;
import me.kall.targetsindicate.data.TargetState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.kall.targetsindicate.event.DamageDetectEvent.ORDER;
import static me.kall.targetsindicate.event.DamageDetectEvent.TARGET_STATES;

@SuppressWarnings("UnnecessaryLocalVariable")
@EventBusSubscriber(modid = TargetsIndicate.MOD_ID, value = Dist.CLIENT)
public class HealthBarRenderEvent {
    private static boolean tickConsumed = false;
    private static long lastFrameNanos = System.nanoTime();

    private static final int PADDING = 4;
    private static final int GAP = 3;
    private static final IntArrayList TO_REMOVE = new IntArrayList();

    @SubscribeEvent
    public static void nextFrame(RenderFrameEvent.Pre event) {
        tickConsumed = false;
    }

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        if (tickConsumed) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (!RenderConfig.ENABLED.get() || minecraft.options.hideGui || player == null || minecraft.level == null || TARGET_STATES.isEmpty()) return;

        long now = System.nanoTime();
        float delta = Math.min((float) ((now - lastFrameNanos) / 1_000_000_000.0), 0.1F);
        lastFrameNanos = now;

        RenderConfig.Side side = RenderConfig.SIDE.get();
        int maxEntries = RenderConfig.MAX_ENTRIES.get();
        int iconSize = RenderConfig.ICON_SIZE.get();
        int barWidth = RenderConfig.BAR_WIDTH.get();
        int barHeight = RenderConfig.BAR_HEIGHT.get();
        int edgeMargin = RenderConfig.EDGE_MARGIN.get();
        int topMargin = RenderConfig.TOP_MARGIN.get();
        int rowSpacing = RenderConfig.ROW_SPACING.get();
        float topScale = RenderConfig.TOP_SCALE.get().floatValue();
        float subScale = RenderConfig.SUB_SCALE.get().floatValue();
        boolean showIcon = RenderConfig.RENDER_ENTITY_ICON.get();
        int iconAreaWidth = showIcon ? iconSize : 0;
        int iconGap = showIcon ? PADDING : 0;

        Font font = minecraft.font;
        int textLineHeight = font.lineHeight;
        int nominalBoxHeight = PADDING * 2 + Math.max(iconSize, textLineHeight + GAP + barHeight);

        float[] rowCenterY = computeRowCenters(topMargin, nominalBoxHeight, maxEntries, topScale, subScale, rowSpacing);

        int lookedAtId = -1;
        if (RenderConfig.PRIORITIZE_LOOKED_AT.get()) {
            HitResult hitResult = minecraft.hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity) {
                lookedAtId = entityHitResult.getEntity().getId();
            }
        }
        rank(lookedAtId, maxEntries);

        Window window = minecraft.getWindow();
        int guiScaledWidth = window.getGuiScaledWidth();
        List<TargetState> visible = prepareTargets(delta, side, maxEntries, edgeMargin, guiScaledWidth, rowCenterY, topScale, subScale);

        int guiScaledHeight = window.getGuiScaledHeight();
        MouseHandler mouse = minecraft.mouseHandler;
        double scaleX = (double) guiScaledWidth / window.getScreenWidth();
        double scaleY = (double) guiScaledHeight / window.getScreenHeight();
        int mouseX = (int) (mouse.xpos() * scaleX);
        int mouseY = (int) (mouse.ypos() * scaleY);

        GuiGraphics graphics = event.getGuiGraphics();
        int bgColor = RenderConfig.BG_COLOR.get();
        boolean renderHealth = RenderConfig.RENDER_HEALTH.get();
        boolean renderPercent = RenderConfig.RENDER_PRECENT.get();
        boolean left = side == RenderConfig.Side.LEFT;

        for (TargetState state : visible) {
            draw(graphics, state, font, iconSize, barWidth, barHeight, iconAreaWidth, iconGap, textLineHeight, mouseX, mouseY, bgColor, showIcon, renderHealth, renderPercent, left, minecraft);
        }

        tickConsumed = true;
    }

    @Contract(pure = true)
    private static float @NotNull [] computeRowCenters(int topMargin, int nominalBoxHeight, int maxEntries, float topScale, float subScale, int rowSpacing) {
        float[] centers = new float[maxEntries];
        float cursor = topMargin;
        for (int i = 0; i < maxEntries; i++) {
            float scale = (i == 0) ? topScale : subScale;
            cursor += nominalBoxHeight * scale / 2f;
            centers[i] = cursor;
            cursor += nominalBoxHeight * scale / 2f + rowSpacing;
        }
        return centers;
    }

    private static void rank(int lookedAtId, int maxEntries) {
        TargetState lookedAtState = (lookedAtId != -1) ? TARGET_STATES.get(lookedAtId) : null;
        boolean hasLookedPriority = lookedAtState != null && lookedAtState.alive;

        int aliveRank = 0;
        if (hasLookedPriority) {
            lookedAtState.rank = 0;
            aliveRank = 1;
        }

        for (int i = 0; i < ORDER.size(); i++) {
            int id = ORDER.getInt(i);
            if (hasLookedPriority && id == lookedAtId) continue;

            TargetState state = TARGET_STATES.get(id);
            if (state == null) continue;
            if (state.alive) {
                if (aliveRank < maxEntries) {
                    state.rank = aliveRank;
                    aliveRank++;
                } else {
                    state.alive = false;
                }
            }
        }
    }

    private static @NotNull List<TargetState> prepareTargets(float delta, RenderConfig.Side side, int maxEntries, int edgeMargin, int guiScaledWidth, float[] rowCenterY, float topScale, float subScale) {
        float alphaDamp = (float) Math.exp(-10.0 * delta);
        float slideDamp = (float) Math.exp(-14.0 * delta);
        float posDamp = (float) Math.exp(-12.0 * delta);
        float scaleDamp = (float) Math.exp(-14.0 * delta);
        boolean left = side == RenderConfig.Side.LEFT;
        float hiddenSlide = left ? -40f : 40f;

        List<TargetState> visible = new ArrayList<>();
        TO_REMOVE.clear();

        for (var entry : TARGET_STATES.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            TargetState state = entry.getValue();

            int clampedRank = Math.clamp(state.rank, 0, maxEntries - 1);
            float targetScale = clampedRank == 0 ? topScale : subScale;
            float targetY = rowCenterY[clampedRank];
            float targetAlpha = state.alive ? 1.0f : 0.0f;

            if (state.deathTime > 0 && state.alive) {
                long nowMillis = System.currentTimeMillis();
                if (state.displayedHealthRatio < 0.005F || nowMillis - state.deathTime > 1500L) {
                    state.alive = false;
                    targetAlpha = 0.0f;
                }
            }

            float targetSlide = state.alive ? 0.0f : hiddenSlide;

            state.targetY = targetY;
            state.targetScale = targetScale;
            state.targetX = left ? edgeMargin : (guiScaledWidth - edgeMargin);

            state.alpha += (targetAlpha - state.alpha) * (1.0f - alphaDamp);
            state.slideX += (targetSlide - state.slideX) * (1.0f - slideDamp);
            state.currentScale += (targetScale - state.currentScale) * (1.0f - scaleDamp);
            state.currentY += (targetY - state.currentY) * (1.0f - posDamp);
            state.currentX += (state.targetX - state.currentX) * (1.0f - posDamp);

            state.displayedHealth += (state.health - state.displayedHealth) * (1.0f - posDamp);
            float targetRatio = clamp(state.health / state.maxHealth);
            state.displayedHealthRatio += (targetRatio - state.displayedHealthRatio) * (1.0f - posDamp);

            if (!state.initialized) {
                state.currentY = targetY;
                state.currentX = state.targetX;
                state.currentScale = targetScale;
                state.slideX = targetSlide;
                state.displayedHealthRatio = targetRatio;
                state.displayedHealth = state.health;
                state.initialized = true;
            }

            if (!state.alive && state.alpha < 0.01f) {
                TO_REMOVE.add(id);
                continue;
            }
            if (state.alpha > 0.01f) visible.add(state);
        }

        for (int i = 0; i < TO_REMOVE.size(); i++) {
            int id = TO_REMOVE.getInt(i);
            TARGET_STATES.remove(id);
            ORDER.rem(id);
        }

        visible.sort((a, b) -> Integer.compare(b.rank, a.rank));
        return visible;
    }

    private static void draw(GuiGraphics graphics, @NotNull TargetState state, Font font, int iconSize, int barWidth, int barHeight, int iconAreaWidth, int iconGap, int textLineHeight, int mouseX, int mouseY, int bgColor, boolean showIcon, boolean renderHealth, boolean renderPercent, boolean left, Minecraft minecraft) {
        StringBuilder displayed = new StringBuilder(state.name);
        if (renderHealth) displayed.append(" | ").append(Math.round(state.displayedHealth)).append("/").append(Math.round(state.maxHealth));
        if (renderPercent) displayed.append(" | ").append(Math.round(state.displayedHealthRatio * 100)).append("%");
        String displayName = displayed.toString();

        int nameWidth = font.width(displayName);
        int contentWidth = iconAreaWidth + iconGap + Math.max(nameWidth, barWidth);
        int boxWidth = contentWidth + PADDING * 2;
        int boxHeight = PADDING * 2 + Math.max(iconSize, textLineHeight + GAP + barHeight);

        float anchorX = state.currentX + state.slideX;
        float anchorY = state.currentY;
        float localLeftFloat = left ? 0 : -boxWidth;
        float localTopFloat = -boxHeight / 2f;

        int screenIconX = (int) (anchorX + localLeftFloat + PADDING);
        int screenIconY = (int) (anchorY + localTopFloat + (boxHeight - iconSize) / 2f);
        int screenIconX2 = screenIconX + iconSize;
        int screenIconY2 = screenIconY + iconSize;

        Entity entity = minecraft.level != null ? minecraft.level.getEntity(state.entityId) : null;

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX, anchorY, 0);
        pose.scale(state.currentScale, state.currentScale, 1f);

        int localLeft = (int) localLeftFloat;
        int localTop = (int) localTopFloat;

        int drawBg = applyAlpha(bgColor, state.alpha);
        graphics.fill(RenderType.guiOverlay(), localLeft, localTop, localLeft + boxWidth, localTop + boxHeight, drawBg);

        int textX = localLeft + PADDING + iconAreaWidth + iconGap;
        int textY = localTop + PADDING;
        int textColor = applyAlpha(0xFFFFFFFF, state.alpha);
        graphics.drawString(font, displayName, textX, textY, textColor, true);

        int barX = textX;
        int barY = textY + font.lineHeight + GAP;
        float ratio = state.displayedHealthRatio;
        float alpha = state.alpha;
        int fillWidth = Math.round(barWidth * clamp(ratio));
        if (fillWidth > 0) {
            int high = RenderConfig.HEALTH_HIGH_COLOR.get();
            int mid = RenderConfig.HEALTH_MID_COLOR.get();
            int low = RenderConfig.HEALTH_LOW_COLOR.get();
            int barColor = applyAlpha(ratio > 0.5f ? lerpColor(mid, high, (ratio - 0.5f) / 0.5f) : lerpColor(low, mid, ratio / 0.5f), alpha);
            graphics.fill(RenderType.guiOverlay(), barX, barY, barX + fillWidth, barY + barHeight, barColor);
        }

        pose.popPose();

        if (showIcon && entity instanceof LivingEntity livingEntity) InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, screenIconX, screenIconY, screenIconX2, screenIconY2, (int) ((float) iconSize / 2 * state.currentScale), 0.0625f, mouseX, mouseY, livingEntity);
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp(t);
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int applyAlpha(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int a = Math.round(baseAlpha * clamp(alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (Math.min(v, 1f));
    }
}