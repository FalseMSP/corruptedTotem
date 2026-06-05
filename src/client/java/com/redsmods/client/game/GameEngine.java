package com.redsmods.client.game;

import com.redsmods.client.beatmap.OsuBeatmap;

/**
 * Drives the per-tick logic for an Corrupted Mania game (its just Osu Mania lmao):
 * - Auto-misses notes that fell past the hit zone
 * - Processes key presses against upcoming notes
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

        // Auto-miss notes that have gone past the 50ms window
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

        // Check held keys against notes that just entered the hit window
        for (int c = 0; c < GameState.COLUMNS; c++) {
            if (!state.columnPressed[c]) continue;

            for (int i = state.noteIndex; i < state.beatmap.notes.size(); i++) {
                OsuBeatmap.ManiaNote note = state.beatmap.notes.get(i);
                if (note.hit || note.missed) continue;
                if (note.column != c) continue;
                if (note.startTime > now + state.beatmap.hitWindow50) break;

                long delta = Math.abs(now - note.startTime);
                if (delta <= state.beatmap.hitWindow50) {
                    note.hit = true;
                    GameState.HitResult.Type type;
                    if (delta <= state.beatmap.hitWindow300)      type = GameState.HitResult.Type.PERFECT;
                    else if (delta <= state.beatmap.hitWindow100) type = GameState.HitResult.Type.GREAT;
                    else                                          type = GameState.HitResult.Type.GOOD;
                    state.registerHit(type, c);
                    break;
                }
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
     */
    public static void onColumnPress(GameState state, int column) {
        if (state.phase != GameState.Phase.PLAYING) return;
        long now = state.getCurrentMs();

        OsuBeatmap.ManiaNote best = null;
        int bestDelta = Integer.MAX_VALUE;

        // Scan notes near current time in this column
        for (int i = state.noteIndex; i < state.beatmap.notes.size(); i++) {
            OsuBeatmap.ManiaNote note = state.beatmap.notes.get(i);
            if (note.hit || note.missed) continue;
            if (note.column != column) continue;

            int delta = (int) Math.abs(now - note.startTime);

            // Stop scanning far-future notes
            if (note.startTime - now > state.beatmap.hitWindow50 + 200) break;

            // Only consider notes within the 50ms window
            if (delta <= state.beatmap.hitWindow50) {
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = note;
                }
            }
        }

        if (best != null) {
            best.hit = true;
            GameState.HitResult.Type type;
            if (bestDelta <= state.beatmap.hitWindow300) {
                type = GameState.HitResult.Type.PERFECT;
            } else if (bestDelta <= state.beatmap.hitWindow100) {
                type = GameState.HitResult.Type.GREAT;
            } else {
                type = GameState.HitResult.Type.GOOD;
            }
            state.registerHit(type, column);
        }
        // No note found → ghost hit (no penalty, no feedback)
    }
}
