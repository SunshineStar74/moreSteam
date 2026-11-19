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

	/** The cylinders releasing pressure as the train pulls away from a standstill. */
	public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTURE = register("bogey.departure");

	/** A cylinder puffing while the train speeds up. */
	public static final DeferredHolder<SoundEvent, SoundEvent> PULL_AWAY_BURST = register("bogey.pull_away_burst");

	private MoreSteamSounds() {}

	// The range that matters is set per sound on the client; see BurstSound#resolve.
	private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
		return SOUND_EVENTS.register(name,
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MoreSteam.MOD_ID, name)));
	}

	public static void register(IEventBus modEventBus) {
		SOUND_EVENTS.register(modEventBus);
	}
}
