package com.vv.sporeadjust.event;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.vv.sporeadjust.entity.ReinforcementMobPool;
import com.vv.sporeadjust.SporeAdjust;
import com.vv.sporeadjust.registry.SAEffectRegister;
import com.vv.sporeadjust.network.ReinforcementSmokePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.vv.sporeadjust.registry.SAEffectRegister.*;

public class ReinforcementManager extends SavedData {
    private static final int FIND_ATTEMPTS = 10;
    private static final int REINFORCE_RADIUS = 8;
    private static final int REINFORCE_GAP = 160;
    private static final int REINFORCE_PLAYER_RANGE = 128;
    private static final int BASE_COOLDOWN = 2400;
    private static final int MAX_POINT_FAILURES = 3;
    private static final float[][] POINT_REGIONS = {
            {8, 10, 8, 10}, {-10, 12, -6, 8}, {-6, 8, -10, 12}
    };

    private int reinforceCooldown = 0;
    private int reinforceDuration = 0;
    private int reinforceTick = 0;
    private Vec3 reinforceCenter;
    private final List<ReinforcePoint> reinforcePoints = new ArrayList<>();
    private final ServerLevel reinforceLevel;
    private enum Status {
        IDLE,
        RUNNING,
        COOLDOWN,
    }
    private Status status = Status.IDLE;

    private static final class ReinforcePoint {
        final int slot;
        Vec3 position;
        int failures;
        boolean active = true;

        ReinforcePoint(int slot, Vec3 position) {
            this.slot = slot;
            this.position = position;
        }

        void onWaveResult(boolean spawned, Vec3 replacement) {
            if (spawned) {
                failures = 0;
            } else {
                failures++;
                if (replacement != null) position = replacement;
                if (failures >= MAX_POINT_FAILURES) active = false;
            }
        }
    }

    private ReinforcementManager(ServerLevel level) {
        this.reinforceLevel = level;
    }

