package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.network.payload.SimulationEngineSpeedPayload;
import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class SimulationControllerScreen extends AbstractContainerScreen<SimulationControllerMenu> {
    Player player;
    public LogSlider simulationSpeedSlider;
    public LogSlider frameSpeedSlider;

    public SimulationControllerScreen(SimulationControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        player = inv.player;
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


        simulationSpeedSlider = this.addRenderableWidget(new LogSlider(
                centerX - 70,
                y + 30,
                140,
                20,
                0.001,
                5,
                sim.getSimSpeed(),
                (value) -> String.format("Simulation Speed: %.3fx", value),
                (value) -> PacketDistributor.sendToServer(new SimulationEngineSpeedPayload(value, player.getId()))
        ));

        frameSpeedSlider = this.addRenderableWidget(new LogSlider(
                centerX - 70,
                y + 60,
                140,
                20,
                0.01,
                1,
                sim.getFrameMovementPerTick(),
                (value) -> String.format("Frame Speed: %.3fx", value),
                sim::setFrameMovementPerTick
        ));

    }

    @Override
    protected void containerTick() {
        super.containerTick();

    }

    // --------------------------------------------------
    // Rendering
    // --------------------------------------------------
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
//        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);



        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0x404040, false);

//        graphics.drawString(this.font, "Simulation Speed", this.leftPos + 8, this.topPos + 68, 0x404040, false);

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
    public static class LogSlider extends AbstractSliderButton {

        private final double logMin;
        private final double logMax;

        private final double minVal;
        private final double maxVal;
        private final Function<Double, String> stringFormatter;
        private final Consumer<Double> onValueChange;

        public LogSlider(int x, int y, int width, int height, double minValue, double maxValue, double value, Function<Double, String> stringFormatter, Consumer<Double> onValueChange) {
            super(x, y, width, height, Component.empty(), 0);

            this.minVal = minValue;
            this.maxVal = maxValue;
            this.logMin = Math.log(minValue);
            this.logMax = Math.log(maxValue);

            this.stringFormatter = stringFormatter;
            this.onValueChange = onValueChange;

            this.value = valueToFraction(value);

            updateMessage();
        }

        @Override
        protected void applyValue() {
            double actualValue = fractionToValue(this.value);
            onValueChange.accept(actualValue);
            updateMessage();
        }


        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(stringFormatter.apply(fractionToValue(this.value))));
        }

        private double fractionToValue(double sliderValue) {
            double logValue = logMin + sliderValue * (logMax - logMin);
            return Math.exp(logValue);
        }

        private double valueToFraction(double value) {
            value = Math.clamp(value, minVal, maxVal);
            //the log value has to be in the range 0 to 1
            return (Math.log(value) - logMin) / (logMax - logMin);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            System.out.println("on drag");
            super.onDrag(mouseX, mouseY, dragX, dragY);
            this.setValueFromFraction((mouseX - (this.getX() + 4)) / (this.width - 8));
        }

        /**
         * Sets the value of this slider, given a fraction from 0 to 1 on the x scale of the slider. 0 is minValue, 1 is maxValue
         * @param fraction 'progress' on the slider
         */
        private void setValueFromFraction(double fraction) {
            var newValue = valueToFraction(Mth.lerp(Mth.clamp(fraction, 0, 1), minVal, maxVal));
            if (!Mth.equal(newValue, minVal)) {
                this.applyValue();
            }
        }

        public void setValueExternal(double newSimulationSpeed) {
            this.value = valueToFraction(newSimulationSpeed);
            updateMessage();
        }
    }
}
