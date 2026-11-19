package com.sunshinestar.moreSteam;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings, stored in config/moresteam-client.toml. */
public final class MoreSteamConfig {

	/** Create releases steam for this many ticks after a train stops. */
	public static final int VANILLA_STEAM_TICKS = 10;

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	// Apply to all of the extra steam; kept at the top level

	public static final ModConfigSpec.IntValue SOUND_RANGE;
	public static final ModConfigSpec.DoubleValue STEAM_AXLE_BASE;

	// Steam after a train stops

	public static final ModConfigSpec.BooleanValue STOP_STEAM;
	public static final ModConfigSpec.DoubleValue STEAM_DURATION_MULTIPLIER;
	public static final ModConfigSpec.DoubleValue STOP_BURST_LENGTH;
	public static final ModConfigSpec.DoubleValue STOP_PRESSURE_CURVE;

	// Steam while standing

	public static final ModConfigSpec.BooleanValue IDLE_STEAM;
	public static final ModConfigSpec.DoubleValue IDLE_DELAY_AFTER_STOP;
	public static final ModConfigSpec.DoubleValue IDLE_INTERVAL_MIN;
	public static final ModConfigSpec.DoubleValue IDLE_INTERVAL_MAX;
	public static final ModConfigSpec.DoubleValue IDLE_BURST_LENGTH;

	// Steam while pulling away

	public static final ModConfigSpec.BooleanValue PULL_AWAY_STEAM;
	public static final ModConfigSpec.DoubleValue PULL_AWAY_INTERVAL_MIN;
	public static final ModConfigSpec.DoubleValue PULL_AWAY_INTERVAL_MAX;
	public static final ModConfigSpec.DoubleValue PULL_AWAY_BURST_LENGTH;
	public static final ModConfigSpec.DoubleValue PULL_AWAY_CUTOFF;

	static {
		SOUND_RANGE = BUILDER
			.comment("How far away, in blocks, the cylinder bursts can be heard. They fade out evenly up to this",
				"distance. Minecraft's own sounds use 16.")
			.translation("moresteam.configuration.soundRange")
			.defineInRange("soundRange", 32, 16, 256);

		STEAM_AXLE_BASE = BUILDER
			.comment("How much more steam a bogey lets off for every axle beyond two: amount = base ^ (axles - 2).",
				"1.0 gives every bogey the same amount; 2.0 doubles it with every axle.",
				"WARNING: this grows very fast. At 2.0 a six-axle bogey already lets off 16 times as much",
				"steam as a two-axle one, and at 3.0 it is 81 times as much - thousands of particles per",
				"bogey, which can bury a train in white and drop the frame rate to a crawl. Lower it if",
				"long bogeys cause lag, and raise it only in small steps.")
			.translation("moresteam.configuration.steamAxleBase")
			.defineInRange("steamAxleBase", 2.0, 1.0, 3.0);

		BUILDER.comment("Steam let off when a train comes to a stop.")
			.translation("moresteam.configuration.section.stopSteam")
			.push("stopSteam");

		STOP_STEAM = BUILDER
			.comment("Whether the cylinders of a train with a firebox blow steam backwards, with a hiss, when it stops.",
				"When off, Create's own small downward puffs are also left at Create's own length.")
			.translation("moresteam.configuration.stopSteam")
			.define("enableStopSteam", true);

		STEAM_DURATION_MULTIPLIER = BUILDER
			.comment("How much longer Create's own small downward puffs last after a train stops, on every bogey.",
				"1.0 is Create's own duration (up to half a second); 3.0 is three times as long.")
			.translation("moresteam.configuration.steamDurationMultiplier")
			.defineInRange("createPuffMultiplier", 3.0, 1.0, 20.0);

		STOP_BURST_LENGTH = BUILDER
			.comment("Longest time, in seconds, the cylinders of a train with a firebox blow steam backwards after it",
				"stops. A train that travelled 100+ blocks and braked from full speed gets the whole length; shorter",
				"or slower trips get less, down to 1.5 s.")
			.translation("moresteam.configuration.stopBurstLength")
			.defineInRange("burstLength", 5.0, 0.5, 30.0);

		STOP_PRESSURE_CURVE = BUILDER
			.comment("How much less steam slow or short trips give when the train stops. The stop's pressure P (0 to 1)",
				"becomes (base ^ P - 1) / (base - 1). 1.0 is a straight line; at 4.0 half pressure gives a third.")
			.translation("moresteam.configuration.stopPressureCurve")
			.defineInRange("pressureCurve", 4.0, 1.0, 16.0);

		BUILDER.pop();
		BUILDER.comment("Bursts from the cylinders of a standing train.")
			.translation("moresteam.configuration.section.idleSteam")
			.push("idleSteam");

		IDLE_STEAM = BUILDER
			.comment("Whether the cylinders of a standing train with a firebox let off a burst of steam every now and then.")
			.translation("moresteam.configuration.idleSteam")
			.define("enableIdleSteam", true);

		IDLE_DELAY_AFTER_STOP = BUILDER
			.comment("How long, in seconds, a train stays quiet after its stop hiss has died away before idle steam",
				"starts.")
			.translation("moresteam.configuration.idleDelayAfterStop")
			.defineInRange("delayAfterStop", 20.0, 0.0, 120.0);

		IDLE_INTERVAL_MIN = BUILDER
			.comment("Shortest wait, in seconds, before a standing train's cylinder lets off a burst. Each cylinder",
				"picks a new wait between the minimum and the maximum every time.")
			.translation("moresteam.configuration.idleIntervalMin")
			.defineInRange("intervalMin", 10.0, 1.0, 120.0);

		IDLE_INTERVAL_MAX = BUILDER
			.comment("Longest wait, in seconds, before a standing train's cylinder lets off a burst.",
				"If it is set below the minimum, the minimum is used.")
			.translation("moresteam.configuration.idleIntervalMax")
			.defineInRange("intervalMax", 15.0, 1.0, 120.0);

		IDLE_BURST_LENGTH = BUILDER
			.comment("How long, in seconds, each burst of a standing train lasts.")
			.translation("moresteam.configuration.idleBurstLength")
			.defineInRange("burstLength", 5.0, 0.5, 30.0);

		BUILDER.pop();
		BUILDER.comment("Bursts from the cylinders while a train pulls away from a standstill.")
			.translation("moresteam.configuration.section.pullAwaySteam")
			.push("pullAwaySteam");

		PULL_AWAY_STEAM = BUILDER
			.comment("Whether the cylinders of a train with a firebox let off bursts as it pulls away from a standstill.")
			.translation("moresteam.configuration.pullAwaySteam")
			.define("enablePullAwaySteam", true);

		PULL_AWAY_INTERVAL_MIN = BUILDER
			.comment("Shortest wait, in seconds, between the bursts of a cylinder while the train pulls away.")
			.translation("moresteam.configuration.pullAwayIntervalMin")
			.defineInRange("intervalMin", 4.0, 0.5, 60.0);

		PULL_AWAY_INTERVAL_MAX = BUILDER
			.comment("Longest wait, in seconds, between the bursts of a cylinder while the train pulls away.",
				"If it is set below the minimum, the minimum is used.")
			.translation("moresteam.configuration.pullAwayIntervalMax")
			.defineInRange("intervalMax", 7.0, 0.5, 60.0);

		PULL_AWAY_BURST_LENGTH = BUILDER
			.comment("How long, in seconds, each burst lasts while the train pulls away.")
			.translation("moresteam.configuration.pullAwayBurstLength")
			.defineInRange("burstLength", 1.0, 0.1, 10.0);

		PULL_AWAY_CUTOFF = BUILDER
			.comment("Fraction of the train's top speed at which the pull-away bursts stop. They only start again",
				"after the train has come to a standstill.")
			.translation("moresteam.configuration.pullAwayCutoff")
			.defineInRange("cutoff", 0.2, 0.0, 1.0);

		BUILDER.pop();
	}

