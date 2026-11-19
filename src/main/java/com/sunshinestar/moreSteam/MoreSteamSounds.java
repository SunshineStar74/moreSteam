package com.sunshinestar.moreSteam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events. The actual files are listed in assets/moresteam/sounds.json and live in
 * assets/moresteam/sounds/steam/.
 */
public final class MoreSteamSounds {

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
		DeferredRegister.create(Registries.SOUND_EVENT, MoreSteam.MOD_ID);

	/** A cylinder of a standing train letting off a burst. */
	public static final DeferredHolder<SoundEvent, SoundEvent> IDLE_BURST = register("bogey.idle_burst");

	/** The cylinders letting off steam as the train comes to a stop. */
	public static final DeferredHolder<SoundEvent, SoundEvent> STOP_BURST = register("bogey.stop_burst");

	private MoreSteamSounds() {}

	private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
		return SOUND_EVENTS.register(name,
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MoreSteam.MOD_ID, name)));
	}

	public static void register(IEventBus modEventBus) {
		SOUND_EVENTS.register(modEventBus);
	}
}
