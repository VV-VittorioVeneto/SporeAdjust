package com.vv.sporeadjust;

import com.vv.sporeadjust.registry.SABiomeSourceRegister;
import com.vv.sporeadjust.registry.SAEffectRegister;
import com.vv.sporeadjust.registry.SAItemRegister;
import com.vv.sporeadjust.registry.SAParticleRegister;
import com.vv.sporeadjust.network.SANetwork;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

@Mod(SporeAdjust.MOD_ID)
public class SporeAdjust {
    public static final String MOD_ID = "spore_adjust";
    public static final Logger LOGGER = LogUtils.getLogger();
    public SporeAdjust(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SANetwork::register);
        SAParticleRegister.register(modEventBus);
        SAEffectRegister.register(modEventBus);
        SAItemRegister.register(modEventBus);
        SABiomeSourceRegister.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

}
