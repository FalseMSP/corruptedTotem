package com.redsmods;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import com.mojang.math.Transformation;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

public class VisualDomain {

    public static Map<Player, VisualDomain> domainMap = new HashMap();

    private static final float TARGET_SCALE   = 16f;
    private static final int   INTRO_TICKS    = 40;
    private static final int   OUTRO_TICKS    = 40;
    private static final float INTRO_SPIN_DEG = 18f;
    private static final float IDLE_SPIN_DEG  = 1.5f;
    private static final float OUTRO_SPIN_DEG = 1.5f;

    private enum Phase { INTRO, IDLE, OUTRO, DONE }

    private final ServerLevel level;
    private final Vec3        origin;

    private BlockDisplay entity;
    private Phase        phase      = Phase.INTRO;
    private int          phaseTick  = 0;
    private float        angleDeg   = 0f;

    private volatile boolean terminateCalled = false;

    public VisualDomain(Player player) {
        domainMap.put(player,this);
        this.level  = (ServerLevel) player.level();
        this.origin = new Vec3(player.getX(), player.getY(), player.getZ());
    }

    public void spawn() {
        if (entity != null) return;

        entity = new BlockDisplay(net.minecraft.world.entity.EntityType.BLOCK_DISPLAY, level);
        entity.setPos(origin.x, origin.y, origin.z);
        entity.setBlockState(Blocks.NOTE_BLOCK.defaultBlockState());
        entity.setBrightnessOverride(new Brightness(15, 15));
        entity.setViewRange(256f);
        entity.setPosRotInterpolationDuration(1);
        entity.setTransformationInterpolationDuration(1);
        entity.setTransformationInterpolationDelay(0);

        // Start invisible
        applyAttributes(0f, 0f);

        level.addFreshEntity(entity);
    }

    public void terminate() { terminateCalled = true; }
    public boolean isDone() { return phase == Phase.DONE; }

    public boolean tick() {
        if (phase == Phase.DONE) return false;
        if (entity == null || entity.isRemoved()) {
            phase = Phase.DONE;
            return false;
        }

        switch (phase) {
            case INTRO -> tickIntro();
            case IDLE  -> tickIdle();
            case OUTRO -> tickOutro();
            default    -> {}
        }

        return phase != Phase.DONE;
    }

    // ── Phase logic ──────────────────────────────────────────────────────────

    private void tickIntro() {
        phaseTick++;
        float t     = (float) phaseTick / INTRO_TICKS;
        float eased = 1f - (1f - t) * (1f - t);
        angleDeg += INTRO_SPIN_DEG;
        applyAttributes(TARGET_SCALE * eased, angleDeg);
        if (phaseTick >= INTRO_TICKS) { phase = Phase.IDLE; phaseTick = 0; }
    }

    private void tickIdle() {
        if (terminateCalled) { phase = Phase.OUTRO; phaseTick = 0; return; }
        angleDeg += IDLE_SPIN_DEG;
        applyAttributes(TARGET_SCALE, angleDeg);
    }

    private void tickOutro() {
        phaseTick++;
        float t     = (float) phaseTick / OUTRO_TICKS;
        float eased = t * t;
        angleDeg += OUTRO_SPIN_DEG;
        applyAttributes(Math.max(0f, TARGET_SCALE * (1f - eased)), angleDeg);
        if (phaseTick >= OUTRO_TICKS) {
            entity.discard();
            entity = null;
            phase  = Phase.DONE;
        }
    }

    // ── Attribute-based transform ─────────────────────────────────────────────

    /**
     * Drives scale via the Transformation's scale vector and rotation via
     * setYRot — both are native entity attributes that the client interpolates
     * automatically. The centering translation (-0.5) was baked in at spawn
     * and is never overwritten here.
     *
     * NOTE: setYRot drives the entity's own Y-rotation attribute. Because
     * BlockDisplay applies left-rotation from the Transformation BEFORE the
     * entity's own rotation, we use left-rotation here so the visual pivot
     * stays correct (centered on origin). Alternatively: leave left-rotation
     * as identity and bake the angle into a Quaternionf — either works; this
     * approach is simpler to read.
     */
    private void applyAttributes(float scale, float angleDeg) {
        if (entity == null) return;

        float half = scale / 2f;
        float angleRad = (float) Math.toRadians(angleDeg % 360.0);

        // Move entity position so the visual center lands on origin.
        // The matrix centers the block around entity pos, but entity pos
        // itself needs to be at origin - half so the scaled cube is centered
        // on origin in world space.
        entity.setPos(
                origin.x - half,
                origin.y - half,
                origin.z - half
        );

        org.joml.Matrix4f mat = new org.joml.Matrix4f()
                .translate(half, half, half)      // re-center after scale
                .rotateY(angleRad)                // spin around center
                .scale(scale, scale, scale)       // scale up
                .translate(-0.5f, -0.5f, -0.5f); // move block corner to origin

        entity.setTransformation(new Transformation(mat));
        entity.setTransformationInterpolationDelay(0);
    }

    // ── Auto-tick helper ──────────────────────────────────────────────────────

    public static void autoTick(VisualDomain sde) {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level != sde.level) return;
            sde.tick();
        });
    }
}