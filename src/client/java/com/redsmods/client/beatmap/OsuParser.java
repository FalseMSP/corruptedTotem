package com.redsmods.client.beatmap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses osu! beatmap files (.osu format).
 * Supports versions 5-14 of the .osu format.
 * Handles osu!standard (mode 0) and osu!mania (mode 3) hit objects.
 * Standard mode circles/sliders are converted to 4K mania layout.
 */
public class OsuParser {

    private static final int COLUMNS = 4;

    public static OsuBeatmap parse(File file) throws IOException {
        OsuBeatmap beatmap = new OsuBeatmap();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            String section = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("//")) continue;

                // Detect section headers
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1);
                    continue;
                }

                switch (section) {
                    case "General"     -> parseGeneral(line, beatmap);
                    case "Metadata"    -> parseMetadata(line, beatmap);
                    case "Difficulty"  -> parseDifficulty(line, beatmap);
                    case "TimingPoints"-> parseTimingPoint(line, beatmap);
                    case "HitObjects"  -> parseHitObject(line, beatmap);
                }
            }
        }

        beatmap.calculateHitWindows();
        beatmap.notes.sort((a, b) -> a.startTime - b.startTime);
        return beatmap;
    }

    private static void parseGeneral(String line, OsuBeatmap beatmap) {
        String[] kv = splitKeyValue(line);
        if (kv == null) return;
        switch (kv[0]) {
            case "AudioFilename" -> beatmap.audioFilename = kv[1];
            case "Mode" -> beatmap.mode = parseInt(kv[1], 0);
        }
    }

    private static void parseMetadata(String line, OsuBeatmap beatmap) {
        String[] kv = splitKeyValue(line);
        if (kv == null) return;
        switch (kv[0]) {
            case "Title"   -> beatmap.title = kv[1];
            case "Artist"  -> beatmap.artist = kv[1];
            case "Creator" -> beatmap.creator = kv[1];
            case "Version" -> beatmap.version = kv[1];
        }
    }

    private static void parseDifficulty(String line, OsuBeatmap beatmap) {
        String[] kv = splitKeyValue(line);
        if (kv == null) return;
        switch (kv[0]) {
            case "OverallDifficulty" -> beatmap.overallDifficulty = parseFloat(kv[1], 5.0f);
            case "ApproachRate"      -> beatmap.approachRate = parseFloat(kv[1], 5.0f);
            case "CircleSize"        -> beatmap.circleSize = (int) parseFloat(kv[1], 4.0f);
        }
    }

    private static void parseTimingPoint(String line, OsuBeatmap beatmap) {
        String[] parts = line.split(",");
        if (parts.length < 2) return;
        try {
            int offset = (int) Double.parseDouble(parts[0].trim());
            double beatLength = Double.parseDouble(parts[1].trim());
            boolean inherited = parts.length > 6 && parts[6].trim().equals("0");
            beatmap.timingPoints.add(new OsuBeatmap.TimingPoint(offset, beatLength, inherited));
        } catch (NumberFormatException ignored) {}
    }

    private static void parseHitObject(String line, OsuBeatmap beatmap) {
        // Format: x,y,time,type,hitSound[,objectParams][,hitSample]
        String[] parts = line.split(",");
        if (parts.length < 5) return;

        try {
            int x = parseInt(parts[0].trim(), 0);
            // int y = parseInt(parts[1].trim(), 0); // unused
            int time = parseInt(parts[2].trim(), 0);
            int type = parseInt(parts[3].trim(), 0);

            int column;

            if (beatmap.mode == 3) {
                // osu!mania: x maps to column directly
                // Formula: column = floor(x * columns / 512)
                int cols = Math.max(1, beatmap.circleSize);
                column = (int) Math.floor((double) x * cols / 512.0);
                column = Math.min(Math.max(column, 0), COLUMNS - 1);

                // If more than 4 columns, collapse to 4
                if (cols > COLUMNS) {
                    column = (int) ((double) column / cols * COLUMNS);
                    column = Math.min(column, COLUMNS - 1);
                }

                // Check for hold note (type bit 7 = 128)
                int endTime = time;
                if ((type & 128) != 0 && parts.length > 5) {
                    String extra = parts[5].trim();
                    int colonIdx = extra.indexOf(':');
                    String endStr = colonIdx >= 0 ? extra.substring(0, colonIdx) : extra;
                    endTime = parseInt(endStr, time);
                }

                beatmap.notes.add(new OsuBeatmap.ManiaNote(column, time, endTime));
            } else {
                // osu!standard: map (x,y) position to one of 4 columns
                // Divide the 512-wide playfield into 4 equal columns
                column = Math.min((int) (x / 512.0 * COLUMNS), COLUMNS - 1);

                // Check for slider (type bit 1) — treat as tap at start
                // Check for circle (type bit 0)
                beatmap.notes.add(new OsuBeatmap.ManiaNote(column, time, time));
            }

        } catch (NumberFormatException ignored) {}
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String[] splitKeyValue(String line) {
        int idx = line.indexOf(':');
        if (idx < 0) return null;
        return new String[]{
            line.substring(0, idx).trim(),
            line.substring(idx + 1).trim()
        };
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return def; }
    }
}