    public static ReinforcementManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                () -> new ReinforcementManager(level),
                (tag, registries) -> load(level, tag)), "spore_adjust_reinforcements");
    }

    private static ReinforcementManager load(ServerLevel level, CompoundTag tag) {
        ReinforcementManager manager = new ReinforcementManager(level);
        try {
            manager.status = Status.valueOf(tag.getString("Status"));
        } catch (IllegalArgumentException ignored) {
            manager.status = Status.IDLE;
        }
        manager.reinforceCooldown = Math.max(0, tag.getInt("Cooldown"));
        manager.reinforceDuration = Math.max(0, tag.getInt("Duration"));
        manager.reinforceTick = Math.max(0, tag.getInt("Tick"));
        if (tag.contains("Center", Tag.TAG_COMPOUND)) {
            manager.reinforceCenter = readPosition(tag.getCompound("Center"));
        }
        ListTag points = tag.getList("Points", Tag.TAG_COMPOUND);
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < points.size(); i++) {
            CompoundTag pointTag = points.getCompound(i);
            int slot = pointTag.getInt("Slot");
            Vec3 position = readPosition(pointTag);
            if (slot < 0 || slot >= POINT_REGIONS.length || position == null || !slots.add(slot)) continue;
            ReinforcePoint point = new ReinforcePoint(slot, position);
            point.failures = Math.min(MAX_POINT_FAILURES, Math.max(0, pointTag.getInt("Failures")));
            point.active = pointTag.getBoolean("Active") && point.failures < MAX_POINT_FAILURES;
            manager.reinforcePoints.add(point);
        }
        if (manager.status == Status.COOLDOWN && manager.reinforceCooldown == 0) manager.status = Status.IDLE;
        if (manager.status == Status.RUNNING && (manager.reinforceCenter == null
                || manager.reinforceTick >= manager.reinforceDuration
                || manager.reinforcePoints.stream().noneMatch(point -> point.active))) {
            manager.status = Status.COOLDOWN;
            manager.reinforceCooldown = BASE_COOLDOWN;
        }
        if (manager.status != Status.RUNNING) {
            manager.reinforceCenter = null;
            manager.reinforcePoints.clear();
            manager.reinforceTick = 0;
        }
        manager.setDirty();
        return manager;
    }

    private static Vec3 readPosition(CompoundTag tag) {
        Vec3 position = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                ? position : null;
    }

    private static CompoundTag writePosition(Vec3 position) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        return tag;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Status", status.name());
        tag.putInt("Cooldown", reinforceCooldown);
        tag.putInt("Duration", reinforceDuration);
        tag.putInt("Tick", reinforceTick);
        if (reinforceCenter != null) tag.put("Center", writePosition(reinforceCenter));
        ListTag points = new ListTag();
        for (ReinforcePoint point : reinforcePoints) {
            CompoundTag pointTag = writePosition(point.position);
            pointTag.putInt("Slot", point.slot);
            pointTag.putInt("Failures", point.failures);
            pointTag.putBoolean("Active", point.active);
            points.add(pointTag);
        }
        tag.put("Points", points);
        return tag;
    }

    private ReinforcementSmokePayload smokeSnapshot() {
        List<Vec3> points = status == Status.RUNNING
                ? reinforcePoints.stream().filter(point -> point.active).map(point -> point.position).toList()
                : List.of();
        return new ReinforcementSmokePayload(reinforceLevel.dimension().location(), points,
                status == Status.RUNNING ? Math.max(0, reinforceDuration - reinforceTick) : 0);
    }

    private void syncSmoke() {
        PacketDistributor.sendToPlayersInDimension(reinforceLevel, smokeSnapshot());
    }

    public void syncSmoke(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, smokeSnapshot());
    }

    private List<ServerPlayer> getPlayersAroundCenter() {
        // TODO: 大改增援系统时排除旁观者，并明确附近无有效玩家时的暂停或结束策略。
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

    private Vec3 findSpawnPos(Vec3 center, Mob mob) {
        // TODO: 大改增援系统时将出生点搜索与实体生成分摊到多个 tick，并设置每 tick 工作预算。
        for (int i = 0; i < FIND_ATTEMPTS; i++) {
            double dx = (reinforceLevel.getRandom().nextDouble() * 2 - 1) * REINFORCE_RADIUS;
            double dz = (reinforceLevel.getRandom().nextDouble() * 2 - 1) * REINFORCE_RADIUS;

            double x = center.x + dx;
            double z = center.z + dz;

            int top = Math.min(reinforceLevel.getMaxBuildHeight() - 1, (int) Math.floor(center.y + 16));
            int bottom = Math.max(reinforceLevel.getMinBuildHeight() + 1, (int) Math.floor(center.y - 16));
            for (int y = top; y >= bottom; y--) {
                BlockPos pos = BlockPos.containing(x, y, z);
                BlockPos below = pos.below();
                if (!reinforceLevel.hasChunkAt(pos)) continue;
                if (reinforceLevel.getBlockState(below).isFaceSturdy(reinforceLevel, below, Direction.UP)) {
                    mob.setPos(x, y, z);
                    if (!isSafeSpawn(mob)) continue;
                    return new Vec3(x, y, z);
                }
            }
        }
        return null;
    }

    private boolean isSafeSpawn(Mob mob) {
        AABB box = mob.getBoundingBox();
        // Include a one-block margin because collision shapes may protrude from neighbouring blocks.
        if (box.minY < reinforceLevel.getMinBuildHeight() || box.maxY > reinforceLevel.getMaxBuildHeight()
                || !reinforceLevel.getWorldBorder().isWithinBounds(box)
                || !reinforceLevel.hasChunksAt(BlockPos.containing(box.minX - 1, box.minY, box.minZ - 1),
                        BlockPos.containing(box.maxX + 1, box.maxY, box.maxZ + 1))) return false;
        return reinforceLevel.noCollision(mob, box) && reinforceLevel.isUnobstructed(mob)
                && !reinforceLevel.containsAnyLiquid(box);
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
        EventHooks.finalizeMobSpawn(mob, reinforceLevel,
                reinforceLevel.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
        // Initialization may change the variant and therefore the entity's dimensions.
        mob.refreshDimensions();
        if (!isSafeSpawn(mob)) return false;
        applyEffect(mob);
        return reinforceLevel.addFreshEntity(mob);
    }

    private record ReinforceWaveInfo(List<ReinforcementMobPool> mobs, int baseCount, int multiplier) {
    }

    private static final List<ReinforceWaveInfo> MAIN_WAVES = List.of(
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.EXPERIMENT),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.EVOLVED, ReinforcementMobPool.EVOLVED, ReinforcementMobPool.EVOLVED, ReinforcementMobPool.BASE_INFECTED),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.HYPER, ReinforcementMobPool.HYPER, ReinforcementMobPool.EVOLVED),
                4, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.HYPER),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.CALAMITY, ReinforcementMobPool.HYPER),
                4, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.CALAMITY),
                1, 2
        )
    );

    private static final List<ReinforceWaveInfo> SUB_WAVES = List.of(
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.EXPERIMENT, ReinforcementMobPool.EXPERIMENT, ReinforcementMobPool.EXPERIMENT, ReinforcementMobPool.BASE_INFECTED),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.EVOLVED, ReinforcementMobPool.EVOLVED, ReinforcementMobPool.EVOLVED, ReinforcementMobPool.BASE_INFECTED),
                6, 2
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.HYPER, ReinforcementMobPool.EVOLVED),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.HYPER, ReinforcementMobPool.HYPER, ReinforcementMobPool.EVOLVED),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.HYPER),
                3, 1
        ),
        new ReinforceWaveInfo(
                List.of(ReinforcementMobPool.CALAMITY, ReinforcementMobPool.HYPER),
                4, 2
        )
    );

    private boolean spawnWave(Vec3 center, int waveIndex, int playerCount, int posIndex) {
        int index, amount;
        boolean spawnedAny = false;
        List<ReinforcementMobPool> mobs;

        List<ReinforceWaveInfo> wave_list = (posIndex == 1)? MAIN_WAVES : SUB_WAVES;
        waveIndex = Math.min(waveIndex, wave_list.size() - 1);
        ReinforceWaveInfo wave = wave_list.get(waveIndex);

        amount = playerCount * wave.multiplier + wave.baseCount;
        // TODO: 大改增援系统时设置人数倍率上限及增援实体总量上限。
        mobs = wave.mobs;
        for (int i = 0; i < amount; i++) {
            // TODO: 大改增援系统时明确列表是固定编排还是抽取权重；当前末项填满剩余数量。
            index = Math.min(i, mobs.size() - 1);
            Mob mob = mobs.get(index).createRandom(reinforceLevel);
            if (mob != null) spawnedAny |= spawnOne(findSpawnPos(center, mob), mob);
            SporeAdjust.LOGGER.debug("spawn:{} {} {}", waveIndex, index, mobs.get(index));
        }
        return spawnedAny;
    }

    private Vec3 generateReinforcePos(Vec3 center, int slot) {
        RandomSource random = reinforceLevel.getRandom();
        float[] region = POINT_REGIONS[slot];
        Vec3 testVec3;
        for (int i = 0; i < FIND_ATTEMPTS; i++) {
            testVec3 = center.add(region[0] + random.nextDouble() * region[1], 0,
                    region[2] + random.nextDouble() * region[3]);
            BlockPos pos = BlockPos.containing(testVec3);
            BlockPos below = pos.below();
            if (!reinforceLevel.hasChunkAt(pos) || reinforceLevel.isOutsideBuildHeight(below)
                    || reinforceLevel.isOutsideBuildHeight(pos.above())
                    || !reinforceLevel.getWorldBorder().isWithinBounds(pos)) continue;
            if (reinforceLevel.getBlockState(pos).isAir()
                    && reinforceLevel.getBlockState(pos.above()).isAir()
                    && reinforceLevel.getBlockState(below).isFaceSturdy(reinforceLevel, below, Direction.UP)) {
                return testVec3;
            }
        }
        return null;
    }

    public void tryTrigger(Vec3 center) {
        if (status != Status.IDLE) return;
        reinforceCenter = center;
        reinforcePoints.clear();
        for (int slot = 0; slot < POINT_REGIONS.length; slot++) {
            Vec3 position = generateReinforcePos(center, slot);
            if (position != null) reinforcePoints.add(new ReinforcePoint(slot, position));
        }
        if (reinforcePoints.isEmpty()) {
            reinforceCenter = null;
            return;
        }
        reinforceDuration = getReinforceDuration();
        reinforceTick = 0;
        status = Status.RUNNING;
        setDirty();
        syncSmoke();
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
        if (status == Status.RUNNING) {
            reinforceTick++;
            if (reinforceTick % REINFORCE_GAP == REINFORCE_GAP / 2) {
                int playerCount = getPlayersAroundCenter().size();
                boolean pointsChanged = false;
                for (ReinforcePoint point : reinforcePoints) {
                    if (!point.active) continue;
                    if (spawnWave(point.position, reinforceTick / REINFORCE_GAP, playerCount, point.slot + 1)) {
                        point.onWaveResult(true, null);
                    } else {
                        Vec3 replacement = generateReinforcePos(reinforceCenter, point.slot);
                        point.onWaveResult(false, replacement);
                        pointsChanged = true;
                    }
                }
                if (pointsChanged) syncSmoke();
            }
            if (reinforceTick >= reinforceDuration || reinforcePoints.stream().noneMatch(point -> point.active)) {
                status = Status.COOLDOWN;
                reinforceCooldown = getReinforceCooldown();
                reinforcePoints.clear();
                reinforceCenter = null;
                reinforceTick = 0;
                syncSmoke();
            }
        } else if (status == Status.COOLDOWN) {
            if (--reinforceCooldown <= 0) {
                reinforceCooldown = 0;
                status = Status.IDLE;
            }
        }
        // Marks memory dirty; actual disk writes are handled by normal world saves.
        setDirty();
    }

    public void queryReinforceInfo(Player player) {
        switch (status) {
            case RUNNING:
                int resetTick = (reinforceDuration - reinforceTick) / 20;
                player.sendSystemMessage(Component.translatable("message.spore_adjust.reinforcement.running", resetTick));
                return;
            case COOLDOWN:
                player.sendSystemMessage(Component.translatable("message.spore_adjust.reinforcement.cooldown", reinforceCooldown / 20));
                return;
            case IDLE:
                player.sendSystemMessage(Component.translatable("message.spore_adjust.reinforcement.idle"));
                return;
            default:
        }
    }
}
