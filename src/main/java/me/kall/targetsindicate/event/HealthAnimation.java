package me.kall.targetsindicate.event;

import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.kall.targetsindicate.TargetsIndicate;
import me.kall.targetsindicate.config.RenderConfig;
import me.kall.targetsindicate.data.HoverPreviewState;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

import static me.kall.targetsindicate.event.DamageDetection.ORDER;
import static me.kall.targetsindicate.event.DamageDetection.TARGET_STATES;

@SuppressWarnings("UnnecessaryLocalVariable")
@EventBusSubscriber(modid = TargetsIndicate.MOD_ID, value = Dist.CLIENT)
public class HealthAnimation {
    private static boolean tickConsumed = false;
    private static long lastFrameNanos = System.nanoTime();

    private static final int PADDING = 4;
    private static final int GAP = 3;
    private static final IntArrayList TO_REMOVE = new IntArrayList();

    private static final HoverPreviewState HOVER = new HoverPreviewState();

    @SubscribeEvent
    public static void nextFrame(TickEvent.RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) tickConsumed = false;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (tickConsumed) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (!RenderConfig.ENABLED.get() || minecraft.options.hideGui || player == null || minecraft.level == null) return;

        long now = System.nanoTime();
        float delta = Math.min((now - lastFrameNanos) / 1_000_000_000.0F, 0.1F);
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
        int bgColor = RenderConfig.BG_COLOR.get();
        boolean renderHealth = RenderConfig.RENDER_HEALTH.get();
        boolean renderPercent = RenderConfig.RENDER_PRECENT.get();
        boolean fontShadow = RenderConfig.FONT_SHADOW.get();
        int hoverBgColor = RenderConfig.HOVER_BG_COLOR.get();
        int hoverTextColor = RenderConfig.HOVER_TEXT_COLOR.get();
        boolean left = side == RenderConfig.Side.LEFT;
        int iconAreaWidth = showIcon ? iconSize : 0;
        int iconGap = showIcon ? PADDING : 0;

        Font font = minecraft.font;
        int textLineHeight = font.lineHeight;
        int nominalBoxHeight = PADDING * 2 + Math.max(iconSize, textLineHeight + GAP + barHeight);

        float[] rowCenterY = new float[maxEntries];
        float cursor = topMargin;
        for (int i = 0; i < maxEntries; i++) {
            float scale = (i == 0) ? topScale : subScale;
            cursor += nominalBoxHeight * scale / 2f;
            rowCenterY[i] = cursor;
            cursor += nominalBoxHeight * scale / 2f + rowSpacing;
        }

        LivingEntity crosshairEntity = null;
        if (RenderConfig.HOVER_ENABLED.get()) {
            int lookedAtEntityId = LookAtProvider.get();
            if (lookedAtEntityId != -1) {
                Entity looked = minecraft.level.getEntity(lookedAtEntityId);
                if (looked instanceof LivingEntity livingLooked && livingLooked.isAlive()) {
                    crosshairEntity = livingLooked;
                }
            }
        }

        int crosshairId = crosshairEntity != null ? crosshairEntity.getId() : -1;

        int lookedAtId = RenderConfig.PRIORITIZE_LOOKED_AT.get() ? crosshairId : -1;
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

        TargetState crosshairState = crosshairId != -1 ? TARGET_STATES.get(crosshairId) : null;
        boolean crosshairAlreadyShown = crosshairState != null && crosshairState.alive;
        boolean showHover = crosshairEntity != null && !crosshairAlreadyShown;

        int handoffEntityId = -1;
        if (crosshairAlreadyShown && HOVER.active && HOVER.entityId == crosshairId) {
            handoffEntityId = crosshairId;
        }

