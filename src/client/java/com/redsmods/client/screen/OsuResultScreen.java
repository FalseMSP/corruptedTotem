package com.redsmods.client.screen;

import com.redsmods.client.game.GameState;
import com.redsmods.client.network.ClientNetworkHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shown after a beatmap finishes. Displays score, accuracy, combo, and grade.
 */
public class OsuResultScreen extends Screen {

    private final Screen parent;
    private final GameState state;

    public OsuResultScreen(Screen parent, GameState state) {
        super(Component.translatable("corrupted.mania.screen.results"));
        this.parent = parent;
        this.state  = state;
    }

    @Override
    protected void init() {
        super.init();
        // Retry button
        this.addRenderableWidget(Button.builder(
            Component.literal("Retry"),
            btn -> {
                assert this.minecraft != null;
                GameState fresh = new GameState(state.beatmap);
                this.minecraft.setScreen(new OsuGameScreen(parent, fresh));
            }
        ).pos(this.width / 2 - 105, this.height - 60).size(100, 20).build());

        // Menu button
        this.addRenderableWidget(Button.builder(
            Component.literal("Quit"),
            btn -> {
                assert this.minecraft != null;
                this.minecraft.setScreen(minecraft.screen);
                ClientNetworkHandler.sendSongEnd();
            }
        ).pos(this.width / 2 + 5, this.height - 60).size(100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, this.width, this.height, 0xFF101020);

        int cx = this.width / 2;
        int y = 40;

        // Title
        drawCenteredBig(gfx, "RESULTS", cx, y, 0xFFFFEE44);
        y += 30;

        // Beatmap name
        drawCentered(gfx, state.beatmap.getDisplayName(), cx, y, 0xFFAAAAFF);
        y += 20;

        // Grade
        String grade = computeGrade();
        drawCenteredBig(gfx, grade, cx, y + 5, gradeColor(grade));
        y += 40;

        // Score
        drawCentered(gfx, String.format("Score: %,d", state.score), cx, y, 0xFFFFFFFF);
        y += 16;

        drawCentered(gfx, String.format("Accuracy: %.2f%%", state.getAccuracy()), cx, y, 0xFFFFFFFF);
        y += 16;

        drawCentered(gfx, String.format("Max Combo: %dx", state.maxCombo), cx, y, 0xFFFFFFFF);
        y += 24;

        // Hit breakdown
        int bx = cx - 100;
        gfx.text(this.font, String.format("PERFECT: %d", state.count300), bx, y, 0xFFFFEE44);
        y += 14;
        gfx.text(this.font, String.format("GREAT:   %d", state.count100), bx, y, 0xFF44DDFF);
        y += 14;
        gfx.text(this.font, String.format("GOOD:    %d", state.count50),  bx, y, 0xFF88FF44);
        y += 14;
        gfx.text(this.font, String.format("MISS:    %d", state.countMiss), bx, y, 0xFFFF4444);

        super.extractRenderState(gfx, mouseX, mouseY, delta);
    }

    private String computeGrade() {
        double acc = state.getAccuracy();
        if (state.countMiss == 0 && state.count50 == 0 && state.count100 == 0) return "SS";
        if (state.countMiss == 0 && acc >= 99.0) return "S";
        if (acc >= 95.0) return "A";
        if (acc >= 90.0) return "B";
        if (acc >= 80.0) return "C";
        return "D";
    }

    private int gradeColor(String grade) {
        return switch (grade) {
            case "SS" -> 0xFFFFEE00;
            case "S"  -> 0xFFFFBB00;
            case "A"  -> 0xFF88FF44;
            case "B"  -> 0xFF4488FF;
            case "C"  -> 0xFFAA44FF;
            default   -> 0xFFFF4444;
        };
    }

    private void drawCentered(GuiGraphicsExtractor gfx, String text, int cx, int y, int color) {
        gfx.text(this.font, text, cx - this.font.width(text) / 2, y, color);
    }

    private void drawCenteredBig(GuiGraphicsExtractor gfx, String text, int cx, int y, int color) {
        var ps = gfx.pose();
        ps.pushMatrix();
        ps.translate(cx, y);
        ps.scale(2.0f, 2.0f);
        gfx.text(this.font, text, -this.font.width(text) / 2, 0, color);
        ps.popMatrix();               // was pushMatrix() — must pop to restore state
    }
}
