package com.redsmods.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redsmods.client.beatmap.OsuBeatmap;
import com.redsmods.client.game.GameEngine;
import com.redsmods.client.game.GameState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The main osu! gameplay screen.
 *
 * Layout (all coords in screen pixels, playfield centered):
 *
 *   ┌─────────────────────────────────────────┐
 *   │  Score / Combo / Accuracy (top-left)    │
 *   │                                         │
 *   │   ┌──┬──┬──┬──┐                        │
 *   │   │  │  │  │  │  ← columns (falling)   │
 *   │   │  │▓▓│  │  │                        │
 *   │   │  │  │▓▓│  │                        │
 *   │   └──┴──┴──┴──┘                        │
 *   │   [D][F][J][K]  ← hit zone             │
 *   └─────────────────────────────────────────┘
 *
 * Notes are squares (equal width & height).
 * The playfield is centered horizontally and takes up 70% of screen height.
 */
public class OsuGameScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int COLUMNS = GameState.COLUMNS;
    /** Note square size in pixels */
    private static final int NOTE_SIZE = 40;
    private static final int COLUMN_GAP = 4;
    /** Total width of all columns */
    private static final int PLAYFIELD_WIDTH = COLUMNS * NOTE_SIZE + (COLUMNS - 1) * COLUMN_GAP;
    /** Height of the playfield (notes fall from top to hit zone) */
    private static final int PLAYFIELD_HEIGHT_RATIO = 70; // percent of screen height
    /** Hit zone strip height at the bottom of the playfield */
    private static final int HIT_ZONE_HEIGHT = NOTE_SIZE + 8;

    // ── Colors ────────────────────────────────────────────────────────────────
    // argb packed ints
    private static final int COL_BG          = 0xCC000000; // semi-transparent black
    private static final int COL_PLAYFIELD   = 0xAA111122;
    private static final int COL_COLUMN_EDGE = 0xFF334455;
    private static final int COL_HIT_ZONE    = 0xFF223344;
    private static final int COL_HIT_LINE    = 0xFFAADDFF;
    private static final int[] COL_NOTE = {
        0xFFFF6688, // col 0 – pink
        0xFF88BBFF, // col 1 – blue
        0xFF88BBFF, // col 2 – blue
        0xFFFF6688, // col 3 – pink
    };
    private static final int[] COL_NOTE_PRESSED = {
        0xFFFFAABB, // lit up when key held
        0xFFBBDDFF,
        0xFFBBDDFF,
        0xFFFFAABB,
    };
    private static final int COL_PERFECT  = 0xFFFFEE44;
    private static final int COL_GREAT    = 0xFF44DDFF;
    private static final int COL_GOOD     = 0xFF88FF44;
    private static final int COL_MISS     = 0xFFFF4444;
    private static final int COL_TEXT     = 0xFFFFFFFF;
    private static final int COL_SHADOW   = 0xFF000000;

    // ── Key bindings: D F J K (indices match GLFW key codes) ─────────────────
    private static final int[] COLUMN_KEYS = {GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F,
                                              GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K};

    // ── State ─────────────────────────────────────────────────────────────────
    private final GameState state;
    private final Screen parent;

    /** Derived layout values computed in init() */
    private int pfLeft, pfTop, pfBottom;
    private int playfieldActualHeight;

    public OsuGameScreen(Screen parent, GameState state) {
        super(Component.translatable("corrupted.mania.screen.title"));
        this.parent = parent;
        this.state  = state;
    }

    @Override
    protected void init() {
        super.init();
        // Center playfield horizontally
        pfLeft = (this.width - PLAYFIELD_WIDTH) / 2;
        playfieldActualHeight = this.height * PLAYFIELD_HEIGHT_RATIO / 100;
        pfTop = (this.height - playfieldActualHeight) / 2;
        pfBottom = pfTop + playfieldActualHeight;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        GameEngine.tick(state);
        if (state.phase == GameState.Phase.FINISHED) {
            // Show results screen
            assert this.minecraft != null;
            this.minecraft.setScreen(new OsuResultScreen(parent, state));
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        // ESC → pause / back
        if (event.isEscape()) {
            if (state.phase == GameState.Phase.PLAYING) {
                state.pause();
            } else if (state.phase == GameState.Phase.PAUSED) {
                assert this.minecraft != null;
                this.minecraft.setScreen(parent);
            }
            return true;
        }
        // P → pause/resume toggle
        if (event.isConfirmation()) {
            if (state.phase == GameState.Phase.PLAYING) state.pause();
            else if (state.phase == GameState.Phase.PAUSED) state.resume();
            return true;
        }
        // Column keys
        for (int c = 0; c < COLUMNS; c++) {
            if (event.key() == COLUMN_KEYS[c] && !state.columnWasPressed[c]) {
                state.columnPressed[c] = true;
                state.columnWasPressed[c] = true;
                GameEngine.onColumnPress(state, c);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, this.width, this.height, COL_BG);

        drawPlayfield(gfx);
        drawNotes(gfx);
        drawHitZone(gfx);
        drawKeyLabels(gfx);
        drawHUD(gfx);
        drawHitFeedback(gfx);

        if (state.phase == GameState.Phase.PAUSED) {
            drawPauseOverlay(gfx);
        }

        super.extractRenderState(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {    // KeyEvent, not (int, int, int)
        for (int c = 0; c < COLUMNS; c++) {
            if (event.key() == COLUMN_KEYS[c]) {
                state.columnPressed[c] = false;
                state.columnWasPressed[c] = false;
                return true;
            }
        }
        return super.keyReleased(event);
    }

    // Don't pause game when screen loses focus (so key releases still work)
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawPlayfield(GuiGraphicsExtractor gfx) {
        // Background of the playfield columns
        gfx.fill(pfLeft - 2, pfTop, pfLeft + PLAYFIELD_WIDTH + 2, pfBottom, COL_PLAYFIELD);

        // Column dividers
        for (int c = 0; c <= COLUMNS; c++) {
            int x = pfLeft + c * (NOTE_SIZE + COLUMN_GAP) - (c > 0 ? COLUMN_GAP : 0);
            gfx.fill(x, pfTop, x + 1, pfBottom, COL_COLUMN_EDGE);
        }
    }

    private void drawNotes(GuiGraphicsExtractor gfx) {
        long nowMs = state.getCurrentMs();
        List<OsuBeatmap.ManiaNote> notes = state.beatmap.notes;

        int hitZoneY = pfBottom - HIT_ZONE_HEIGHT;

        for (OsuBeatmap.ManiaNote note : notes) {
            if (note.hit || note.missed) continue;

            int deltaMs = (int) (note.startTime - nowMs);
            // Skip notes too far in future or past
            if (deltaMs > GameState.SCROLL_MS + 200) continue;
            if (deltaMs < -state.beatmap.hitWindow50 - 50) continue;

            // Y position: 0 ms = hitZoneY, SCROLL_MS = pfTop
            float t = (float) deltaMs / GameState.SCROLL_MS;
            int noteY = hitZoneY - (int) (t * (hitZoneY - pfTop)) - NOTE_SIZE;

            // Clamp to playfield
            if (noteY + NOTE_SIZE < pfTop) continue;
            if (noteY > pfBottom) continue;

            int noteX = pfLeft + note.column * (NOTE_SIZE + COLUMN_GAP);

            int color = state.columnPressed[note.column] ? COL_NOTE_PRESSED[note.column] : COL_NOTE[note.column];

            // Draw note square
            gfx.fill(noteX, noteY, noteX + NOTE_SIZE, noteY + NOTE_SIZE, color);

            // Inner highlight
            gfx.fill(noteX + 3, noteY + 3, noteX + NOTE_SIZE - 3, noteY + 8, 0x44FFFFFF);

            // Draw hold trail if it's a hold note
            if (note.isHold()) {
                int endDeltaMs = (int) (note.endTime - nowMs);
                float te = (float) endDeltaMs / GameState.SCROLL_MS;
                int holdEndY = hitZoneY - (int) (te * (hitZoneY - pfTop));
                int holdTop = Math.max(pfTop, Math.min(noteY, holdEndY));
                int holdBot = Math.min(pfBottom, Math.max(noteY + NOTE_SIZE, holdEndY));
                // Draw hold body as a narrow strip
                int hx = noteX + NOTE_SIZE / 2 - 6;
                gfx.fill(hx, holdTop, hx + 12, holdBot, 0x88AADDFF);
            }
        }
    }

    private void drawHitZone(GuiGraphicsExtractor gfx) {
        int hitZoneTop = pfBottom - HIT_ZONE_HEIGHT;

        // Hit zone background
        gfx.fill(pfLeft, hitZoneTop, pfLeft + PLAYFIELD_WIDTH, pfBottom, COL_HIT_ZONE);

        // Hit line
        gfx.fill(pfLeft, hitZoneTop, pfLeft + PLAYFIELD_WIDTH, hitZoneTop + 2, COL_HIT_LINE);

        // Per-column receptor squares (glow when key held)
        for (int c = 0; c < COLUMNS; c++) {
            int rx = pfLeft + c * (NOTE_SIZE + COLUMN_GAP);
            int ry = hitZoneTop + 4;
            int col = state.columnPressed[c] ? COL_NOTE_PRESSED[c] : 0xFF223355;
            gfx.fill(rx, ry, rx + NOTE_SIZE, ry + NOTE_SIZE, col);
            // Outline
            gfx.fill(rx, ry, rx + NOTE_SIZE, ry + 2, 0x55FFFFFF);
            gfx.fill(rx, ry + NOTE_SIZE - 2, rx + NOTE_SIZE, ry + NOTE_SIZE, 0x33FFFFFF);
        }
    }

    private void drawKeyLabels(GuiGraphicsExtractor gfx) {
        String[] labels = {"D", "F", "J", "K"};
        for (int c = 0; c < COLUMNS; c++) {
            int rx = pfLeft + c * (NOTE_SIZE + COLUMN_GAP);
            int labelY = pfBottom - HIT_ZONE_HEIGHT + 4 + NOTE_SIZE + 6;
            String lbl = labels[c];
            int lx = rx + (NOTE_SIZE - this.font.width(lbl)) / 2;
            gfx.text(this.font, lbl, lx, labelY, COL_TEXT);
        }
    }

    private void drawHUD(GuiGraphicsExtractor gfx) {
        long score = state.score;
        int combo   = state.combo;
        double acc  = state.getAccuracy();

        // Score (top-right of playfield)
        String scoreStr = String.format("%08d", score);
        gfx.text(this.font, scoreStr,
                pfLeft + PLAYFIELD_WIDTH - this.font.width(scoreStr), pfTop + 4, COL_TEXT);

        // Combo (above hit zone, centered)
        if (combo > 0) {
            String comboStr = combo + "x";
            int cx = pfLeft + (PLAYFIELD_WIDTH - this.font.width(comboStr)) / 2;
            gfx.text(this.font, comboStr, cx, pfBottom - HIT_ZONE_HEIGHT - 16, 0xFFFFEE44);
        }

        // Accuracy (top-left of playfield)
        String accStr = String.format("%.1f%%", acc);
        gfx.text(this.font, accStr, pfLeft, pfTop + 4, COL_TEXT);

        // Beatmap name (top, centered)
        String bname = state.beatmap.getDisplayName();
        int bnx = (this.width - this.font.width(bname)) / 2;
        gfx.text(this.font, bname, bnx, 6, 0xAAFFFFFF);

        // Progress bar (bottom of screen)
        long total = state.beatmap.notes.isEmpty() ? 1
                : state.beatmap.notes.get(state.beatmap.notes.size() - 1).startTime;
        float progress = total > 0 ? Mth.clamp((float) state.getCurrentMs() / total, 0, 1) : 0;
        int barWidth = this.width - 20;
        gfx.fill(10, this.height - 8, 10 + barWidth, this.height - 4, 0x55FFFFFF);
        gfx.fill(10, this.height - 8, 10 + (int)(barWidth * progress), this.height - 4, 0xFFAADDFF);
    }

    private void drawHitFeedback(GuiGraphicsExtractor gfx) {
        long now = System.currentTimeMillis();
        for (GameState.HitResult hit : state.recentHits) {
            if (hit.expireTime < now) continue;

            float age = 1.0f - (float)(hit.expireTime - now) / 800f; // 0=fresh, 1=expired
            int alpha = (int)((1.0f - age) * 255) << 24;
            int baseY = pfBottom - HIT_ZONE_HEIGHT - 40 - (int)(age * 20);

            String label;
            int color;
            switch (hit.type) {
                case PERFECT -> { label = "PERFECT"; color = alpha | (COL_PERFECT & 0x00FFFFFF); }
                case GREAT   -> { label = "GREAT";   color = alpha | (COL_GREAT   & 0x00FFFFFF); }
                case GOOD    -> { label = "GOOD";    color = alpha | (COL_GOOD    & 0x00FFFFFF); }
                default      -> { label = "MISS";    color = alpha | (COL_MISS    & 0x00FFFFFF); }
            }

            int lx = pfLeft + hit.column * (NOTE_SIZE + COLUMN_GAP)
                     + (NOTE_SIZE - this.font.width(label)) / 2;
            gfx.text(this.font, label, lx, baseY, color);
        }
    }

    private void drawPauseOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, this.height, 0x88000000);
        String msg = "PAUSED  –  [ENTER] Resume  [ESC] Quit";
        int mx = (this.width - this.font.width(msg)) / 2;
        int my = this.height / 2 - 6;
        gfx.text(this.font, msg, mx, my, 0xFFFFFFFF);
    }
}
