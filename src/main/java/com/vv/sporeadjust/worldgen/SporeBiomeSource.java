package com.vv.sporeadjust.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

public class SporeBiomeSource extends BiomeSource {

    public static final MapCodec<SporeBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            RegistryCodecs.homogeneousList(Registries.BIOME)
                    .validate(biomes -> biomes.size() == 3 ? DataResult.success(biomes)
                            : DataResult.error(() -> "Spore biome source requires exactly 3 biomes: plains, forest, hills; got " + biomes.size()))
                    .fieldOf("biomes").forGetter(s -> s.biomes)
        ).apply(instance, SporeBiomeSource::new)
    );

    private final HolderSet<Biome> biomes;

    public SporeBiomeSource(HolderSet<Biome> biomes) {
        if (biomes.size() != 3) {
            throw new IllegalArgumentException("Spore biome source requires exactly 3 biomes: plains, forest, hills");
        }
        this.biomes = biomes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Climate.TargetPoint point = sampler.sample(x, y, z);
        float continentalness = Climate.unquantizeCoord(point.continentalness());
        float erosion = Climate.unquantizeCoord(point.erosion());
        float humidity = Climate.unquantizeCoord(point.humidity());

        if (continentalness > 0.5f && erosion < -0.3f) {
            return biomes.get(2);
        }
        if (humidity > 0.5f) {
            return biomes.get(1);
        }
        return biomes.get(0);
    }
}
