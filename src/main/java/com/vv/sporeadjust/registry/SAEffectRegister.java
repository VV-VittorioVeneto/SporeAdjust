package com.vv.sporeadjust.registry;

import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.effect.TagEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SAEffectRegister {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SporeAdjust.MOD_ID);

    public static final Holder<MobEffect> CALM = MOB_EFFECTS.register("calm", () -> new TagEffect(MobEffectCategory.NEUTRAL,0xFF00FF));
    public static final Holder<MobEffect> LURE = MOB_EFFECTS.register("lure", () -> new TagEffect(MobEffectCategory.NEUTRAL,0xFF00FF));

    public static final Holder<MobEffect> DAMAGE_LIMIT = MOB_EFFECTS.register("damage_limit",
            ()->new TagEffect(MobEffectCategory.BENEFICIAL,0xFF00FF));
    public static final Holder<MobEffect> DEDICATION = MOB_EFFECTS.register("dedication",
            ()->new TagEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));
    public static final Holder<MobEffect> INSANITY = MOB_EFFECTS.register("insanity",
            ()->new TagEffect(MobEffectCategory.BENEFICIAL,0xFF00FF));
    public static final Holder<MobEffect> TRUE_DEFENSE = MOB_EFFECTS.register("true_defense",
            ()->new TagEffect(MobEffectCategory.BENEFICIAL,0xFF00FF));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
