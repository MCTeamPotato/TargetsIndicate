package me.kall.targetsindicate.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RenderConfig {
    public enum Side { LEFT, RIGHT }

    public static final ModConfigSpec CONFIG;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.EnumValue<Side> SIDE;
    public static final ModConfigSpec.IntValue MAX_ENTRIES;
    public static final ModConfigSpec.IntValue ENTRY_LIFETIME;

    public static final ModConfigSpec.IntValue BAR_WIDTH;
    public static final ModConfigSpec.IntValue BAR_HEIGHT;
    public static final ModConfigSpec.IntValue ICON_SIZE;
    public static final ModConfigSpec.IntValue ROW_SPACING;
    public static final ModConfigSpec.IntValue TOP_MARGIN;
    public static final ModConfigSpec.IntValue EDGE_MARGIN;

    public static final ModConfigSpec.DoubleValue TOP_SCALE;
    public static final ModConfigSpec.DoubleValue SUB_SCALE;

    public static final ModConfigSpec.IntValue BG_COLOR;
    public static final ModConfigSpec.IntValue HEALTH_HIGH_COLOR;
    public static final ModConfigSpec.IntValue HEALTH_MID_COLOR;
    public static final ModConfigSpec.IntValue HEALTH_LOW_COLOR;

    public static final ModConfigSpec.IntValue HOVER_BG_COLOR;
    public static final ModConfigSpec.IntValue HOVER_TEXT_COLOR;

    public static final ModConfigSpec.BooleanValue RENDER_ENTITY_ICON;

    public static final ModConfigSpec.BooleanValue PRIORITIZE_LOOKED_AT;

    public static final ModConfigSpec.BooleanValue HOVER_ENABLED;
    public static final ModConfigSpec.DoubleValue HOVER_MAX_DISTANCE;

    public static final ModConfigSpec.BooleanValue RENDER_HEALTH, RENDER_PRECENT;

    public static final ModConfigSpec.BooleanValue FONT_SHADOW;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder
                .comment("是否启用伤害血条显示", "Whether to enable the damage health bar display")
                .define("enabled", true);
        RENDER_ENTITY_ICON = builder
                .comment("是否渲染生物实体模型图标（较耗性能）", "Whether to render the entity model icon (that's expensive regarding performance)")
                .define("renderEntityModel", false);
        SIDE = builder
                .comment("显示在屏幕的哪一侧：LEFT / RIGHT", "Which side of the screen to display: LEFT / RIGHT")
                .defineEnum("side", Side.LEFT);
        MAX_ENTRIES = builder
                .comment("最多同时堆叠显示多少个目标", "Maximum number of targets displayed at once")
                .defineInRange("maxEntries", 3, 1, 10);
        ENTRY_LIFETIME = builder
                .comment("目标多久没有再次受到伤害后开始淡出消失（秒）", "Time in seconds after which an undamaged target starts fading out")
                .defineInRange("entryLifetime", 12, 0, Integer.MAX_VALUE);
        PRIORITIZE_LOOKED_AT = builder
                .comment("开启后，如果玩家当前视线指向的生物已经在列表中，该目标会被强制提升为第一位（主目标位）", "When enabled, if the entity currently under the player's line of sight is already tracked, it is promoted to the top (main) slot")
                .define("prioritizeLookedAtTarget", true);
        HOVER_ENABLED = builder
                .comment("是否显示视线指向但尚未被追踪目标的预览信息框", "Whether to show the preview info box for a line-of-sight target that is not yet tracked")
                .define("hoverPreviewEnabled", true);
        HOVER_MAX_DISTANCE = builder
                .comment("预览未追踪目标时，视线检测的最大距离（方块，未被遮挡即可命中）", "Maximum line-of-sight distance (blocks) for previewing an untracked target, as long as it is not obstructed")
                .defineInRange("hoverMaxDistance", 32.0, 1.0, 128.0);
        RENDER_HEALTH = builder.comment("是否在信息框生物名称后面渲染生物血量和最大血量。", "Whether to render health and max health after name.")
                .define("renderHealthAndMaxHealth", true);
        RENDER_PRECENT = builder.comment("是否在信息框生物名称后渲染生物血量百分比", "Whether to render health percentage after name.")
                .define("renderHealthPercent", true);
        FONT_SHADOW = builder
                .comment("信息框文字是否渲染阴影", "Whether the info box text is rendered with a shadow.")
                .define("fontShadow", true);
        builder.pop();

        builder.push("layout");
        BAR_WIDTH = builder
                .comment("血条宽度（像素，基准缩放下）", "Health bar width (pixels, at base scale)")
                .defineInRange("barWidth", 56, 40, 300);
        BAR_HEIGHT = builder
                .comment("血条高度（像素，基准缩放下）", "Health bar height (pixels, at base scale)")
                .defineInRange("barHeight", 3, 2, 30);
        ICON_SIZE = builder
                .comment("生物模型尺寸（像素，基准缩放下）", "Entity icon size (pixels, at base scale)")
                .defineInRange("modelSize", 16, 8, 64);
        ROW_SPACING = builder
                .comment("列表中每一行之间的额外间距（像素）", "Extra spacing between each row in the list (pixels)")
                .defineInRange("rowSpacing", 6, 0, 40);
        TOP_MARGIN = builder
                .comment("列表顶部与屏幕顶部的距离（像素）", "Distance from the top of the list to the top of the screen (pixels)")
                .defineInRange("topMargin", 70, 0, 500);
        EDGE_MARGIN = builder
                .comment("列表与屏幕左/右边缘的距离（像素）", "Distance from the list to the left/right screen edge (pixels)")
                .defineInRange("edgeMargin", 8, 0, 200);
        TOP_SCALE = builder
                .comment("主目标（排在最上方）的显示缩放比例", "Display scale for the main target (top entry)")
                .defineInRange("topScale", 1.15, 0.5, 3.0);
        SUB_SCALE = builder
                .comment("副目标的显示缩放比例", "Display scale for the remaining targets")
                .defineInRange("subScale", 0.8, 0.3, 1.0);
        builder.pop();

        builder.push("color");
        BG_COLOR = builder
                .comment("背景颜色 ARGB", "The background color of the box ARGB")
                .defineInRange("backgroundColor", 0xC0101010, Integer.MIN_VALUE, Integer.MAX_VALUE);
        HEALTH_HIGH_COLOR = builder
                .comment("血量充足时的颜色 ARGB", "Color when health percentage is high ARGB")
                .defineInRange("healthHighColor", 0xFF55D65A, Integer.MIN_VALUE, Integer.MAX_VALUE);
        HEALTH_MID_COLOR = builder
                .comment("血量中等时的颜色 ARGB", "Color when health percentage is medium ARGB")
                .defineInRange("healthMidColor", 0xFFE6C13A, Integer.MIN_VALUE, Integer.MAX_VALUE);
        HEALTH_LOW_COLOR = builder
                .comment("血量较低时的颜色 ARGB", "Color when health percentage is low ARGB")
                .defineInRange("healthLowColor", 0xFFE04B3B, Integer.MIN_VALUE, Integer.MAX_VALUE);
        HOVER_BG_COLOR = builder
                .comment("视线指向但尚未被追踪的目标，其预览信息框的背景颜色 ARGB（默认：半透明浅灰色）", "Background color ARGB of the preview box for a line-of-sight target that is not yet tracked (default: semi-transparent light gray)")
                .defineInRange("hoverPreviewBackgroundColor", 0x60CCCCCC, Integer.MIN_VALUE, Integer.MAX_VALUE);
        HOVER_TEXT_COLOR = builder
                .comment("视线指向但尚未被追踪的目标，其预览信息框的文字与血条颜色 ARGB（默认：灰色）", "Text and health bar color ARGB of the preview box for a line-of-sight target that is not yet tracked (default: gray)")
                .defineInRange("hoverPreviewTextColor", 0xFFAAAAAA, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

        CONFIG = builder.build();
    }
}