package de.tyro.mcnetwork.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.network.payload.NewNetworkFramePayload;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class NetworkFrameEntity extends Entity implements IEntityWithComplexSpawn {
    static Logger logger = LogUtils.getLogger();

    public INetworkNode getFrom() {
        return from;
    }

    public INetworkNode getTo() {
        return to;
    }

    public int getTtl() {
        return ttl;
    }

    public INetworkPacket getPacket() {
        return packet;
    }

    private INetworkNode from;
    private INetworkNode to;
    private int ttl;
    private INetworkPacket packet;
    private boolean initialized = false;

    public NetworkFrameEntity(Level level, Vec3 pos) {
        super(EntityRegistry.NETWORK_FRAME_ENTITY.get(), level);
        this.setPos(pos);
        this.noPhysics = true;
    }

    public NetworkFrameEntity(Level level) {
        this(level, Vec3.ZERO);
    }


    public NetworkFrameEntity(Level level, INetworkNode from, INetworkNode to, int ttl, INetworkPacket packet) {
        this(level, from.getPos());
        if (from == to) throw new IllegalArgumentException();

        this.from = from;
        this.to = to;
        this.ttl = ttl;
        this.packet = packet;
        packet.setFrame(this);

        initialized = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public void tick() {
        if (!initialized) return;

        super.tick();
        super.move(MoverType.SELF, this.getDeltaMovement());
        if (!level().isClientSide && hasArrived()) {
            to.onFrameReceive(this);
            discard();
        }
    }

    private boolean hasArrived() {
        return getPosition(0).distanceTo(to.getPos()) < 0.1;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        if (this.getFrom() == null) {
            this.discard();
            return;
        }
        NewNetworkFramePayload.STREAM_CODEC.encode(buffer, new NewNetworkFramePayload(this));
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        try {
            var payload = NewNetworkFramePayload.STREAM_CODEC.decode(additionalData);
            from = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level(), payload.from());
            to = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level(), payload.to());
            ttl = payload.ttl();
            packet = payload.packet();
            packet.setFrame(this);

            initialized = true;
        } catch (Exception e) {
            this.discard();
        }
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        if (!initialized) return Vec3.ZERO;

        var sim = SimulationEngine.getInstance(level().isClientSide());
        if (sim.isPaused()) return Vec3.ZERO;

        var tickSpeed = sim.getSimSpeed() * 10;

        Vec3 toTarget = to.getPos().subtract(getPosition(0));
        double distance = toTarget.length();

        if (distance < 1e-9) {
            return Vec3.ZERO;
        }

        // Prevent overshooting
        if (distance <= tickSpeed) {
            return toTarget;
        }

        Vec3 direction = toTarget.scale(1.0 / distance);
        return direction.scale(tickSpeed);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha) {
        var mc = Minecraft.getInstance();

        poseStack.pushPose();

        poseStack.translate(0, 0.5, 0); //render above
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);

        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);

        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "UDP @ " + from.getIP().toString() + " -> " + to.getIP().toString() + " TTL: " + ttl, alpha, 0, -15, poseStack, bufferSource, packedLight);

        poseStack.popPose();

        packet.render(poseStack, bufferSource, packedLight, alpha);

        poseStack.popPose();
    }
}
