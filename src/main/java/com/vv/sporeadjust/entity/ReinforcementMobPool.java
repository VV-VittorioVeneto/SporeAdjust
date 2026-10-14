package com.vv.sporeadjust.entity;

import com.Harbinger.Spore.core.Sentities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.function.Supplier;

/** Reinforcement tiers. Registry holders are resolved only when a mob is created. */
public enum ReinforcementMobPool {
    BASE_INFECTED(
            () -> Sentities.INF_HUMAN.get(),
            () -> Sentities.INF_PILLAGER.get(),
            () -> Sentities.INF_VILLAGER.get(),
            () -> Sentities.INF_WITCH.get(),
            () -> Sentities.INF_DROWNED.get(),
            () -> Sentities.INF_HUSK.get(),
            () -> Sentities.INF_PLAYER.get(),
            () -> Sentities.INF_WANDERER.get(),
            () -> Sentities.INF_HAZMAT.get(),
            () -> Sentities.INF_DISEASED_VILLAGER.get()
    ),
    EXPERIMENT(
            () -> Sentities.BIOBLOOB.get(),
            () -> Sentities.SAUGLING.get(),
            () -> Sentities.PLAGUED.get(),
            () -> Sentities.LACERATOR.get()
    ),
    EVOLVED(
            () -> Sentities.CONDUCTOR.get(),
            () -> Sentities.GARGOYLE.get(),
            () -> Sentities.GORGON.get(),
            () -> Sentities.NAIAD.get(),
            () -> Sentities.CHEMIST.get(),
            () -> Sentities.MEPHETIC.get(),
            () -> Sentities.INEBRIATER.get(),
            () -> Sentities.PROTECTOR.get(),
            () -> Sentities.JAGD.get(),
            () -> Sentities.SCAVENGER.get(),
            () -> Sentities.BLOATER.get(),
            () -> Sentities.NUCLEA.get(),
            () -> Sentities.VOLATILE.get(),
            () -> Sentities.BRUTE.get(),
            () -> Sentities.STALKER.get(),
            () -> Sentities.HOWLER.get(),
            () -> Sentities.INF_EVOKER.get(),
            () -> Sentities.INF_VINDICATOR.get(),
            () -> Sentities.SPITTER.get(),
            () -> Sentities.SLASHER.get(),
            () -> Sentities.LEAPER.get(),
            () -> Sentities.BRAIOMIL.get(),
            () -> Sentities.GRIEFER.get(),
            () -> Sentities.BUSSER.get(),
            () -> Sentities.KNIGHT.get()
    ),
    HYPER(
            () -> Sentities.WENDIGO.get(),
            () -> Sentities.INQUISITOR.get(),
            () -> Sentities.BROTKATZE.get(),
            () -> Sentities.OGRE.get(),
            () -> Sentities.HEVOKER.get(),
            () -> Sentities.HVINDICATOR.get(),
            () -> Sentities.GROBER.get(),
            () -> Sentities.HOLLEN.get()
    ),
    CALAMITY(
            () -> Sentities.SIEGER.get(),
            () -> Sentities.GAZENBREACHER.get(),
            () -> Sentities.HINDENBURG.get(),
            () -> Sentities.HOWITZER.get(),
            () -> Sentities.HOHLFRESSER.get(),
            () -> Sentities.STALH.get(),
            () -> Sentities.LEVIATHAN.get(),
            () -> Sentities.VERFALL.get()
    );

    private final List<Supplier<? extends EntityType<? extends Mob>>> entityTypes;

    @SafeVarargs
    ReinforcementMobPool(Supplier<? extends EntityType<? extends Mob>>... entityTypes) {
        this.entityTypes = List.of(entityTypes);
    }

    public Mob createRandom(ServerLevel level) {
        return entityTypes.get(level.getRandom().nextInt(entityTypes.size())).get().create(level);
    }
}
