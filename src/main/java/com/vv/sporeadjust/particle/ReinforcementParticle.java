package com.vv.sporeadjust.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ReinforcementParticle extends TextureSheetParticle {
    protected ReinforcementParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        RandomSource random = level.getRandom();
        float brightness = 0.6f + random.nextFloat() * 0.2f;

        this.rCol = (0.9f + random.nextFloat() * 0.1f) * brightness;
        this.gCol = (0.4f + random.nextFloat() * 0.2f) * brightness;
        this.bCol = (0.0f + random.nextFloat() * 0.1f) * brightness;

        this.quadSize = 0.4f + random.nextFloat() * 0.4f;
        this.lifetime = (int) (40 + random.nextFloat() * 10);

        this.yd += 0.12f + random.nextFloat() * 0.06f;

        this.xd *= 0.05f;
        this.zd *= 0.05f;

        this.friction = 0.88f + random.nextFloat() * 0.12f;
        this.gravity = 0.0f;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = (this.age + partialTick) / this.lifetime;
        return this.quadSize * (1.0f - t * 0.5f);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            ReinforcementParticle particle = new ReinforcementParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
