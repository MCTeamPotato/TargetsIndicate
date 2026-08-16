package me.kall.targetsindicate.data;

public class HoverPreviewState {
    public int entityId = -1;
    public String name = "";

    public float maxHealth = 1.0F;
    public float displayedHealth = 1.0F;
    public float displayedHealthRatio = 1.0F;

    public boolean active = false;
    public boolean initialized = false;

    public float currentX, currentY;
    public float targetX, targetY;
    public float currentScale = 0.8F;
    public float targetScale = 0.8F;
    public float alpha = 0.0F;
}