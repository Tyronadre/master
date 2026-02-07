package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import org.jetbrains.annotations.NotNull;

public class SimulationControllerScreen extends AbstractContainerScreen<SimulationControllerMenu> {

    public SimulationControllerScreen(SimulationControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        SimulationEngine sim = SimulationEngine.getInstance();

        int centerX = this.leftPos + this.imageWidth / 2;
        int y = this.topPos + 30;

        this.addRenderableWidget(Button.builder(getPauseLabel(sim), btn -> {
            togglePause(sim);
            btn.setMessage(getPauseLabel(sim));
        }).bounds(centerX - 40, y, 80, 20).build());



        this.addRenderableWidget(new SimulationSpeedSlider(
                centerX - 70,
                y + 30,
                140,
                20
        ));
    }

    // --------------------------------------------------
    // Rendering
    // --------------------------------------------------
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0x404040, false);

        graphics.drawString(this.font, "Simulation Speed", this.leftPos + 8, this.topPos + 68, 0x404040, false);

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Optional: draw background texture here
    }

    // --------------------------------------------------
    // Pause handling
    // --------------------------------------------------
    private void togglePause(SimulationEngine sim) {
        sim.setPaused(!sim.isPaused());
    }

    private Component getPauseLabel(SimulationEngine sim) {
        return Component.literal(sim.isPaused() ? "Resume" : "Pause");
    }

    // --------------------------------------------------
    // Slider Implementation
    // --------------------------------------------------
    public static class SimulationSpeedSlider extends AbstractSliderButton {

        private static final double MIN_SPEED = 0.001;
        private static final double MAX_SPEED = 5.0;

        private static final double LOG_MIN = Math.log10(MIN_SPEED);
        private static final double LOG_MAX = Math.log10(MAX_SPEED);

        public SimulationSpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), speedToSlider(
                    SimulationEngine.getInstance().getSimSpeed()
            ));
            updateMessage();
        }

        @Override
        protected void applyValue() {
            double speed = sliderToSpeed(this.value);
            SimulationEngine.getInstance().setSimSpeed(speed);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double speed = sliderToSpeed(this.value);
            this.setMessage(Component.literal(
                    String.format("Speed: %.3fx", speed)
            ));
        }

        private static double sliderToSpeed(double sliderValue) {
            double logValue = LOG_MIN + sliderValue * (LOG_MAX - LOG_MIN);
            return Math.pow(10, logValue);
        }

        private static double speedToSlider(double speed) {
            speed = Math.clamp(speed, MIN_SPEED, MAX_SPEED);
            return (Math.log10(speed) - LOG_MIN) / (LOG_MAX - LOG_MIN);
        }
    }
}
