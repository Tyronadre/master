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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.slf4j.Logger;

public class NetworkFrameEntity extends Entity implements IEntityWithComplexSpawn {
    static Logger logger = LogUtils.getLogger();

    public enum State {
        TRAVELING,
        ARRIVED,
        INTERFERED
    }

    private State state = State.TRAVELING;
    private int interferedTicks = 0;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(NetworkFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INTERFERED_TICKS = SynchedEntityData.defineId(NetworkFrameEntity.class, EntityDataSerializers.INT);

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
    private Vec3 simulationPosition; // Position calculated from simulation

    public static double M_PER_MS = 300000; //~lightspeed

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
        this.simulationPosition = from.getPos();
        packet.setFrame(this);
        var sim = SimulationEngine.getInstance(level.isClientSide);
        sim.registerNetworkFrame(this);

        initialized = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATE, State.TRAVELING.ordinal());
        builder.define(DATA_INTERFERED_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        state = State.values()[compound.getInt("state")];
        interferedTicks = compound.getInt("interferedTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("state", state.ordinal());
        compound.putInt("interferedTicks", interferedTicks);
    }

    public void simTick() {
        if (!initialized) return;

        if (state == State.TRAVELING) {
            simulationPosition = simulationPosition.add(getSimDeltaMovement());

            if (hasArrived()) {
                setPos(simulationPosition);

                state = State.ARRIVED;
//                entityData.set(DATA_STATE, state.ordinal());

                try {
                    to.onFrameReceive(this);
                    if (state == State.ARRIVED) {
                        SimulationEngine.getInstance(level().isClientSide).removeNetworkFrame(this);
                        discard();
                    }
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }

        if (state == State.INTERFERED) {
            interferedTicks++;
//            entityData.set(DATA_INTERFERED_TICKS, interferedTicks);
            if (interferedTicks > 100) {
                SimulationEngine.getInstance(level().isClientSide).removeNetworkFrame(this);
                discard();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!initialized) return;

//        if (!level().isClientSide) return

        Vec3 currentPos = getPosition(1);
        Vec3 diff = simulationPosition.subtract(currentPos);
        if (state != State.TRAVELING) return;
        if (diff.lengthSqr() > 1e-6) move(MoverType.SELF, diff);
    }

    public Vec3 getSimDeltaMovement() {
        if (!initialized) return Vec3.ZERO;

        var sim = SimulationEngine.getInstance(level().isClientSide());
        if (sim.isPaused()) return Vec3.ZERO;

        Vec3 toTarget = to.getPos().subtract(simulationPosition);
        double distance = toTarget.length();

        //return zero if we are really close
        if (distance < 1e-9) return Vec3.ZERO;

        var movement = toTarget.normalize().scale(M_PER_MS / 100_000 * sim.getSimSpeed() * SimulationEngine.MS_PER_SIM_TICK);
//        System.out.println(movement.length());

        // Prevent overshooting
        if (distance <= movement.length()) return toTarget;

        return movement;
    }

    public boolean hasArrived() {
        return simulationPosition.distanceTo(to.getPos()) < 0.1;
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
            this.simulationPosition = from.getPos();

            var sim = SimulationEngine.getInstance(level().isClientSide);
            sim.registerNetworkFrame(this);

            initialized = true;
            state = State.TRAVELING;
            interferedTicks = 0;
            entityData.set(DATA_STATE, state.ordinal());
            entityData.set(DATA_INTERFERED_TICKS, interferedTicks);
        } catch (Exception e) {
            this.discard();
        }
    }


    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha) {
        State currentState = State.values()[entityData.get(DATA_STATE)];
        if (currentState == State.ARRIVED) return;

        var renderer = new RenderUtil(poseStack, bufferSource, alpha, packedLight);

        var mc = Minecraft.getInstance();

        poseStack.pushPose();

        poseStack.translate(0, 0.5, 0); //render above
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);

        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);

        if (currentState == State.INTERFERED) {
            renderer.renderBackgroundQuadWithColor(100, 20, 0xFF0000);
        }

        int textColor = currentState == State.INTERFERED ? 0xFFFF0000 : RenderUtil.getTextColorFromAlpha(alpha);
        renderer.drawString(RenderUtil.Align.CENTER, "UDP @ " + from.getIP().toString() + " -> " + to.getIP().toString() + " TTL: " + ttl, textColor, 0, -15);

        poseStack.popPose();

        packet.render(renderer);

        poseStack.popPose();
    }

    public void setInterfered() {
        state = State.INTERFERED;
        entityData.set(DATA_STATE, state.ordinal());
        interferedTicks = 0;
        entityData.set(DATA_INTERFERED_TICKS, 0);
    }
}
