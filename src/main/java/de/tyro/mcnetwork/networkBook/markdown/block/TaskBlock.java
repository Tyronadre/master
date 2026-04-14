package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TaskBlock extends Block implements GuiEventListener {

    private final Minecraft mc = Minecraft.getInstance();
    private final List<TaskEntry> tasks;

    private int x, y, w, h;
    private int focusedTask = -1;

    public TaskBlock(List<List<Block>> questionBlocks, List<String> answers) {
        this.tasks = new ArrayList<>();
        for (int i = 0; i < questionBlocks.size(); i++) {
            tasks.add(new TaskEntry(questionBlocks.get(i), answers.get(i)));
        }
    }

    private void drawRect(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y, x + 1, y + h, color);
        gg.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        this.x = x;
        this.y = y;
        this.w = width;

        int curY = y;

        for (int i = 0; i < tasks.size(); i++) {
            TaskEntry task = tasks.get(i);
            curY = task.render(gg, x, curY, width, i == focusedTask);
        }

        this.h = curY - y;
        return this.h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        focusedTask = -1;

        int curY = y;

        for (int i = 0; i < tasks.size(); i++) {
            TaskEntry task = tasks.get(i);

            if (task.mouseClicked(mx, my, x, curY, w)) {
                if (task.isInputFocused(mx, my)) {
                    focusedTask = i;
                }
                return true;
            }

            curY = task.getBottomY();
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedTask >= 0) {
            return tasks.get(focusedTask).charTyped(codePoint);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedTask >= 0) {
            return tasks.get(focusedTask).keyPressed(keyCode);
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        focusedTask = -1;
    }

    @Override
    public boolean isFocused() {
        return focusedTask != -1;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ========================= TASK ENTRY =========================

    private class TaskEntry {

        private final List<Block> questionBlocks;
        private final String answer;

        private String userInput = "";
        private boolean correct = false;
        private int cursor = 0;

        private boolean submitted = false;
        private boolean showSolution = false;

        private int bottomY;

        private long lastBlink = 0;
        private boolean cursorVisible = true;

        // cached layout for hit detection
        private int tfX, tfY, tfW, tfH;
        private int btnX, btnY;

        public TaskEntry(List<Block> questionBlocks, String answer) {
            this.questionBlocks = questionBlocks;
            this.answer = answer;
        }

        public int render(GuiGraphics gg, int x, int y, int width, boolean focused) {
            Font font = mc.font;
            int curY = y;

            curY += 2;
            for (Block b : questionBlocks) {
                curY += b.render(gg, x, curY, width, font.lineHeight);
            }
            curY += 2;

            tfX = x;
            tfY = curY;
            tfW = width;
            tfH = 15;

            int bg = 0xFF222233;
            if (submitted) {
                bg = correct ? 0xFF224422 : 0xFF442222;
            }

            if (showSolution) {
                tfW -= font.width(answer) + 10;
            }

            int border = focused ? 0xFF66AAFF : 0xFF888888;

            gg.fill(tfX, tfY, tfX + tfW, tfY + tfH, bg);
            drawRect(gg, tfX, tfY, tfW, tfH, border);

            String display = userInput;

            if (focused && !correct) {
                long now = System.currentTimeMillis();
                if (now - lastBlink > 500) {
                    cursorVisible = !cursorVisible;
                    lastBlink = now;
                }
                if (cursorVisible) {
                    display = display.substring(0, cursor) + "|" + display.substring(cursor);
                }
            }

            display = font.plainSubstrByWidth(display, tfW - 8);
            gg.drawString(font, display, tfX + 4, tfY + 4, 0xFFFFFF);

            if (submitted && showSolution && !correct) {
                gg.drawString(font, answer, tfX + tfW + 8, tfY + 4, 0xFFFF00);
            }

            curY += tfH + 4;

            // Buttons (per task)
            int btnH = 15;
            int btnW = 80;

            btnX = tfX;
            btnY = curY;

            //Submit button
            if (!submitted || !correct) {
                gg.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF224466);
                drawRect(gg, btnX, btnY, btnW, btnH, 0xFFAAAAAA);
                gg.drawCenteredString(font, "Submit", btnX + btnW / 2, btnY + 4, 0xFFFFFF);
            }
            //show answer button
            if (submitted && !correct) {
                int btnX2 = btnX + btnW + 12;
                gg.fill(btnX2, btnY, btnX2 + btnW, btnY + btnH, 0xFF446622);
                drawRect(gg, btnX2, btnY, btnW, btnH, 0xFFAAAAAA);
                gg.drawCenteredString(font, "Solution", btnX2 + btnW / 2, btnY + 4, 0xFFFFFF);
            }
            if (!(submitted && correct)) curY += btnH + 4;

            bottomY = curY;
            return curY;
        }

        public boolean mouseClicked(double mx, double my, int baseX, int baseY, int width) {
            // input focus
            if (inside(mx, my, tfX, tfY, tfW, tfH) && !correct) {
                return true;
            }

            int btnH = 22;
            int btnW = 90;

            if ((!submitted || !correct) && inside(mx, my, btnX, btnY, btnW, btnH)) {
                submitted = true;
                evaluate();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }

            if (submitted && !correct && inside(mx, my, btnX + btnW + 12, btnY, btnW, btnH)) {
                showSolution = true;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }

            return false;
        }

        public boolean isInputFocused(double mx, double my) {
            return inside(mx, my, tfX, tfY, tfW, tfH) && !correct;
        }

        public boolean charTyped(char c) {
            if (correct) return false;

            if (c >= 32) {
                userInput = userInput.substring(0, cursor) + c + userInput.substring(cursor);
                cursor++;
                return true;
            }
            return false;
        }

        public boolean keyPressed(int keyCode) {
            if (correct) return false;

            if (keyCode == 259 && cursor > 0) {
                userInput = userInput.substring(0, cursor - 1) + userInput.substring(cursor);
                cursor--;
                return true;
            }
            if (keyCode == 262 && cursor < userInput.length()) {
                cursor++;
                return true;
            }
            if (keyCode == 263 && cursor > 0) {
                cursor--;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE && cursor < userInput.length() - 2) {
                userInput = userInput.substring(0, cursor) + userInput.substring(cursor + 1);
                return true;
            }
            return false;
        }

        public void evaluate() {
            correct = userInput.trim().equalsIgnoreCase(answer.trim());
        }

        public int getBottomY() {
            return bottomY;
        }
    }
}