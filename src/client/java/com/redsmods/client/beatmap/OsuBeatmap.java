package com.redsmods.client.beatmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed osu! beatmap (.osu file).
 * Supports osu!mania (4K) and standard osu! modes.
 * All hit objects are converted to 4-lane mania format for gameplay.
 */
public class OsuBeatmap {

    // Metadata
    public String title = "Unknown";
    public String artist = "Unknown";
    public String creator = "Unknown";
    public String version = "Unknown";
    public String audioFilename = "";

    // Difficulty settings
    public float overallDifficulty = 5.0f;
    public float approachRate = 5.0f;
    public int circleSize = 4; // columns for mania

    // Game mode (0=standard, 3=mania)
    public int mode = 0;

    // Timing points (for BPM display)
    public List<TimingPoint> timingPoints = new ArrayList<>();

    // Hit objects converted to mania notes
    public List<ManiaNote> notes = new ArrayList<>();

    // The hit window in milliseconds based on OD
    public int hitWindow300;
    public int hitWindow100;
    public int hitWindow50;

    public String getDisplayName() {
        return artist + " - " + title + " [" + version + "]";
    }

    /**
     * Calculates hit windows from Overall Difficulty.
     * Uses osu!mania formula.
     */
    public void calculateHitWindows() {
        hitWindow300 = (int) (16 + 64 * (10 - overallDifficulty) / 10f);
        hitWindow100 = (int) (34 + 124 * (10 - overallDifficulty) / 10f);
        hitWindow50  = (int) (95 + 171 * (10 - overallDifficulty) / 10f);
    }

    /** A single note in 4K mania format */
    public static class ManiaNote {
        /** Column index 0-3 */
        public int column;
        /** Time in milliseconds */
        public int startTime;
        /** End time for hold notes (0 = tap note) */
        public int endTime;
        /** Whether this note has been hit/missed */
        public boolean hit = false;
        public boolean missed = false;

        public ManiaNote(int column, int startTime, int endTime) {
            this.column = column;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public boolean isHold() {
            return endTime > startTime;
        }
    }

    /** A timing point for BPM/offset */
    public static class TimingPoint {
        public int offset;
        public double beatLength;
        public boolean inherited;

        public TimingPoint(int offset, double beatLength, boolean inherited) {
            this.offset = offset;
            this.beatLength = beatLength;
            this.inherited = inherited;
        }

        public double getBPM() {
            if (beatLength > 0) {
                return 60000.0 / beatLength;
            }
            return 0;
        }
    }
}
