package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.payload.SetProtocolPayload;
import de.tyro.mcnetwork.network.payload.SimulationEngineSettingsPayload;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimulationControllerScreen extends AbstractContainerScreen<SimulationControllerMenu> {
    Player player;
    public Button receiveWindowEnabledButton;
    public ExtendedSlider receiveWindowSizeMsButton;
    public Button simulationEnabledButton;
    public LogSlider simulationSpeedSlider;
    public ExtendedSlider simulationCommunicationRadius;
    private Button[] protocolButtons;

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

        var receiveWindowTitle = this.addRenderableWidget(new Label(x, y, 120, 20, "Packet Collision"));
        y += 15;

        receiveWindowEnabledButton = this.addRenderableWidget(Button.builder(
                        getReceiveWindowLabel(sim),
                        btn -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).receiveWindowActive(!sim.getReceiveWindowActive()).build()))
                .tooltip(Tooltip.create(Component.literal("If this is enabled, NetworkFrames can collided with each other. Collided Frames will be rendered red")))
                .bounds(x, y, 120, 15)
                .build());
        y += 20;

        receiveWindowSizeMsButton = this.addRenderableWidget(new ExtendedSlider(
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

        var receiveWindowMaxPacketsPerWindow = this.addRenderableWidget(new ExtendedSlider(
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

        // ---- OTHER ----- //

        y += 15;
        var otherLabel = this.addRenderableWidget(new Label(x, y, 120, 15, "Other"));
        y += 20;

        var protocolLabel = this.addRenderableWidget(new Label(x, y, 120, 20, "Set all protocols"));
        protocolLabel.setTooltip(Tooltip.create(Component.literal("Sets all protocols of all nodes in the simulation. Future nodes will have this protocol.")));
        y += 15;

        // Protocol buttons
        String[] protocols = {"AODV", "DSR", "LAR", "OLSR"};
        int spacing = 1;
        int buttonWidth = (120 - spacing * protocols.length) / 4;

        protocolButtons = new Button[protocols.length];

        for (int i = 0; i < protocols.length; i++) {
            final String protocol = protocols[i];
            protocolButtons[i] = this.addRenderableWidget(Button.builder(
                            Component.literal(protocols[i]),
                            btn -> PacketDistributor.sendToServer(new SetProtocolPayload(protocol + "Protocol", null)))
                    .bounds(x + i * (buttonWidth + spacing), y, buttonWidth, 15)
                    .build());
        }

        y += 20;

        var reloadQuestButton = this.addRenderableWidget(Button.builder(
                        Component.literal("Reload Quests"),
                        btn -> TopicManager.getInstance().reloadTopics())
                .bounds(x, y, 120, 15)
                .build()
        );

        // ----- SIMULATION INFORMATION ----- //
        x += imageWidth / 2 + 10;
        y = topPos;

        var simInfoLabel = this.addRenderableWidget(new Label(x, y, 120, 15, "Simulation Info"));
        y += 20;

        var simTime = this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Simulation Time " + sim.getSimTime()));
        y += 20;

        var simNodes = this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Registered Nodes: " + sim.getNodeList().size()));
        y += 20;

        var simFrames = this.addRenderableWidget(new Label(x, y, 120, 15, () -> "Registered Frames: " + sim.getFrameList().size()));
        y += 25;


        // ----- SIMULATION SETTINGS ----- //


        var simSettingsLabel = this.addRenderableWidget(new Label(x, y, 120, 15, "Simulation Settings"));
        y += 15;

        simulationEnabledButton = this.addRenderableWidget(Button.builder(
                        simulationEnabledButtonLabel(sim),
                        btn -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).paused(!sim.isPaused()).build()))
                .bounds(x, y, 120, 15)
                .build());
        y += 20;

        simulationSpeedSlider = this.addRenderableWidget(new LogSlider(
                x,
                y,
                120,
                15,
                0.001,
                5,
                sim.getSimSpeed(),
                (value) -> String.format("Sim. Speed: %.3fx", value),
                (value) -> PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).simSpeed(value).build())
        ));
        y += 20;

        simulationCommunicationRadius = this.addRenderableWidget(new ExtendedSlider(x, y, 120, 15, Component.literal("Comm. Range"), Component.empty(), 5, 25, sim.getCommRange(), true) {
            @Override
            protected void applyValue() {
                PacketDistributor.sendToServer(SimulationEngineSettingsPayload.Builder(sim).commRadius((int) getValue()).build());
            }
        });
        y += 20;

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
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

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