        if (showHover) {
            if (HOVER.entityId != crosshairId) {
                HOVER.entityId = crosshairId;
                HOVER.initialized = false;
            }
            HOVER.name = crosshairEntity.getDisplayName().getString();
            HOVER.maxHealth = crosshairEntity.getMaxHealth();
            HOVER.displayedHealth = crosshairEntity.getHealth();
            HOVER.displayedHealthRatio = HOVER.maxHealth > 0 ? clamp(HOVER.displayedHealth / HOVER.maxHealth) : 0f;
            HOVER.active = true;
        } else {
            HOVER.active = false;
        }

        Window window = minecraft.getWindow();
        int guiScaledWidth = window.getGuiScaledWidth();
        float alphaDamp = (float) Math.exp(-10.0 * delta);
        float slideDamp = (float) Math.exp(-14.0 * delta);
        float posDamp = (float) Math.exp(-12.0 * delta);
        float scaleDamp = (float) Math.exp(-14.0 * delta);
        float hiddenSlide = left ? -40f : 40f;

        float hoverScale = subScale;
        float hoverBoxHeightEstimate = nominalBoxHeight * hoverScale;
        float hoverTargetX = left ? edgeMargin : (guiScaledWidth - edgeMargin);
        float hoverTargetY = topMargin - rowSpacing - hoverBoxHeightEstimate / 2f;

        HOVER.targetX = hoverTargetX;
        HOVER.targetY = hoverTargetY;
        HOVER.targetScale = hoverScale;

        float hoverTargetAlpha = HOVER.active ? 1.0f : 0.0f;
        HOVER.alpha += (hoverTargetAlpha - HOVER.alpha) * (1.0f - alphaDamp);
        HOVER.currentScale += (HOVER.targetScale - HOVER.currentScale) * (1.0f - scaleDamp);
        HOVER.currentY += (HOVER.targetY - HOVER.currentY) * (1.0f - posDamp);
        HOVER.currentX += (HOVER.targetX - HOVER.currentX) * (1.0f - posDamp);

        if (!HOVER.initialized) {
            HOVER.currentX = HOVER.targetX;
            HOVER.currentY = HOVER.targetY;
            HOVER.currentScale = HOVER.targetScale;
            HOVER.initialized = true;
        }

        float handoffX = 0, handoffY = 0, handoffScale = hoverScale, handoffAlpha = 0;
        if (handoffEntityId != -1) {
            handoffX = HOVER.currentX;
            handoffY = HOVER.currentY;
            handoffScale = HOVER.currentScale;
            handoffAlpha = HOVER.alpha;

            HOVER.active = false;
            HOVER.alpha = 0f;
            HOVER.initialized = false;
            HOVER.entityId = -1;
        }

        List<TargetState> visible = new ArrayList<>();
        TO_REMOVE.clear();