	public static final ModConfigSpec SPEC = BUILDER.build();

	private MoreSteamConfig() {}

	/** Length of the stop-steam phase in ticks, replacing Create's fixed 10. */
	public static int steamTicks() {
		return scaleSteamTicks(VANILLA_STEAM_TICKS);
	}

	/**
	 * Length of the stop burst in ticks for a stop of the given pressure (0..1): from 1.5 s, the
	 * length of Create's own stretched stop steam, up to the configured length at full pressure.
	 */
	public static int stopBurstTicks(float pressure) {
		double max = STOP_BURST_LENGTH.get();
		double min = Math.min(1.5, max);
		return Math.max(1, (int) Math.round(20 * (min + (max - min) * pressure)));
	}

	/** Bends a stop's pressure (0..1) down so that low pressures count for less. */
	public static float curvePressure(float pressure) {
		double base = STOP_PRESSURE_CURVE.get();
		if (base < 1.0001)
			return pressure;
		return (float) ((Math.pow(base, pressure) - 1) / (base - 1));
	}

	/** Quiet time in ticks between the stop hiss dying away and idle steam. */
	public static int idleDelayAfterStopTicks() {
		return (int) Math.round(20 * IDLE_DELAY_AFTER_STOP.get());
	}

	/** Spread in ticks between the shortest and longest idle wait. */
	public static int idleSpreadTicks() {
		return (int) Math.round(20 * Math.max(0, IDLE_INTERVAL_MAX.get() - IDLE_INTERVAL_MIN.get()));
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

	/** A fresh random wait in ticks before the next pull-away burst. */
	public static int pullAwayWaitTicks(net.minecraft.util.RandomSource random) {
		double min = PULL_AWAY_INTERVAL_MIN.get();
		double max = Math.max(min, PULL_AWAY_INTERVAL_MAX.get());
		return (int) Math.round(20 * (min + random.nextDouble() * (max - min)));
	}

	/** Length of a pull-away burst in ticks. */
	public static int pullAwayBurstTicks() {
		return Math.max(1, (int) Math.round(20 * PULL_AWAY_BURST_LENGTH.get()));
	}

	/** Stretches a number of steam ticks by the configured multiplier. */
	public static int scaleSteamTicks(int ticks) {
		if (!STOP_STEAM.get())
			return ticks;
		return (int) Math.round(ticks * STEAM_DURATION_MULTIPLIER.get());
	}
}
