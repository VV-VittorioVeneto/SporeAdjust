package com.vv.sporeadjust.event;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Seffects;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Objects;

import static com.vv.sporeadjust.registry.SAEffectRegister.*;
import static com.vv.sporeadjust.registry.SAEffectRegister.INSANITY;

public class DamageHandler {
    public static void DefenseBypass(LivingDamageEvent.Pre event) {
        Entity attacker = event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        DamageSource damageSource = event.getSource();
        float currentDamage = event.getNewDamage();
        float modifiedDamage = currentDamage;

        if (target.hasEffect(DAMAGE_LIMIT)) {
            float limit = 0.15F * target.getMaxHealth();
            if (modifiedDamage > limit) {
                target.addEffect(new MobEffectInstance(TRUE_DEFENSE, 10, 0));
            }
            modifiedDamage = Math.min(modifiedDamage, limit);
        }

        if (attacker instanceof LivingEntity src) {
            if (src.hasEffect(INSANITY)) {
                int insanity_level = Objects.requireNonNull(src.getEffect(INSANITY)).getAmplifier();
                modifiedDamage *= (2F + insanity_level * 2F);
            }
        }

        if (damageSource.is(ISSDamageTypes.ICE_MAGIC) && target.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            modifiedDamage = currentDamage * 4.0F;
            target.addEffect(new MobEffectInstance(Seffects.FROSTBITE, 160, 4));
        }

        if (target instanceof Infected infected) {
           if (damageSource.is(ISSDamageTypes.LIGHTNING_MAGIC) && infected.getLinked()) {
               infected.setLinked(false);
           }
        }

        event.getContainer().setNewDamage(modifiedDamage);
    }
}