        for (var entry : TARGET_STATES.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            TargetState state = entry.getValue();

            int clampedRank = Math.min(maxEntries - 1, Math.max(state.rank, 0));
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
            state.spawnBlend += (0f - state.spawnBlend) * (1.0f - scaleDamp);

            state.displayedHealth += (state.health - state.displayedHealth) * (1.0f - posDamp);
            float targetRatio = clamp(state.health / state.maxHealth);
            state.displayedHealthRatio += (targetRatio - state.displayedHealthRatio) * (1.0f - posDamp);

            if (!state.initialized) {
                if (id == handoffEntityId) {
                    state.currentX = handoffX;
                    state.currentY = handoffY;
                    state.currentScale = handoffScale;
                    state.slideX = 0f;
                    state.alpha = handoffAlpha;
                    state.spawnBlend = 1.0f;
                } else {
                    state.currentY = targetY;
                    state.currentX = state.targetX;
                    state.currentScale = targetScale;
                    state.slideX = targetSlide;
                }
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

        int guiScaledHeight = window.getGuiScaledHeight();
        MouseHandler mouse = minecraft.mouseHandler;
        double scaleX = (double) guiScaledWidth / window.getScreenWidth();
        double scaleY = (double) guiScaledHeight / window.getScreenHeight();
        int mouseX = (int) (mouse.xpos() * scaleX);
        int mouseY = (int) (mouse.ypos() * scaleY);

        GuiGraphics graphics = event.getGuiGraphics();

        for (TargetState state : visible) {
            StringBuilder displayed = new StringBuilder(state.name);
            if (renderHealth) displayed.append(" | ").append(Math.round(state.displayedHealth)).append("/").append(Math.round(state.maxHealth));
            if (renderPercent) displayed.append(" | ").append(Math.round(state.displayedHealthRatio * 100)).append("%");
            String displayName = displayed.toString();

            int nameWidth = font.width(displayName);
            int contentWidth = iconAreaWidth + iconGap + Math.max(nameWidth, barWidth);
            int boxWidth = contentWidth + PADDING * 2;
            int boxHeight = nominalBoxHeight;

            float anchorX = state.currentX + state.slideX;
            float anchorY = state.currentY;
            float localLeftFloat = left ? 0 : -boxWidth;
            float localTopFloat = -boxHeight / 2f;

            int screenIconX = (int) (anchorX + (localLeftFloat + PADDING) * state.currentScale);
            int screenIconY = (int) (anchorY + (localTopFloat + (boxHeight - iconSize) / 2f) * state.currentScale);
            int screenIconX2 = screenIconX + iconSize;
            int screenIconY2 = screenIconY + iconSize;

            Entity entity = minecraft.level != null ? minecraft.level.getEntity(state.entityId) : null;

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(anchorX, anchorY, 0);
            pose.scale(state.currentScale, state.currentScale, 1f);

            int localLeft = (int) localLeftFloat;
            int localTop = (int) localTopFloat;

            int effectiveBg = state.spawnBlend > 0.001f ? lerpColorARGB(bgColor, hoverBgColor, state.spawnBlend) : bgColor;
            graphics.fill(RenderType.guiOverlay(), localLeft, localTop, localLeft + boxWidth, localTop + boxHeight, applyAlpha(effectiveBg, state.alpha));

            int textX = localLeft + PADDING + iconAreaWidth + iconGap;
            int textY = localTop + PADDING;
            int effectiveTextColor = state.spawnBlend > 0.001f ? lerpColorARGB(0xFFFFFFFF, hoverTextColor, state.spawnBlend) : 0xFFFFFFFF;
            graphics.drawString(font, displayName, textX, textY, applyAlpha(effectiveTextColor, state.alpha), fontShadow);

            int barX = textX;
            int barY = textY + font.lineHeight + GAP;
            float ratio = state.displayedHealthRatio;
            int fillWidth = Math.round(barWidth * clamp(ratio));
            if (fillWidth > 0) {
                int high = RenderConfig.HEALTH_HIGH_COLOR.get();
                int mid = RenderConfig.HEALTH_MID_COLOR.get();
                int low = RenderConfig.HEALTH_LOW_COLOR.get();
                int healthColor = ratio > 0.5f ? lerpColor(mid, high, (ratio - 0.5f) / 0.5f) : lerpColor(low, mid, ratio / 0.5f);
                int barColorBase = state.spawnBlend > 0.001f ? lerpColorARGB(healthColor, hoverTextColor, state.spawnBlend) : healthColor;
                graphics.fill(RenderType.guiOverlay(), barX, barY, barX + fillWidth, barY + barHeight, applyAlpha(barColorBase, state.alpha));
            }

            pose.popPose();

            if (showIcon && entity instanceof LivingEntity livingEntity) {
                int iconCenterX = (screenIconX + screenIconX2) / 2;
                int iconCenterY = (screenIconY + screenIconY2) / 2;
                int renderScale = (int) ((float) iconSize / 2 * state.currentScale);
                int renderY = iconCenterY + Math.round(renderScale * (0.0625F + livingEntity.getBbHeight() / 2.0F));

                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics,
                        iconCenterX,
                        renderY,
                        renderScale,
                        iconCenterX - mouseX,
                        iconCenterY - mouseY,
                        livingEntity
                );
            }
        }

        if (HOVER.alpha > 0.01f) {
            StringBuilder hoverDisplayed = new StringBuilder(HOVER.name);
            if (renderHealth) hoverDisplayed.append(" | ").append(Math.round(HOVER.displayedHealth)).append("/").append(Math.round(HOVER.maxHealth));
            if (renderPercent) hoverDisplayed.append(" | ").append(Math.round(HOVER.displayedHealthRatio * 100)).append("%");
            String hoverName = hoverDisplayed.toString();

            int hoverNameWidth = font.width(hoverName);
            int hoverContentWidth = iconAreaWidth + iconGap + Math.max(hoverNameWidth, barWidth);
            int hoverBoxWidth = hoverContentWidth + PADDING * 2;
            int hoverBoxHeight = nominalBoxHeight;

            float hAnchorX = HOVER.currentX;
            float hAnchorY = HOVER.currentY;
            float hLocalLeftFloat = left ? 0 : -hoverBoxWidth;
            float hLocalTopFloat = -hoverBoxHeight / 2f;

            int hScreenIconX = (int) (hAnchorX + (hLocalLeftFloat + PADDING) * HOVER.currentScale);
            int hScreenIconY = (int) (hAnchorY + (hLocalTopFloat + (hoverBoxHeight - iconSize) / 2f) * HOVER.currentScale);
            int hScreenIconX2 = hScreenIconX + iconSize;
            int hScreenIconY2 = hScreenIconY + iconSize;

            Entity hoverEntity = minecraft.level != null ? minecraft.level.getEntity(HOVER.entityId) : null;

            var hoverPose = graphics.pose();
            hoverPose.pushPose();
            hoverPose.translate(hAnchorX, hAnchorY, 0);
            hoverPose.scale(HOVER.currentScale, HOVER.currentScale, 1f);

            int hLocalLeft = (int) hLocalLeftFloat;
            int hLocalTop = (int) hLocalTopFloat;

            graphics.fill(RenderType.guiOverlay(), hLocalLeft, hLocalTop, hLocalLeft + hoverBoxWidth, hLocalTop + hoverBoxHeight, applyAlpha(hoverBgColor, HOVER.alpha));

            int hTextX = hLocalLeft + PADDING + iconAreaWidth + iconGap;
            int hTextY = hLocalTop + PADDING;
            graphics.drawString(font, hoverName, hTextX, hTextY, applyAlpha(hoverTextColor, HOVER.alpha), fontShadow);

            int hBarX = hTextX;
            int hBarY = hTextY + font.lineHeight + GAP;
            int hFillWidth = Math.round(barWidth * clamp(HOVER.displayedHealthRatio));
            if (hFillWidth > 0) {
                graphics.fill(RenderType.guiOverlay(), hBarX, hBarY, hBarX + hFillWidth, hBarY + barHeight, applyAlpha(hoverTextColor, HOVER.alpha * 0.8f));
            }

            hoverPose.popPose();

            if (showIcon && hoverEntity instanceof LivingEntity hoverLiving) {
                int hoverIconCenterX = (hScreenIconX + hScreenIconX2) / 2;
                int hoverIconCenterY = (hScreenIconY + hScreenIconY2) / 2;
                int hoverRenderScale = (int) ((float) iconSize / 2 * HOVER.currentScale);
                int hoverRenderY = hoverIconCenterY + Math.round(hoverRenderScale * (0.0625F + hoverLiving.getBbHeight() / 2.0F));

                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics,
                        hoverIconCenterX,
                        hoverRenderY,
                        hoverRenderScale,
                        hoverIconCenterX - mouseX,
                        hoverIconCenterY - mouseY,
                        hoverLiving
                );
            }
        }

        tickConsumed = true;
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

    private static int lerpColorARGB(int from, int to, float t) {
        t = clamp(t);
        int fa = (from >>> 24) & 0xFF, fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
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