package com.vv.sporeadjust.event;

import com.vv.sporeadjust.SporeAdjust;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Objects;

import static com.vv.sporeadjust.registry.SAEffectRegister.*;
import static com.vv.sporeadjust.registry.SAItemRegister.*;

@EventBusSubscriber(modid = SporeAdjust.MOD_ID)
public class EventsHandler {
    public EventsHandler() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(TRUE_DEFENSE)) {
            event.getContainer().setNewDamage(0);
            return;
        }
        DamageHandler.DefenseBypass(event);
        if (event.getSource().getEntity() instanceof Player) {
            ReinforcementManager.touch(event);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ReinforcementManager.unload(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        ReinforcementManager.ticking(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob) {
            if (mob.hasEffect(DEDICATION)){
                final TargetingConditions allCondition = TargetingConditions.forCombat().ignoreLineOfSight().selector(e -> true);
                mob.level().getNearbyEntities(Mob.class, allCondition, mob, mob.getBoundingBox().inflate(8.0D)).forEach(entityNear -> {
                    entityNear.heal(mob.getMaxHealth());
                    int preLevel = 0;
                    if (entityNear.hasEffect(INSANITY)) {
                        preLevel = Objects.requireNonNull(entityNear.getEffect(INSANITY)).getAmplifier() + 2;
                        entityNear.removeEffect(INSANITY);
                    }
                    entityNear.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, preLevel));
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            return;
        }
        ItemStack handHeldItemStack = event.getItemStack();
        Player player = event.getEntity();
        if (handHeldItemStack.is(BAIT.get())) {
            player.removeEffect(CALM);
            player.addEffect(new MobEffectInstance(LURE, 20 * 60 * 10, 0));
            handHeldItemStack.setCount(handHeldItemStack.getCount() - 1);
        }
        if (handHeldItemStack.is(STRONG_BAIT.get())) {
            player.removeEffect(CALM);
            player.addEffect(new MobEffectInstance(LURE, 20 * 60 * 30, 1));
            handHeldItemStack.setCount(handHeldItemStack.getCount() - 1);
        }
        if (handHeldItemStack.is(SEDATIVE.get())) {
            player.removeEffect(LURE);
            player.addEffect(new MobEffectInstance(CALM, 20 * 60 * 10, 0));
            handHeldItemStack.setCount(handHeldItemStack.getCount() - 1);
        }
        if (handHeldItemStack.is(DETECTOR.get()) && player.level() instanceof ServerLevel serverLevel) {
            ReinforcementManager.get(serverLevel).queryReinforceInfo(player);
        }
    }
}
