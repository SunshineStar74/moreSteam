package com.sunshinestar.moreSteam;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MoreSteamBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MoreSteam.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreSteam.MOD_ID);

    public static final DeferredBlock<Block> FIREBOX = BLOCKS.registerSimpleBlock("firebox",
        BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()
            .sound(SoundType.METAL).noOcclusion()); // noOcclusion: needed if the model isn't a full cube
    public static final DeferredItem<BlockItem> FIREBOX_ITEM = ITEMS.registerSimpleBlockItem(FIREBOX);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(MoreSteamBlocks::addToTabs);
    }

    private static void addToTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(FIREBOX_ITEM);
    }
}
