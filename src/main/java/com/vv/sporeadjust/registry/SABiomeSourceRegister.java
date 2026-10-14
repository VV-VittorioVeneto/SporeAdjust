package com.vv.sporeadjust.registry;

import com.mojang.serialization.MapCodec;
import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.worldgen.SporeBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SABiomeSourceRegister {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCE_CODECS =
            DeferredRegister.create(Registries.BIOME_SOURCE, SporeAdjust.MOD_ID);

    public static final Supplier<MapCodec<SporeBiomeSource>> SPORE_BIOME_SOURCE =
            BIOME_SOURCE_CODECS.register("spore_biome_source", () -> SporeBiomeSource.CODEC);

    public static void register(IEventBus eventBus) {
        BIOME_SOURCE_CODECS.register(eventBus);
    }
}