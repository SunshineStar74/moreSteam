package com.sunshinestar.moreSteam;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings, stored in config/moresteam-client.toml. */
public final class MoreSteamConfig {

	/** Create releases steam for this many ticks after a train stops. */
	public static final int VANILLA_STEAM_TICKS = 10;

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.DoubleValue STEAM_DURATION_MULTIPLIER = BUILDER
		.comment("How much longer bogeys release steam after a train stops.",
			"1.0 is Create's own duration (up to half a second); 3.0 is three times as long.")
		.translation("moresteam.configuration.steamDurationMultiplier")
		.defineInRange("steamDurationMultiplier", 3.0, 1.0, 20.0);

	public static final ModConfigSpec.BooleanValue IDLE_STEAM = BUILDER
		.comment("Whether the bogeys of a standing train keep letting off small puffs of steam.")
		.translation("moresteam.configuration.idleSteam")
		.define("idleSteam", true);

	public static final ModConfigSpec.IntValue IDLE_STEAM_INTERVAL = BUILDER
		.comment("Average number of ticks between idle puffs, per bogey. Lower means more steam.",
			"20 ticks are one second.")
		.translation("moresteam.configuration.idleSteamInterval")
		.defineInRange("idleSteamInterval", 8, 1, 200);

	public static final ModConfigSpec.DoubleValue STEAM_AXLE_BASE = BUILDER
		.comment("How much more steam a bogey lets off for every axle beyond two: amount = base ^ (axles - 2).",
			"1.0 gives every bogey the same amount; 2.0 doubles it with every axle.",
			"WARNING: this grows very fast. At 2.0 a six-axle bogey already lets off 16 times as much",
			"steam as a two-axle one, and at 3.0 it is 81 times as much - thousands of particles per",
			"bogey, which can bury a train in white and drop the frame rate to a crawl. Lower it if",
			"long bogeys cause lag, and raise it only in small steps.")
		.translation("moresteam.configuration.steamAxleBase")
		.defineInRange("steamAxleBase", 2.0, 1.0, 3.0);

	public static final ModConfigSpec SPEC = BUILDER.build();

	private MoreSteamConfig() {}

	/** Length of the stop-steam phase in ticks, replacing Create's fixed 10. */
	public static int steamTicks() {
		return scaleSteamTicks(VANILLA_STEAM_TICKS);
	}

	/** Stretches a number of steam ticks by the configured multiplier. */
	public static int scaleSteamTicks(int ticks) {
		return (int) Math.round(ticks * STEAM_DURATION_MULTIPLIER.get());
	}
}
