package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.payload.SetProtocolPayload;
import de.tyro.mcnetwork.network.payload.SimulationEngineSettingsPayload;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.tyro.mcnetwork.Config.DEV_MODE;

public class SimulationControllerScreen extends AbstractContainerScreen<SimulationControllerMenu> {
    Player player;
    public Button receiveWindowEnabledButton;
    public Button simulationEnabledButton;
    public LogSlider simulationSpeedSlider;
    public ExtendedSlider simulationCommunicationRadius;

    public SimulationControllerScreen(SimulationControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        player = inv.player;
        this.imageWidth = 280;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        SimulationEngine sim = SimulationEngine.getInstance(true);

        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(new Label(x, y, 120, 20, "Packet Collision"));
        y += 15;

        receiveWindowEnabledButton = this.addRenderableWidget(Button.builder(
                        getReceiveWindowLabel(sim),
                        btn -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).receiveWindowActive(!sim.getReceiveWindowActive()).build()))
                .tooltip(Tooltip.create(Component.literal("If this is enabled, NetworkFrames can collided with each other. Collided Frames will be rendered red")))
                .bounds(x, y, 120, 15)
                .build());
        y += 20;

        var receiveWindowMsButton = this.addRenderableWidget(new ExtendedSlider(
                x,
                y,
                120,
                15,
                Component.literal("Coll. Window "),
                Component.literal(" ms"),
                1,
                10,
                1,
                true));
        y += 20;
        receiveWindowMsButton.setTooltip(Tooltip.create(Component.literal("How long it takes to 'receive' Frames. Each frame blocks for this amount of time.")));

        var receiveWindowSizeButton = this.addRenderableWidget(new ExtendedSlider(
                x,
                y,
                120,
                15,
                Component.literal("Packets per Window "),
                Component.literal(""),
                1,
                10,
                2,
                true));
        y += 15;
        receiveWindowSizeButton.setTooltip(Tooltip.create(Component.literal("How many packets can be received simultaneously.")));

        // ---- OTHER ----- //

        y += 15;
        this.addRenderableWidget(new Label(x, y, 120, 15, "Other"));
        y += 20;

        var protocolLabel = this.addRenderableWidget(new Label(x, y, 120, 20, "Set all protocols"));
        protocolLabel.setTooltip(Tooltip.create(Component.literal("Sets all protocols of all nodes in the simulation. Future nodes will have this protocol.")));
        y += 15;

        // Protocol buttons
        String[] protocols = {"AODV", "DSR", "OLSR"};
        int spacing = 1;
        int buttonWidth = (120 - spacing * protocols.length) / 4;


        for (int i = 0; i < protocols.length; i++) {
            final String protocol = protocols[i];
            this.addRenderableWidget(Button.builder(
                            Component.literal(protocols[i]),
                            btn -> PacketDistributor.sendToServer(new SetProtocolPayload(protocol + "Protocol", null)))
                    .bounds(x + i * (buttonWidth + spacing), y, buttonWidth, 15)
                    .build());
        }

        y += 20;

        if (DEV_MODE.getAsBoolean()) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Reload Quests"),
                            btn -> TopicManager.getInstance().reloadTopics())
                    .bounds(x, y, 120, 15)
                    .build()
            );
        }

        // ----- SIMULATION INFORMATION ----- //
        x += imageWidth / 2 + 10;
        y = topPos;

        this.addRenderableWidget(new Label(x, y, 120, 15, "Simulation Info"));
        y += 20;

        this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Simulation Time " + sim.getSimTime()));
        y += 20;

        this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Registered Nodes: " + sim.getNodeList().size()));
        y += 20;

        this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Registered Frames: " + sim.getFrameList().size()));
        y += 25;


        // ----- SIMULATION SETTINGS ----- //


        this.addRenderableWidget(new Label(x, y, 120, 15, "Simulation Settings"));
        y += 15;

        simulationEnabledButton = this.addRenderableWidget(Button.builder(
                        simulationEnabledButtonLabel(sim),
                        btn -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).paused(!sim.isPaused()).build()))
                .bounds(x, y, 120, 15)
                .build());
        y += 20;
        simulationEnabledButton.setTooltip(Tooltip.create(Component.literal("Pauses/Unpauses the Simulation")));

        simulationSpeedSlider = this.addRenderableWidget(new LogSlider(
                x,
                y,
                120,
                15,
                0.001,
                1,
                sim.getSimSpeed(),
                (value) -> String.format("Sim. Speed: %.3fx", value),
                (value) -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).simSpeed(value).build())
        ));
        simulationSpeedSlider.setTooltip(Tooltip.create(Component.literal("After changing the speed, it can take a moment for the packets and nodes to normalize to this new speed. E.g. you could see extensively more packets for a moment when slowing down the simulation.")));
        y += 20;

        simulationCommunicationRadius = this.addRenderableWidget(new ExtendedSlider(x, y, 120, 15, Component.literal("Comm. Range"), Component.empty(), 5, 25, sim.getCommRange(), true) {
            @Override
            protected void applyValue() {
                PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).commRadius((int) getValue()).build());
            }
        });
        simulationCommunicationRadius.setTooltip(Tooltip.create(Component.literal("How far a packet can travel between nodes. You might need to reset all protocols when changing this value, to stop unwanted side effects.")));

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
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) renderable.render(graphics, mouseX, mouseY, partialTick);

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Optional: draw background texture here
    }

    public Component simulationEnabledButtonLabel(SimulationEngine sim) {
        return Component.literal(sim.isPaused() ? "Simulation: OFF" : "Simulation: ON");
    }

    public Component getReceiveWindowLabel(SimulationEngine sim) {
        return Component.literal(sim.getReceiveWindowActive() ? "Frame Collision: ON" : "Frame Collision: OFF");
    }

    public static class Label extends AbstractWidget {
        Supplier<String> supplier;

        public Label(int x, int y, int width, int height, String message) {
            super(x, y, width, height, Component.empty());
            this.supplier = () -> message;
        }

        public Label(int x, int y, int width, int height, Supplier<String> supplier) {
            super(x, y, width, height, Component.empty());
            this.supplier = supplier;
        }


        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.drawString(Minecraft.getInstance().font, supplier.get(), getX(), getY(), RenderUtil.Color.WHITE.value);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

        }
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
         *
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
