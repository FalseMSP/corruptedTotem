package com.redsmods.client.screen;

import com.redsmods.client.beatmap.OsuBeatmap;
import com.redsmods.client.beatmap.OsuParser;
import com.redsmods.client.game.GameState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Song selection / beatmap import menu.
 *
 * Players can:
 *  - Import a .osu file via a system file picker
 *  - See a list of all imported beatmaps this session
 *  - Launch the game with the selected beatmap
 */
public class OsuMenuScreen extends Screen {

    private final Screen parent;

    /** Beatmaps imported this Minecraft session */
    private static final List<OsuBeatmap> importedBeatmaps = new ArrayList<>();
    private static OsuBeatmap selected = null;

    // UI state
    private String statusMessage = "";
    private long statusExpiry = 0;
    private int scrollOffset = 0;

    // Layout
    private static final int LIST_ITEM_H = 22;
    private static final int LIST_TOP_OFFSET = 80;

    public OsuMenuScreen(Screen parent) {
        super(Component.translatable("corrupted.mania.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int bw = 160;
        int bx = this.width / 2 - bw / 2;

        // Import button
        this.addRenderableWidget(Button.builder(
            Component.translatable("corrupted.mania.screen.import"),
            btn -> openFilePicker()
        ).pos(bx, this.height - 80).size(bw, 20).build());

        // Play button
        this.addRenderableWidget(Button.builder(
            Component.translatable("corrupted.mania.screen.play"),
            btn -> {
                if (selected != null) {
                    assert this.minecraft != null;
                    GameState gs = new GameState(selected);
                    this.minecraft.setScreen(new OsuGameScreen(this, gs));
                } else {
                    setStatus("Select a beatmap first!");
                }
            }
        ).pos(bx, this.height - 56).size(bw, 20).build());

        // Back button
        this.addRenderableWidget(Button.builder(
            Component.translatable("corrupted.mania.screen.back"),
            btn -> {
                assert this.minecraft != null;
                this.minecraft.setScreen(parent);
            }
        ).pos(bx, this.height - 32).size(bw, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
        gfx.fill(0, 0, this.width, this.height, 0xFF0A0A18);

        // Title
        String title = "CorruptedMania – Song Select";
        gfx.text(this.font, title, (this.width - this.font.width(title)) / 2, 16, 0xFFFFEE44);

        // Key hint
        String hint = "Keys: [D] [F] [J] [K]  |  [P] Pause  |  [ESC] Back";
        gfx.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 32, 0xFF888888);

        // Column headers
        gfx.text(this.font, "Imported Beatmaps:", 20, 56, 0xFFCCCCCC);

        // Beatmap list
        int listY = LIST_TOP_OFFSET;
        int listH = this.height - LIST_TOP_OFFSET - 96;
        int maxVisible = listH / LIST_ITEM_H;

        // Clamp scroll
        int maxScroll = Math.max(0, importedBeatmaps.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Clip region background
        gfx.fill(10, listY, this.width - 10, listY + listH, 0x55111133);

        int drawn = 0;
        for (int i = scrollOffset; i < importedBeatmaps.size() && drawn < maxVisible; i++, drawn++) {
            OsuBeatmap bm = importedBeatmaps.get(i);
            int iy = listY + drawn * LIST_ITEM_H;
            boolean isSel = (bm == selected);

            // Row background
            gfx.fill(12, iy, this.width - 12, iy + LIST_ITEM_H - 2,
                    isSel ? 0xBB2244AA : 0x33334466);

            // Row text
            String name = bm.getDisplayName();
            if (this.font.width(name) > this.width - 30) {
                name = this.font.plainSubstrByWidth(name, this.width - 50) + "…";
            }
            gfx.text(this.font, name, 16, iy + (LIST_ITEM_H - 8) / 2,
                    isSel ? 0xFFFFFFFF : 0xFFCCCCCC);

            // Note count
            String ncount = bm.notes.size() + " notes";
            gfx.text(this.font, ncount,
                    this.width - 14 - this.font.width(ncount), iy + (LIST_ITEM_H - 8) / 2,
                    0xFF888888);
        }

        if (importedBeatmaps.isEmpty()) {
            String empty = "No beatmaps imported yet. Click \"Import Beatmap\" to load a .osu file.";
            gfx.text(this.font, empty,
                    (this.width - this.font.width(empty)) / 2,
                    listY + listH / 2 - 4, 0xFF666688);
        }

        // Status message
        if (System.currentTimeMillis() < statusExpiry) {
            int sx = (this.width - this.font.width(statusMessage)) / 2;
            gfx.text(this.font, statusMessage, sx, this.height - 96, 0xFFFFAA44);
        }

        super.extractRenderState(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // List item click
        int listY = LIST_TOP_OFFSET;
        int listH = this.height - LIST_TOP_OFFSET - 96;
        int maxVisible = listH / LIST_ITEM_H;

        if (event.x() >= 12 && event.x() <= this.width - 12 && event.y() >= listY && event.y() < listY + listH) {
            int idx = (int)((event.y() - listY) / LIST_ITEM_H) + scrollOffset;
            if (idx >= 0 && idx < importedBeatmaps.size()) {
                selected = importedBeatmaps.get(idx);
                // Double click → play
                setStatus("Selected: " + selected.getDisplayName());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        scrollOffset = Math.max(0, scrollOffset - (int) vScroll);
        return true;
    }

    // ── File import ───────────────────────────────────────────────────────────

    /**
     * Opens a system file chooser dialog on a background thread, then parses
     * the chosen .osu file. Uses Swing's JFileChooser since Minecraft does not
     * provide a native file picker API on all platforms.
     */
    private void openFilePicker() {
        Thread pickerThread = new Thread(() -> {
            String result;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Build a null-terminated filter array: one entry "*.osu"
                org.lwjgl.PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.osu")).flip();

                result = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select a .osu beatmap file",
                        "",
                        filters,
                        "osu! Beatmap (*.osu)",
                        false
                );
            }

            if (result != null && !result.isEmpty()) {
                importOsuFile(new java.io.File(result));
            } else {
                setStatus("No file selected.");
            }
        }, "CorruptedMania-FilePicker");
        pickerThread.setDaemon(true);
        pickerThread.start();
    }

    private void importOsuFile(File file) {
        try {
            OsuBeatmap beatmap = OsuParser.parse(file);
            if (beatmap.notes.isEmpty()) {
                setStatus("No hit objects found in: " + file.getName());
                return;
            }
            // Run on main thread
            assert this.minecraft != null;
            this.minecraft.execute(() -> {
                importedBeatmaps.add(beatmap);
                selected = beatmap;
                setStatus("Loaded: " + beatmap.getDisplayName() + " (" + beatmap.notes.size() + " notes)");
            });
        } catch (Exception e) {
            setStatus("Error loading beatmap: " + e.getMessage());
        }
    }

    private void setStatus(String msg) {
        statusMessage = msg;
        statusExpiry = System.currentTimeMillis() + 4000;
    }
}
