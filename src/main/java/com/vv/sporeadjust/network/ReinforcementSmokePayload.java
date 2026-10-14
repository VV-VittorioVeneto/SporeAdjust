package com.vv.sporeadjust.network;

import com.vv.sporeadjust.SporeAdjust;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A complete snapshot; an empty point list stops the emitters in this dimension. */
public record ReinforcementSmokePayload(ResourceLocation dimension, List<Vec3> points, int remainingTicks)
        implements CustomPacketPayload {
    public static final Type<ReinforcementSmokePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SporeAdjust.MOD_ID, "reinforcement_smoke"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReinforcementSmokePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension());
                buffer.writeVarInt(payload.remainingTicks());
                buffer.writeVarInt(payload.points().size());
                for (Vec3 point : payload.points()) {
                    buffer.writeDouble(point.x);
                    buffer.writeDouble(point.y);
                    buffer.writeDouble(point.z);
                }
            }, buffer -> {
                ResourceLocation dimension = buffer.readResourceLocation();
                int remaining = buffer.readVarInt();
                int count = buffer.readVarInt();
                if (count < 0 || count > 3) throw new IllegalArgumentException("Invalid reinforcement point count");
                List<Vec3> points = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    points.add(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
                }
                return new ReinforcementSmokePayload(dimension, points, Math.max(0, remaining));
            });

    public ReinforcementSmokePayload {
        points = List.copyOf(points);
        if (points.size() > 3) throw new IllegalArgumentException("Too many reinforcement points");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
