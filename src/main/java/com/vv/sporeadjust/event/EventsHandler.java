package com.vv.sporeadjust.event;

import com.vv.sporeadjust.SporeAdjust;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        DamageHandler.DefenseBypass(event);
        if (event.getSource().getEntity() instanceof Player) {
            ReinforcementManager.touch(event);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncSmoke(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncSmoke(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncSmoke(event.getEntity());
    }

    private static void syncSmoke(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ReinforcementManager.get(serverPlayer.serverLevel()).syncSmoke(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        ReinforcementManager.ticking(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel serverLevel && entity instanceof Mob mob
                && (mob instanceof Infected || mob instanceof Calamity)) {
            if (mob.hasEffect(DEDICATION)){
                serverLevel.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(8.0D),
                        nearby -> nearby != mob && nearby.isAlive()
                                && (nearby instanceof Infected || nearby instanceof Calamity)).forEach(entityNear -> {
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
        boolean bait = handHeldItemStack.is(BAIT.get());
        boolean strongBait = handHeldItemStack.is(STRONG_BAIT.get());
        boolean sedative = handHeldItemStack.is(SEDATIVE.get());
        boolean detector = handHeldItemStack.is(DETECTOR.get());
        if (!bait && !strongBait && !sedative && !detector) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (bait) {
            player.removeEffect(CALM);
            player.addEffect(new MobEffectInstance(LURE, 20 * 60 * 10, 0));
            handHeldItemStack.shrink(1);
        }
        else if (strongBait) {
            player.removeEffect(CALM);
            player.addEffect(new MobEffectInstance(LURE, 20 * 60 * 30, 1));
            handHeldItemStack.shrink(1);
        }
        else if (sedative) {
            player.removeEffect(LURE);
            player.addEffect(new MobEffectInstance(CALM, 20 * 60 * 10, 0));
            handHeldItemStack.shrink(1);
        }
        else if (detector) {
            ReinforcementManager.get(serverLevel).queryReinforceInfo(player);
        }
    }
}
