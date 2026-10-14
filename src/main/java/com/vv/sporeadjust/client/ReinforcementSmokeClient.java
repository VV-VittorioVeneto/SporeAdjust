package com.vv.sporeadjust.client;

import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.network.ReinforcementSmokePayload;
import com.vv.sporeadjust.registry.SAParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = SporeAdjust.MOD_ID, value = Dist.CLIENT)
public final class ReinforcementSmokeClient {
    private static final Map<ResourceLocation, Emitter> EMITTERS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Emitter {
        final List<Vec3> points;

        Emitter(ReinforcementSmokePayload payload) {
            points = payload.points();
        }
    }

    public static void receive(ReinforcementSmokePayload payload) {
        if (payload.points().isEmpty() || payload.remainingTicks() <= 0) {
            EMITTERS.remove(payload.dimension());
        } else {
            EMITTERS.put(payload.dimension(), new Emitter(payload));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || minecraft.player == null) return;
        ResourceLocation dimension = level.dimension().location();
        Emitter emitter = EMITTERS.get(dimension);
        if (emitter == null) return;
        for (Vec3 center : emitter.points) {
            // Match vanilla sendParticles' normal 32-block receiving radius.
            if (!minecraft.player.blockPosition().closerToCenterThan(center, 32.0)
                    || !level.hasChunkAt(BlockPos.containing(center))) continue;
            spawnSmoke(level, center);
        }
        // Only the server decides when the event ends; client ticks can outpace a lagging server.
    }

    private static void spawnSmoke(ClientLevel level, Vec3 center) {
        for (int i = 0; i < 3; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0f;
            double radius = 2.0 * Math.sqrt(RANDOM.nextDouble());
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + RANDOM.nextDouble() * 2.0f;
            double vx = (RANDOM.nextDouble() - 0.5) * 0.05f;
            double vy = 10 * (0.8 + RANDOM.nextDouble() * 0.4);
            double vz = (RANDOM.nextDouble() - 0.5) * 0.05f;
            // count=0 particle packets multiply the velocity components by speed.
            double speed = 10 * RANDOM.nextDouble();
            level.addParticle(SAParticleRegister.REINFORCEMENT_SMOKE.get(),
                    x, y, z, vx * speed, vy * speed, vz * speed);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) EMITTERS.remove(level.dimension().location());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        EMITTERS.clear();
    }
}
