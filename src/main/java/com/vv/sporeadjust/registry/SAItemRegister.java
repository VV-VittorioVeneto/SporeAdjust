package com.vv.sporeadjust.registry;

import com.vv.sporeadjust.SporeAdjust;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SAItemRegister {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SporeAdjust.MOD_ID);

    public static final DeferredHolder<Item, Item> BAIT =
        ITEMS.register("bait", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STRONG_BAIT =
        ITEMS.register("strong_bait", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SEDATIVE =
        ITEMS.register("sedative", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DETECTOR =
        ITEMS.register("detector", () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
