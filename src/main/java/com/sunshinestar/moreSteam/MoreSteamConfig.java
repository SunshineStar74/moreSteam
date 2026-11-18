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
		.comment("Whether the cylinders of a standing train with a firebox let off a burst of steam every now and then.")
		.translation("moresteam.configuration.idleSteam")
		.define("idleSteam", true);

	public static final ModConfigSpec.DoubleValue IDLE_INTERVAL_MIN = BUILDER
		.comment("Shortest wait, in seconds, before a standing train's cylinder lets off a burst. Each cylinder",
			"picks a new wait between the minimum and the maximum every time.")
		.translation("moresteam.configuration.idleIntervalMin")
		.defineInRange("idleIntervalMin", 10.0, 1.0, 120.0);

	public static final ModConfigSpec.DoubleValue IDLE_INTERVAL_MAX = BUILDER
		.comment("Longest wait, in seconds, before a standing train's cylinder lets off a burst.",
			"If it is set below the minimum, the minimum is used.")
		.translation("moresteam.configuration.idleIntervalMax")
		.defineInRange("idleIntervalMax", 15.0, 1.0, 120.0);

	public static final ModConfigSpec.DoubleValue IDLE_BURST_LENGTH = BUILDER
		.comment("How long, in seconds, each burst of a standing train lasts.")
		.translation("moresteam.configuration.idleBurstLength")
		.defineInRange("idleBurstLength", 5.0, 0.5, 30.0);

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

	/** A fresh random wait in ticks before the next idle burst. */
	public static int idleWaitTicks(net.minecraft.util.RandomSource random) {
		double min = IDLE_INTERVAL_MIN.get();
		double max = Math.max(min, IDLE_INTERVAL_MAX.get());
		return (int) Math.round(20 * (min + random.nextDouble() * (max - min)));
	}

	/** Length of an idle burst in ticks. */
	public static int idleBurstTicks() {
		return Math.max(1, (int) Math.round(20 * IDLE_BURST_LENGTH.get()));
	}

	/** Stretches a number of steam ticks by the configured multiplier. */
	public static int scaleSteamTicks(int ticks) {
		return (int) Math.round(ticks * STEAM_DURATION_MULTIPLIER.get());
	}
}
