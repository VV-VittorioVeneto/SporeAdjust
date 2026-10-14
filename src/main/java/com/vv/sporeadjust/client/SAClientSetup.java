package com.vv.sporeadjust.client;

import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.network.SANetwork;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.vv.sporeadjust.particle.ReinforcementParticle;
import com.vv.sporeadjust.registry.SAParticleRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = SporeAdjust.MOD_ID, value = Dist.CLIENT)
public class SAClientSetup {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SANetwork.setSmokeReceiver(ReinforcementSmokeClient::receive));
    }

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SAParticleRegister.REINFORCEMENT_SMOKE.get(), ReinforcementParticle.Provider::new);
    }
}
