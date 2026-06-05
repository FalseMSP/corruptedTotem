package com.redsmods.client.game;

import com.redsmods.client.beatmap.OsuBeatmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all runtime state for an active Corrupted Mania game session.
 * The game is a 4-column falling-note rhythm game (osu!mania style)
 * rendered entirely in the Minecraft HUD/screen layer.
 */
public class GameState {

    // ── Configuration ────────────────────────────────────────────────────────

    /** How many milliseconds of notes are visible on screen (scroll speed) */
    public static final int SCROLL_MS = 1500;

    /** Number of columns (lanes) */
    public static final int COLUMNS = 4;

    // ── Session Data ─────────────────────────────────────────────────────────

    public final OsuBeatmap beatmap;

    /** Current playback time in milliseconds (driven by System.currentTimeMillis) */
    public long startWallTime;
    public long pausedAt = -1;

    /** Per-column key state (pressed this frame) */
    public boolean[] columnPressed = new boolean[COLUMNS];
    public boolean[] columnWasPressed = new boolean[COLUMNS];

    /** Index into beatmap.notes for the first unprocessed note */
    public int noteIndex = 0;

    // ── Scoring ───────────────────────────────────────────────────────────────

    public long score = 0;
    public int combo = 0;
    public int maxCombo = 0;

    public int count300 = 0; // Perfect
    public int count100 = 0; // Great
    public int count50  = 0; // Good
    public int countMiss = 0;

    // ── Active Visual Feedback ────────────────────────────────────────────────

    public static class HitResult {
        public enum Type { PERFECT, GREAT, GOOD, MISS }
        public Type type;
        public int column;
        public long expireTime; // System.currentTimeMillis() when it fades
        public HitResult(Type type, int column, long expireTime) {
            this.type = type;
            this.column = column;
            this.expireTime = expireTime;
        }
    }

    public final List<HitResult> recentHits = new ArrayList<>();

    // ── Game Lifecycle ────────────────────────────────────────────────────────

    public enum Phase { PLAYING, PAUSED, FINISHED }
    public Phase phase = Phase.PLAYING;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameState(OsuBeatmap beatmap) {
        this.beatmap = beatmap;
        this.startWallTime = System.currentTimeMillis();
    }

    // ── Time ─────────────────────────────────────────────────────────────────

    public long getCurrentMs() {
        if (phase == Phase.PAUSED) {
            return pausedAt - startWallTime;
        }
        return System.currentTimeMillis() - startWallTime;
    }

    public void pause() {
        if (phase == Phase.PLAYING) {
            pausedAt = System.currentTimeMillis();
            phase = Phase.PAUSED;
        }
    }

    public void resume() {
        if (phase == Phase.PAUSED) {
            // Shift start time forward by the pause duration
            startWallTime += System.currentTimeMillis() - pausedAt;
            pausedAt = -1;
            phase = Phase.PLAYING;
        }
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    public void registerHit(HitResult.Type type, int column) {
        long now = System.currentTimeMillis();
        recentHits.add(new HitResult(type, column, now + 800));

        switch (type) {
            case PERFECT -> { count300++; combo++; score += 300L * (1 + combo / 10); }
            case GREAT   -> { count100++; combo++; score += 100L * (1 + combo / 10); }
            case GOOD    -> { count50++;  combo++; score += 50L  * (1 + combo / 10); }
            case MISS    -> { countMiss++; combo = 0; }
        }
        if (combo > maxCombo) maxCombo = combo;
    }

    public double getAccuracy() {
        int total = count300 + count100 + count50 + countMiss;
        if (total == 0) return 100.0;
        return (300.0 * count300 + 100.0 * count100 + 50.0 * count50)
                / (300.0 * total) * 100.0;
    }

    public void pruneOldHits() {
        long now = System.currentTimeMillis();
        recentHits.removeIf(h -> h.expireTime < now);
    }

    // ── Finish Check ──────────────────────────────────────────────────────────

    public boolean isFinished() {
        return noteIndex >= beatmap.notes.size()
                && getCurrentMs() > (beatmap.notes.isEmpty() ? 0
                    : beatmap.notes.get(beatmap.notes.size() - 1).startTime + 2000);
    }
}
