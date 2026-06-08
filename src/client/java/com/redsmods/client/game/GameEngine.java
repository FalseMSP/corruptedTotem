package com.redsmods.client.game;

import com.redsmods.client.beatmap.OsuBeatmap;

/**
 * Drives the per-tick logic for an Corrupted Mania game (its just Osu Mania lmao):
 * - Auto-misses notes that fell past the hit zone
 * - Processes key presses against upcoming notes
 * - Tracks hold note completion and early-release penalties
 */
public class GameEngine {

    /**
     * Call every client tick while the game is PLAYING.
     * @param state  current game state (mutated in place)
     */
    public static void tick(GameState state) {
        if (state.phase != GameState.Phase.PLAYING) return;

        long now = state.getCurrentMs();
        state.pruneOldHits();

        // ── Resolve active holds ──────────────────────────────────────────────
        for (int c = 0; c < GameState.COLUMNS; c++) {
            OsuBeatmap.ManiaNote hold = state.activeHolds[c];
            if (hold == null) continue;

            if (now >= hold.endTime) {
                // Player held long enough — perfect completion
                state.activeHolds[c] = null;
                state.registerHit(GameState.HitResult.Type.PERFECT, c);
            } else if (!state.columnPressed[c]) {
                // Key released early — penalise as a miss
                state.activeHolds[c] = null;
                state.registerHit(GameState.HitResult.Type.MISS, c);
            }
            // else: still holding, keep waiting
        }

        // ── Auto-miss tap notes and hold-note heads that scrolled past ────────
        while (state.noteIndex < state.beatmap.notes.size()) {
            OsuBeatmap.ManiaNote note = state.beatmap.notes.get(state.noteIndex);

            if (note.hit || note.missed) {
                state.noteIndex++;
                continue;
            }

            // If the note's deadline (startTime + miss window) has passed
            if (now > note.startTime + state.beatmap.hitWindow50) {
                note.missed = true;
                state.registerHit(GameState.HitResult.Type.MISS, note.column);
                state.noteIndex++;
            } else {
                break; // notes are sorted, no need to look further
            }
        }

        // Check if game ended
        if (state.isFinished()) {
            state.phase = GameState.Phase.FINISHED;
        }
    }

    /**
     * Called when a column key is pressed.
     * Finds the nearest note in that column within the hit window and scores it.
     * For hold notes, registers the head hit and starts hold tracking.
     */
    public static void onColumnPress(GameState state, int column) {
        if (state.phase != GameState.Phase.PLAYING) return;

        // Ignore press if a hold is already active in this column
        if (state.activeHolds[column] != null) return;

        long now = state.getCurrentMs();

        OsuBeatmap.ManiaNote best = null;
        int bestDelta = Integer.MAX_VALUE;

        for (int i = state.noteIndex; i < state.beatmap.notes.size(); i++) {
            OsuBeatmap.ManiaNote note = state.beatmap.notes.get(i);
            if (note.hit || note.missed) continue;
            if (note.column != column) continue;

            // Stop scanning far-future notes
            if (note.startTime - now > state.beatmap.hitWindow50 + 200) break;

            int delta = (int) Math.abs(now - note.startTime);
            if (delta <= state.beatmap.hitWindow50 && delta < bestDelta) {
                bestDelta = delta;
                best = note;
            }
        }

        if (best == null) return; // ghost hit — no penalty

        best.hit = true;

        GameState.HitResult.Type type;
        if (bestDelta <= state.beatmap.hitWindow300)      type = GameState.HitResult.Type.PERFECT;
        else if (bestDelta <= state.beatmap.hitWindow100) type = GameState.HitResult.Type.GREAT;
        else                                              type = GameState.HitResult.Type.GOOD;

        if (best.isHold()) {
            // For hold notes, record the head-hit quality but defer the score
            // reward until the tail is reached in tick(). Show feedback now.
            state.activeHolds[column] = best;
            // Show a visual "holding" indicator using the initial hit type
            long now2 = System.currentTimeMillis();
            state.recentHits.add(new GameState.HitResult(type, column, now2 + 800));
        } else {
            state.registerHit(type, column);
        }
    }

    /**
     * Called when a column key is released.
     * For hold notes whose tail hasn't arrived yet, this triggers an early-release miss.
     * tick() handles the release detection each frame, but this provides an
     * immediate response on the exact release event.
     */
    public static void onColumnRelease(GameState state, int column) {
        if (state.phase != GameState.Phase.PLAYING) return;

        OsuBeatmap.ManiaNote hold = state.activeHolds[column];
        if (hold == null) return;

        long now = state.getCurrentMs();

        if (now >= hold.endTime) {
            // Released right at or after the tail — count as perfect
            state.activeHolds[column] = null;
            state.registerHit(GameState.HitResult.Type.PERFECT, column);
        } else {
            // Released too early — miss
            state.activeHolds[column] = null;
            state.registerHit(GameState.HitResult.Type.MISS, column);
        }
    }
}