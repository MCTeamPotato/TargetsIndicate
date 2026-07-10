package me.kall.targetsindicate.data;

public class TargetState {
    public int entityId;
    public String name = "";

    public float maxHealth = 1.0F;
    public float health = 1.0F;

    public long deathTime = 0;

    public float displayedHealthRatio = 1.0F;
    public float displayedHealth = 1.0F;

    public long lastHitTimeMillis;

    public boolean alive = true;

    public boolean initialized = false;

    public int rank = 0;

    public float alpha = 0.0F;
    public float slideX = 40.0F;
    public float currentX, currentY;
    public float targetX, targetY;
    public float currentScale = 0.6F;
    public float targetScale = 1.0F;
}
