package com.vv.sporeadjust.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Dependency-free checks for persisted state; no live world or Minecraft client is required. */
public final class ReinforcementStateChecks {
    private static final Method LOAD;

    static {
        try {
            LOAD = ReinforcementManager.class.getDeclaredMethod("load", ServerLevel.class, CompoundTag.class);
            LOAD.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static ReinforcementManager load(CompoundTag tag) throws Exception {
        return (ReinforcementManager) LOAD.invoke(null, null, tag);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static CompoundTag position(double x, double y, double z) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        return tag;
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        return load(load(tag).save(new CompoundTag(), null)).save(new CompoundTag(), null);
    }

    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    public static void main(String[] args) throws Exception {
        CompoundTag cooldown = new CompoundTag();
        cooldown.putString("Status", "COOLDOWN");
        cooldown.putInt("Cooldown", 1234);
        CompoundTag restored = roundTrip(cooldown);
        check(restored.getInt("Cooldown") == 1234, "Cooldown must survive a restart");
        check(restored.getString("Status").equals("COOLDOWN"), "Cooldown status must survive");
        cooldown.putInt("Cooldown", 0);
        check(roundTrip(cooldown).getString("Status").equals("IDLE"), "Expired cooldown must be idle");

        CompoundTag running = new CompoundTag();
        running.putString("Status", "RUNNING");
        running.putInt("Cooldown", 0);
        running.putInt("Duration", 960);
        running.putInt("Tick", 241);
        running.put("Center", position(-12.5, 64, -8.5));
        ListTag points = new ListTag();
        CompoundTag point = position(-20.5, 64, -16.5);
        point.putInt("Slot", 2);
        point.putInt("Failures", 2);
        point.putBoolean("Active", true);
        points.add(point);
        running.put("Points", points);
        restored = roundTrip(running);
        check(restored.equals(running), "Running progress, negative positions and point failures must round-trip");
        point.putBoolean("Active", false);
        check(roundTrip(running).getString("Status").equals("COOLDOWN"), "All inactive points must end the event");
        check(load(new CompoundTag()).save(new CompoundTag(), null).getString("Status").equals("IDLE"),
                "Missing data must default to idle");

        Class<?> pointClass = Class.forName(ReinforcementManager.class.getName() + "$ReinforcePoint");
        Constructor<?> constructor = pointClass.getDeclaredConstructor(int.class, Vec3.class);
        constructor.setAccessible(true);
        Object failed = constructor.newInstance(0, new Vec3(0, 64, 0));
        Object healthy = constructor.newInstance(1, new Vec3(10, 64, 10));
        Method result = pointClass.getDeclaredMethod("onWaveResult", boolean.class, Vec3.class);
        result.setAccessible(true);
        Vec3 replacement = new Vec3(-5, 65, -5);
        result.invoke(failed, false, replacement);
        check(get(failed, "position").equals(replacement), "A failed point must relocate");
        check((boolean) get(failed, "active"), "One failed wave must not disable the point");
        result.invoke(failed, true, null);
        check((int) get(failed, "failures") == 0, "A successful wave must reset consecutive failures");
        for (int i = 0; i < 3; i++) result.invoke(failed, false, null);
        check(!(boolean) get(failed, "active"), "Three consecutive failures must disable that point");
        check((boolean) get(healthy, "active") && (int) get(healthy, "failures") == 0,
                "A failed point must not affect another point");
        System.out.println("Reinforcement persistence and point recovery checks passed.");
    }
}
