package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.sniffer.CapturedFrame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NetworkSnifferScreen extends Screen {
    private EditBox timestampFilterBefore;
    private EditBox timestampFilterAfter;
    private EditBox frameSrcFilter;
    private EditBox frameDstFilter;
    private EditBox packetSrcFilter;
    private EditBox packetDstFilter;
    private EditBox packetTypeFilter;

    private FrameList frameList;
    private CapturedFrame selectedFrame;

    public NetworkSnifferScreen() {
        super(Component.literal("Network Sniffer"));
    }

    @Override
    protected void init() {
        super.init();

        int x = 50;
        int y = 5;

        timestampFilterBefore = new EditBox(font, x, y, 35, 20, Component.literal("Before Timestamp"));
        timestampFilterBefore.setHint(Component.literal("Before Timestamp"));
        addRenderableWidget(timestampFilterBefore);
        x += 40;
        timestampFilterAfter = new EditBox(font, x, y, 35, 20, Component.literal("After Timestamp"));
        timestampFilterAfter.setHint(Component.literal("After Timestamp"));
        addRenderableWidget(timestampFilterAfter);
        x += 50;

        frameSrcFilter = new EditBox(font, x, y, 80, 20, Component.literal("Frame Src"));
        frameSrcFilter.setHint(Component.literal("Frame Src"));
        addRenderableWidget(frameSrcFilter);
        x += 90;

        frameDstFilter = new EditBox(font, x, y, 80, 20, Component.literal("Frame Dst"));
        frameDstFilter.setHint(Component.literal("Frame Dst"));
        addRenderableWidget(frameDstFilter);
        x += 90;

        packetSrcFilter = new EditBox(font, x, y, 80, 20, Component.literal("Packet Src"));
        packetSrcFilter.setHint(Component.literal("Packet Src"));
        addRenderableWidget(packetSrcFilter);
        x += 90;

        packetDstFilter = new EditBox(font, x, y, 80, 20, Component.literal("Packet Dst"));
        packetDstFilter.setHint(Component.literal("Packet Dst"));
        addRenderableWidget(packetDstFilter);
        x += 90;

        packetTypeFilter = new EditBox(font, x, y, 80, 20, Component.literal("Packet Type"));
        packetTypeFilter.setHint(Component.literal("Packet Type"));
        addRenderableWidget(packetTypeFilter);
        x += 90;

        Button filterButton = Button.builder(Component.literal("Filter"), this::applyFilters)
                .pos(x, y)
                .size(60, 20)
                .build();
        addRenderableWidget(filterButton);

        frameList = new FrameList(minecraft, width, height - 200, 50, height - 50);
        addRenderableWidget(frameList);

        applyFilters(null); // Initial load
    }

    @Override
    public void tick() {
        super.tick();
        addNewValues();
    }

    private void addNewValues() {
        var currentValues = frameList.children().size();
        var newValues = 0;
        for (CapturedFrame value : SimulationEngine.getInstance(true).getCapturedFrames()) {
            if (matchesFilter(value)) {
                newValues++;
            }
            if (newValues > currentValues) {
                frameList.addFrame(value);
            }
        }

    }

    private void applyFilters(Button button) {
        List<CapturedFrame> filtered = new ArrayList<>();
        SimulationEngine sim = SimulationEngine.getInstance(true); // Client side
        for (CapturedFrame frame : sim.getCapturedFrames()) {
            if (matchesFilter(frame)) {
                filtered.add(frame);
            }
        }
        frameList.updateFrames(filtered);
    }

    private boolean matchesFilter(CapturedFrame frame) {
        try {
            if (!timestampFilterBefore.getValue().isEmpty() && frame.getTimestamp() >= Long.parseLong(timestampFilterBefore.getValue())) return false;
            if (!timestampFilterAfter.getValue().isEmpty() && frame.getTimestamp() <= Long.parseLong(timestampFilterAfter.getValue())) return false;
            if (!frameSrcFilter.getValue().isEmpty() && !matchesIP(frame.getFrameFrom(), frameSrcFilter.getValue())) return false;
            if (!frameDstFilter.getValue().isEmpty() && !matchesIP(frame.getFrameTo(), frameDstFilter.getValue())) return false;
            if (!packetSrcFilter.getValue().isEmpty() && !matchesIP(frame.getPacketOriginator(), packetSrcFilter.getValue())) return false;
            if (!packetDstFilter.getValue().isEmpty() && !matchesIP(frame.getPacketDestination(), packetDstFilter.getValue())) return false;
            if (!packetTypeFilter.getValue().isEmpty() && !frame.getPacketType().getSimpleName().toLowerCase().contains(packetTypeFilter.getValue().toLowerCase())) return false;
        } catch (Exception ignored) {

        }
        return true;
    }

    private boolean matchesIP(IP ip, String filter) {
        return ip != null && ip.toString().contains(filter);
    }

    @Override
    public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gg, mouseX, mouseY, partialTicks);
        super.render(gg, mouseX, mouseY, partialTicks);

        // Draw table headers
        int headerY = 35;
        gg.drawString(font, "Timestamp", 20, headerY, 0xFFFFFFFF);
        gg.drawString(font, "Frame Src", 120, headerY, 0xFFFFFFFF);
        gg.drawString(font, "Frame Dst", 200, headerY, 0xFFFFFFFF);
        gg.drawString(font, "Packet Src", 280, headerY, 0xFFFFFFFF);
        gg.drawString(font, "Packet Dst", 360, headerY, 0xFFFFFFFF);
        gg.drawString(font, "Packet Type", 440, headerY, 0xFFFFFFFF);

        if (selectedFrame != null) {
            renderPacketDetails(gg, selectedFrame, 20, height - 130);
        }
    }

    private void renderPacketDetails(GuiGraphics gg, CapturedFrame frame, int x, int y) {
        gg.drawString(font, "Packet Details:", x, y, 0xFFFFFFFF);
        gg.drawString(font, "Type: " + frame.getPacketType().getSimpleName(), x, y + 15, 0xFFFFFFFF);
        gg.drawString(font, "Originator: " + frame.getPacketOriginator(), x, y + 30, 0xFFFFFFFF);
        gg.drawString(font, "Destination: " + frame.getPacketDestination(), x, y + 45, 0xFFFFFFFF);
        // Add more if needed
        var pose = gg.pose();
        pose.pushPose();
        pose.translate(x + 300, y, 0);
        pose.scale(2f, 2f, 2f);
        frame.getPacket().render(new RenderUtil(gg));
        pose.popPose();
    }

    private class FrameList extends AbstractSelectionList<FrameList.FrameEntry> {

        public FrameList(net.minecraft.client.Minecraft minecraft, int width, int height, int y0, int y1) {
            super(minecraft, width, height, y0, 15);
        }

        public void updateFrames(List<CapturedFrame> frames) {
            clearEntries();
            for (CapturedFrame frame : frames) {
                addEntry(new FrameEntry(frame));
            }
        }

        @Override
        public void setSelected(FrameEntry entry) {
            super.setSelected(entry);
            selectedFrame = entry != null ? entry.frame : null;
        }

        @Override
        protected int getScrollbarPosition() {
            return width - 6;
        }

        @Override
        public int getRowWidth() {
            return width - 12;
        }


        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

        }

        public void addFrame(CapturedFrame value) {
            addEntry(new FrameEntry(value));
        }


        private class FrameEntry extends AbstractSelectionList.Entry<FrameEntry> {
            private final CapturedFrame frame;

            public FrameEntry(CapturedFrame frame) {
                this.frame = frame;
            }

            @Override
            public void render(GuiGraphics gg, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
                gg.drawString(font, String.valueOf(frame.getTimestamp()), left + 20, top + 3, 0xFFFFFFFF);
                gg.drawString(font, frame.getFrameFrom() != null ? frame.getFrameFrom().toString() : "?", left + 120, top + 3, 0xFFFFFFFF);
                gg.drawString(font, frame.getFrameTo() != null ? frame.getFrameTo().toString() : "?", left + 200, top + 3, 0xFFFFFFFF);
                gg.drawString(font, frame.getPacketOriginator() != null ? frame.getPacketOriginator().toString() : "?", left + 280, top + 3, 0xFFFFFFFF);
                gg.drawString(font, frame.getPacketDestination() != null ? frame.getPacketDestination().toString() : "?", left + 360, top + 3, 0xFFFFFFFF);
                gg.drawString(font, frame.getPacketType().getSimpleName(), left + 450, top + 3, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                frameList.setSelected(this);
                return true;
            }
        }

    }
}
