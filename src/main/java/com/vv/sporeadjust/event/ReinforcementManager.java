package com.vv.sporeadjust.event;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Sentities;
import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.registry.SAEffectRegister;
import com.vv.sporeadjust.registry.SAParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

import static com.vv.sporeadjust.registry.SAEffectRegister.*;

public class ReinforcementManager {
    private static final int FIND_ATTEMPTS = 10;
    private static final int REINFORCE_RADIUS = 8;
    private static final int REINFORCE_GAP = 160;
    private static final int REINFORCE_PLAYER_RANGE = 128;
    private static final int BASE_COOLDOWN = 2400;

    private int reinforceCooldown = 0;
    private int reinforceDuration = 0;
    private int reinforceTick = 0;
    private Vec3 reinforceCenter;
    private final List<Vec3> reinforcePoints = new ArrayList<>();
    private final ServerLevel reinforceLevel;
    private enum Status {
        IDLE,
        RUNNING,
        COOLDOWN,
    }
    private Status status = Status.IDLE;

    private static final Map<ResourceKey<Level>, ReinforcementManager> INSTANCES = new HashMap<>();

    private ReinforcementManager(ServerLevel level) {
        this.reinforceLevel = level;
    }

    public static ReinforcementManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level.dimension(),
            k -> new ReinforcementManager(level));
    }

    public static void unload(Level level) {
        INSTANCES.remove(level.dimension());
    }

    private List<ServerPlayer> getPlayersAroundCenter() {
        if (reinforceCenter == null) return new ArrayList<>();

        return reinforceLevel.getPlayers(
            p -> p.position().distanceToSqr(reinforceCenter) <= REINFORCE_PLAYER_RANGE * REINFORCE_PLAYER_RANGE
        );
    }

    private static int getMaxMobEffectLevelFromPlayers(List<ServerPlayer> players, Holder<MobEffect> holder) {
        int maxLevel = 0;
        int currentLevel;
        MobEffectInstance effect;
        for (ServerPlayer player : players) {
            effect = player.getEffect(holder);
            if (effect != null) {
                currentLevel = effect.getAmplifier() + 1;
                if (currentLevel > maxLevel) maxLevel = currentLevel;
            }
        }
        return maxLevel;
    }

    private int getReinforceDuration() {
        List<ServerPlayer> players = getPlayersAroundCenter();
        int maxLureLevel = getMaxMobEffectLevelFromPlayers(players, SAEffectRegister.LURE);
        return REINFORCE_GAP * (4 + maxLureLevel);
    }

    private int getReinforceCooldown() {
        List<ServerPlayer> players = getPlayersAroundCenter();
        int maxLureLevel = getMaxMobEffectLevelFromPlayers(players, SAEffectRegister.LURE);
        int maxCalmLevel = getMaxMobEffectLevelFromPlayers(players, SAEffectRegister.CALM);
        return (BASE_COOLDOWN - Math.min(3, maxLureLevel) * BASE_COOLDOWN / 6) * (1 + maxCalmLevel * 2);
    }

    private Vec3 findSpawnPos(Vec3 center) {
        for (int i = 0; i < FIND_ATTEMPTS; i++) {
            double dx = (reinforceLevel.getRandom().nextDouble() * 2 - 1) * REINFORCE_RADIUS;
            double dz = (reinforceLevel.getRandom().nextDouble() * 2 - 1) * REINFORCE_RADIUS;

            double x = center.x + dx;
            double z = center.z + dz;

            for (int y = (int) (center.y + 16); y > center.y - 16; y--) {
                BlockPos pos = new BlockPos((int) x, y, (int) z);
                BlockPos below = pos.below();

                if (reinforceLevel.getBlockState(pos).isAir()
                        && reinforceLevel.getBlockState(pos.above()).isAir()
                        && reinforceLevel.getBlockState(below).isFaceSturdy(reinforceLevel, pos, Direction.UP)) {
                    return new Vec3(x, y, z);
                }
            }
        }
        return null;
    }

    private Mob getRandomHyper() {
        int random = Math.round(reinforceLevel.random.nextFloat() * 8);
        return switch (random) {
            case 1 -> Sentities.WENDIGO.get().create(reinforceLevel);
            case 2 -> Sentities.INQUISITOR.get().create(reinforceLevel);
            case 3 -> Sentities.BROTKATZE.get().create(reinforceLevel);
            case 4 -> Sentities.OGRE.get().create(reinforceLevel);
            case 5 -> Sentities.HEVOKER.get().create(reinforceLevel);
            case 6 -> Sentities.HVINDICATOR.get().create(reinforceLevel);
            case 7 -> Sentities.GROBER.get().create(reinforceLevel);
            default -> Sentities.HOLLEN.get().create(reinforceLevel);
        };
    }

    private Mob getRandomCalamity() {
        int random = Math.round(reinforceLevel.random.nextFloat() * 8);
        return switch (random) {
            case 1 -> Sentities.SIEGER.get().create(reinforceLevel);
            case 2 -> Sentities.GAZENBREACHER.get().create(reinforceLevel);
            case 3 -> Sentities.HINDENBURG.get().create(reinforceLevel);
            case 4 -> Sentities.HOWITZER.get().create(reinforceLevel);
            case 5 -> Sentities.HOHLFRESSER.get().create(reinforceLevel);
            case 6 -> Sentities.STALH.get().create(reinforceLevel);
            case 7 -> Sentities.LEVIATHAN.get().create(reinforceLevel);
            default -> Sentities.VERFALL.get().create(reinforceLevel);
        };
    }

    private Mob getRandomEvolved() {
        int random = Math.round(reinforceLevel.random.nextFloat() * 25);
        return switch (random) {
            case 1 -> Sentities.CONDUCTOR.get().create(reinforceLevel);
            case 2 -> Sentities.GARGOYLE.get().create(reinforceLevel);
            case 3 -> Sentities.GORGON.get().create(reinforceLevel);
            case 4 -> Sentities.NAIAD.get().create(reinforceLevel);
            case 5 -> Sentities.CHEMIST.get().create(reinforceLevel);
            case 6 -> Sentities.MEPHETIC.get().create(reinforceLevel);
            case 7 -> Sentities.INEBRIATER.get().create(reinforceLevel);
            case 8 -> Sentities.PROTECTOR.get().create(reinforceLevel);
            case 9 -> Sentities.JAGD.get().create(reinforceLevel);
            case 10 -> Sentities.SCAVENGER.get().create(reinforceLevel);
            case 11 -> Sentities.BLOATER.get().create(reinforceLevel);
            case 12 -> Sentities.NUCLEA.get().create(reinforceLevel);
            case 13 -> Sentities.VOLATILE.get().create(reinforceLevel);
            case 14 -> Sentities.BRUTE.get().create(reinforceLevel);
            case 15 -> Sentities.STALKER.get().create(reinforceLevel);
            case 16 -> Sentities.HOWLER.get().create(reinforceLevel);
            case 17 -> Sentities.INF_EVOKER.get().create(reinforceLevel);
            case 18 -> Sentities.INF_VINDICATOR.get().create(reinforceLevel);
            case 19 -> Sentities.SPITTER.get().create(reinforceLevel);
            case 20 -> Sentities.SLASHER.get().create(reinforceLevel);
            case 21 -> Sentities.LEAPER.get().create(reinforceLevel);
            case 22 -> Sentities.BRAIOMIL.get().create(reinforceLevel);
            case 23 -> Sentities.GRIEFER.get().create(reinforceLevel);
            case 24 -> Sentities.BUSSER.get().create(reinforceLevel);
            default -> Sentities.KNIGHT.get().create(reinforceLevel);
        };
    }

    private Mob getRandomExperiment() {
        int random = Math.round(reinforceLevel.random.nextFloat() * 4);
        return switch (random) {
            case 1 -> Sentities.BIOBLOOB.get().create(reinforceLevel);
            case 2 -> Sentities.SAUGLING.get().create(reinforceLevel);
            case 3 -> Sentities.PLAGUED.get().create(reinforceLevel);
            default -> Sentities.LACERATOR.get().create(reinforceLevel);
        };
    }

    private Mob getRandomBaseInfected() {
        int random = Math.round(reinforceLevel.random.nextFloat() * 10);
        return switch (random) {
            case 1 -> Sentities.INF_HUMAN.get().create(reinforceLevel);
            case 2 -> Sentities.INF_PILLAGER.get().create(reinforceLevel);
            case 3 -> Sentities.INF_VILLAGER.get().create(reinforceLevel);
            case 4 -> Sentities.INF_WITCH.get().create(reinforceLevel);
            case 5 -> Sentities.INF_DROWNED.get().create(reinforceLevel);
            case 6 -> Sentities.INF_HUSK.get().create(reinforceLevel);
            case 7 -> Sentities.INF_PLAYER.get().create(reinforceLevel);
            case 8 -> Sentities.INF_WANDERER.get().create(reinforceLevel);
            case 9 -> Sentities.INF_HAZMAT.get().create(reinforceLevel);
            default -> Sentities.INF_DISEASED_VILLAGER.get().create(reinforceLevel);
        };
    }

    private Mob getReinforceMob(String mob) {
        return switch (mob) {
            case "INF_HUMAN" -> Sentities.INF_HUMAN.get().create(reinforceLevel);
            case "INF_PILLAGER" -> Sentities.INF_PILLAGER.get().create(reinforceLevel);
            case "INF_VILLAGER" -> Sentities.INF_VILLAGER.get().create(reinforceLevel);
            case "INF_WITCH" -> Sentities.INF_WITCH.get().create(reinforceLevel);
            case "INF_DROWNED" -> Sentities.INF_DROWNED.get().create(reinforceLevel);
            case "INF_HUSK" -> Sentities.INF_HUSK.get().create(reinforceLevel);
            case "INF_PLAYER" -> Sentities.INF_PLAYER.get().create(reinforceLevel);
            case "INF_WANDERER" -> Sentities.INF_WANDERER.get().create(reinforceLevel);
            case "INF_HAZMAT" -> Sentities.INF_HAZMAT.get().create(reinforceLevel);
            case "INF_DISEASED_VILLAGER" -> Sentities.INF_DISEASED_VILLAGER.get().create(reinforceLevel);

            case "BIOBLOOB" -> Sentities.BIOBLOOB.get().create(reinforceLevel);
            case "SAUGLING" -> Sentities.SAUGLING.get().create(reinforceLevel);
            case "PLAGUED" -> Sentities.PLAGUED.get().create(reinforceLevel);
            case "LACERATOR" -> Sentities.LACERATOR.get().create(reinforceLevel);

            case "CONDUCTOR" -> Sentities.CONDUCTOR.get().create(reinforceLevel);
            case "GARGOYLE" -> Sentities.GARGOYLE.get().create(reinforceLevel);
            case "GORGON" -> Sentities.GORGON.get().create(reinforceLevel);
            case "NAIAD" -> Sentities.NAIAD.get().create(reinforceLevel);
            case "CHEMIST" -> Sentities.CHEMIST.get().create(reinforceLevel);
            case "MEPHETIC" -> Sentities.MEPHETIC.get().create(reinforceLevel);
            case "INEBRIATER" -> Sentities.INEBRIATER.get().create(reinforceLevel);
            case "PROTECTOR" -> Sentities.PROTECTOR.get().create(reinforceLevel);
            case "JAGD" -> Sentities.JAGD.get().create(reinforceLevel);
            case "SCAVENGER" -> Sentities.SCAVENGER.get().create(reinforceLevel);
            case "BLOATER" -> Sentities.BLOATER.get().create(reinforceLevel);
            case "NUCLEA" -> Sentities.NUCLEA.get().create(reinforceLevel);
            case "VOLATILE" -> Sentities.VOLATILE.get().create(reinforceLevel);
            case "BRUTE" -> Sentities.BRUTE.get().create(reinforceLevel);
            case "STALKER" -> Sentities.STALKER.get().create(reinforceLevel);
            case "HOWLER" -> Sentities.HOWLER.get().create(reinforceLevel);
            case "INF_EVOKER" -> Sentities.INF_EVOKER.get().create(reinforceLevel);
            case "INF_VINDICATOR" -> Sentities.INF_VINDICATOR.get().create(reinforceLevel);
            case "SPITTER" -> Sentities.SPITTER.get().create(reinforceLevel);
            case "SLASHER" -> Sentities.SLASHER.get().create(reinforceLevel);
            case "LEAPER" -> Sentities.LEAPER.get().create(reinforceLevel);
            case "BRAIOMIL" -> Sentities.BRAIOMIL.get().create(reinforceLevel);
            case "GRIEFER" -> Sentities.GRIEFER.get().create(reinforceLevel);
            case "KNIGHT" -> Sentities.KNIGHT.get().create(reinforceLevel);
            case "BUSSER" -> Sentities.BUSSER.get().create(reinforceLevel);

            case "WENDIGO" -> Sentities.WENDIGO.get().create(reinforceLevel);
            case "INQUISITOR" -> Sentities.INQUISITOR.get().create(reinforceLevel);
            case "BROTKATZE" -> Sentities.BROTKATZE.get().create(reinforceLevel);
            case "OGRE" -> Sentities.OGRE.get().create(reinforceLevel);
            case "HEVOKER" -> Sentities.HEVOKER.get().create(reinforceLevel);
            case "HVINDICATOR" -> Sentities.HVINDICATOR.get().create(reinforceLevel);
            case "GROBER" -> Sentities.GROBER.get().create(reinforceLevel);
            case "HOLLEN" -> Sentities.HOLLEN.get().create(reinforceLevel);

            case "SIEGER" -> Sentities.SIEGER.get().create(reinforceLevel);
            case "GAZENBREACHER" -> Sentities.GAZENBREACHER.get().create(reinforceLevel);
            case "HINDENBURG" -> Sentities.HINDENBURG.get().create(reinforceLevel);
            case "HOWITZER" -> Sentities.HOWITZER.get().create(reinforceLevel);
            case "HOHLFRESSER" -> Sentities.HOHLFRESSER.get().create(reinforceLevel);
            case "STALH" -> Sentities.STALH.get().create(reinforceLevel);
            case "LEVIATHAN" -> Sentities.LEVIATHAN.get().create(reinforceLevel);
            case "VERFALL" -> Sentities.VERFALL.get().create(reinforceLevel);

            case "RANDOM_BASE" -> getRandomBaseInfected();
            case "RANDOM_EVOLVED" -> getRandomEvolved();
            case "RANDOM_EXPERIMENT" -> getRandomExperiment();
            case "RANDOM_HYPER" -> getRandomHyper();
            case "RANDOM_CALAMITY" -> getRandomCalamity();
            default -> null;
        };
    }

    private void applyEffect(Mob mob) {
        mob.addEffect(new MobEffectInstance(DAMAGE_LIMIT,MobEffectInstance.INFINITE_DURATION, 0));
        float random = reinforceLevel.getRandom().nextFloat();
        if (random < 0.01) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 3));
        } else if (random < 0.04) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 2));
        } else if (random < 0.06) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 1));
        } else if (random < 0.09) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 0));
        }
        random = reinforceLevel.getRandom().nextFloat();
        if (random < 0.01) {
            mob.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, 3));
        } else if (random < 0.03) {
            mob.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, 2));
        } else if (random < 0.06) {
            mob.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, 1));
        } else if (random < 0.09) {
            mob.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, 0));
        } else if (random < 0.15) {
            mob.addEffect(new MobEffectInstance(INSANITY, MobEffectInstance.INFINITE_DURATION, 9));
        }
        random = reinforceLevel.getRandom().nextFloat();
        if (random < 0.1) {
            mob.addEffect(new MobEffectInstance(DEDICATION, MobEffectInstance.INFINITE_DURATION, 0));
        }
        random = reinforceLevel.getRandom().nextFloat();
        if (random < 0.01) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 3));
        } else if (random < 0.03) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 2));
        } else if (random < 0.06) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 1));
        } else if (random < 0.09) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
        }
        random = reinforceLevel.getRandom().nextFloat();
        if (random < 0.09) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0));
        }
        double health = Objects.requireNonNull(mob.getAttributes().getInstance(Attributes.MAX_HEALTH)).getValue();
        health = health * (1.5 + random);
        health = Math.min(health, 999);
        Objects.requireNonNull(mob.getAttributes().getInstance(Attributes.MAX_HEALTH)).setBaseValue(health);
        mob.setHealth((float)health);
    }

    private boolean spawnOne(Vec3 pos, Mob mob) {
        if (mob == null || pos == null) return false;

        mob.moveTo(pos.x, pos.y, pos.z, reinforceLevel.getRandom().nextFloat() * 360f, 0);
        applyEffect(mob);
        reinforceLevel.addFreshEntity(mob);
        return true;
    }

    private static final double SMOKE_RADIUS = 2.0;
    private static final int SMOKE_COUNT = 3;
    private static final double SMOKE_UPWARD_SPEED = 10; // 上升速度，越大越快

    private static void spawnSmoke(ServerLevel level, Vec3 center) {
        RandomSource random = level.getRandom();

        for (int i = 0; i < SMOKE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0f;
            double r = SMOKE_RADIUS * Math.sqrt(random.nextDouble());
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            double y = center.y + random.nextDouble() * 2.0f;

            double vx = (random.nextDouble() - 0.5) * 0.05f;
            double vy = SMOKE_UPWARD_SPEED * (0.8 + random.nextDouble() * 0.4);
            double vz = (random.nextDouble() - 0.5) * 0.05f;

            level.sendParticles(
                    SAParticleRegister.REINFORCEMENT_SMOKE.get(),
                    x, y, z,
                    0,
                    vx, vy, vz,
                    10 * random.nextDouble()
            );
        }
    }

    private record ReinforceWaveInfo(List<String> mobs, int baseCount, int multiplier) {
    }

    private static final List<ReinforceWaveInfo> MAIN_WAVES = List.of(
        new ReinforceWaveInfo(
                List.of("RANDOM_EXPERIMENT"),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_EVOLVED", "RANDOM_EVOLVED", "RANDOM_EVOLVED", "RANDOM_BASE"),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_HYPER", "RANDOM_HYPER", "RANDOM_EVOLVED"),
                4, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_HYPER"),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_CALAMITY", "RANDOM_HYPER"),
                4, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_CALAMITY"),
                1, 2
        )
    );

    private static final List<ReinforceWaveInfo> SUB_WAVES = List.of(
        new ReinforceWaveInfo(
                List.of("RANDOM_EXPERIMENT", "RANDOM_EXPERIMENT", "RANDOM_EXPERIMENT", "RANDOM_BASE"),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_EVOLVED", "RANDOM_EVOLVED", "RANDOM_EVOLVED", "RANDOM_BASE"),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_HYPER", "RANDOM_EVOLVED"),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_HYPER", "RANDOM_HYPER", "RANDOM_EVOLVED"),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_HYPER"),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of("RANDOM_CALAMITY", "RANDOM_HYPER"),
                4, 2
        )
    );

    private boolean spawnWave(Vec3 center, int waveIndex, int playerCount, int posIndex) {
        int index, amount;
        boolean spawnedAny = false;
        List<String> mobs;

        List<ReinforceWaveInfo> wave_list = (posIndex == 1)? MAIN_WAVES : SUB_WAVES;
        waveIndex = Math.min(waveIndex, wave_list.size() - 1);
        ReinforceWaveInfo wave = wave_list.get(waveIndex);

        amount = playerCount * wave.multiplier + wave.baseCount;
        mobs = wave.mobs;
        for (int i = 0; i < amount; i++) {
            index = Math.min(i, mobs.size() - 1);
            spawnedAny |= spawnOne(findSpawnPos(center), getReinforceMob(mobs.get(index)));
            SporeAdjust.LOGGER.debug("spawn:{} {} {}", waveIndex, index, mobs.get(index));
        }
        return spawnedAny;
    }

    private boolean doReinforce(Vec3 center, int wave, int index) {
        if (status != Status.RUNNING) return false;
        int playerCount = getPlayersAroundCenter().size();
        return spawnWave(center, wave, playerCount, index);
    }

    private void generateReinforcePos(Vec3 center, float offsetX, float rangeX, float offsetZ, float rangeZ) {
        RandomSource random = reinforceLevel.getRandom();
        Vec3 testVec3;
        for (int i = 0; i < FIND_ATTEMPTS; i++) {
            testVec3 = center.add(offsetX + random.nextDouble() * rangeX, 0, offsetZ + random.nextDouble() * rangeZ);
            BlockPos pos = new BlockPos((int) Math.floor(testVec3.x), (int) Math.floor(testVec3.y), (int) Math.floor(testVec3.z));
            BlockPos below = pos.below();

            if (reinforceLevel.getBlockState(pos).isAir()
                    && reinforceLevel.getBlockState(pos.above()).isAir()
                    && reinforceLevel.getBlockState(below).isFaceSturdy(reinforceLevel, pos, Direction.UP)) {
                reinforcePoints.add(testVec3);
                return;
            }
        }
    }

    public void tryTrigger(Vec3 center) {
        if (status != Status.IDLE) return;
        reinforceCenter = center;
        generateReinforcePos(center, 8, 10, 8, 10);
        generateReinforcePos(center, -10, 12, -6, 8);
        generateReinforcePos(center, -6, 8, -10, 12);
        if (reinforcePoints.isEmpty()) {
            reinforceCenter = null;
            return;
        }
        reinforceDuration = getReinforceDuration();
        status = Status.RUNNING;
    }

    public static void touch(LivingDamageEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof Infected infected) {
            if (infected.level() instanceof ServerLevel serverLevel) {
                get(serverLevel).tryTrigger(infected.position());
            }
        }
    }

    public static void ticking(ServerTickEvent.Pre event) {
        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            get(serverLevel).tick();
        }
    }

    private void tick() {
        if (status == Status.IDLE) return;
        int i = 0;
        if (status == Status.RUNNING) {
            reinforceTick++;
            for (Vec3 reinforcePoint : reinforcePoints) {
                spawnSmoke(reinforceLevel, reinforcePoint);
            }
            if (reinforceTick % REINFORCE_GAP == REINFORCE_GAP / 2) {
                for (Vec3 reinforcePoint : reinforcePoints) {
                    i++;
                    if (!doReinforce(reinforcePoint, reinforceTick / REINFORCE_GAP, i)) {
                        reinforceDuration = 0;
                    }
                }
            }
            if (reinforceTick >= reinforceDuration) {
                status = Status.COOLDOWN;
                reinforceCooldown = getReinforceCooldown();
                reinforcePoints.clear();
                reinforceCenter = null;
                reinforceTick = 0;
            }
        }
        if (status == Status.COOLDOWN) {
            if (--reinforceCooldown == 0) status = Status.IDLE;
        }
    }

    public void queryReinforceInfo(Player player) {
        switch (status) {
            case RUNNING:
                int resetTick = (reinforceDuration - reinforceTick) / 20;
                player.sendSystemMessage(Component.literal("增援中...还有" + resetTick + "秒"));
                return;
            case COOLDOWN:
                player.sendSystemMessage(Component.literal("下一波增援冷却还有" + reinforceCooldown / 20 + "秒"));
                return;
            case IDLE:
                player.sendSystemMessage(Component.literal("下一波增援随时可能到来"));
                return;
            default:
        }
    }
}
