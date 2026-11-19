package com.sunshinestar.moreSteam;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.sunshinestar.moreSteam.mixin.CarriageBogeyAccessor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;

/**
 * What the steam needs to know about a bogey: whether it lets off steam at all, how wide its track
 * is and how many axles it has. Add-on bogeys (Steam 'n' Rails, Blocks &amp; Bogies) are recognised
 * by the ids of their styles and track types, so neither mod is needed to compile or run.
 */
public final class BogeyTraits {

	private static final String RAILWAYS = "railways";

	/** Wheel arrangements such as 0_10_0 or 6_0_6: the numbers count wheels, two per axle. */
	private static final Pattern WHEEL_ARRANGEMENT = Pattern.compile("(\\d+)_(\\d+)_(\\d+)");

	private static final String[] AXLE_WORDS = { "single", "double", "triple", "quadruple", "quintuple", "sextuple" };

	private BogeyTraits() {}

	/** Steam 'n' Rails' invisible bogeys never show any steam, not even Create's own. */
	public static boolean isInvisible(BogeyStyle style) {
		ResourceLocation id = style.id;
		return RAILWAYS.equals(id.getNamespace()) && id.getPath().startsWith("invisible");
	}

	/** Whether this bogey gets no extra steam: invisible ones, the handcar, and styles with their own smoke. */
	public static boolean isSilent(BogeyStyle style) {
		if (style.smokeParticle != ParticleTypes.POOF || isInvisible(style))
			return true;
		return RAILWAYS.equals(style.id.getNamespace()) && style.id.getPath().startsWith("handcar");
	}

	/**
	 * Sideways distance from the middle of the track to where the steam comes out. Create uses 1.15
	 * for its standard gauge; Steam 'n' Rails' narrower and wider tracks move the wheels in or out.
	 */
	public static double halfWidth(CarriageBogey bogey) {
		return switch (gauge(bogey)) {
			case "narrow_gauge" -> 0.65;
			case "wide_gauge" -> 1.65;
			case "monorail" -> 0.6;
			default -> 1.15;
		};
	}

	/** How much steam a bogey lets off compared to a standard-gauge, two-axle one. */
	public static float steamScale(CarriageBogey bogey) {
		float gauge = switch (gauge(bogey)) {
			case "narrow_gauge", "monorail" -> 0.75f;
			case "wide_gauge" -> 1.3f;
			default -> 1f;
		};
		// With the default base of 2: one axle 0.5, two 1, three 2, six 16.
		float axles = (float) Math.pow(MoreSteamConfig.STEAM_AXLE_BASE.get(), axles(bogey) - 2);
		// Comically Large counts as six axles, but gets half of that so it does not drown in steam.
		if (isComicallyLarge(bogey))
			axles *= 0.5f;
		return gauge * axles;
	}

	/** Number of axles, read from the name of the style; styles that do not say are taken as ordinary. */
	public static int axles(CarriageBogey bogey) {
		String path = bogey.getStyle().id.getPath();
		// One giant axle, but it gets as much steam as the biggest bogeys.
		if (isComicallyLarge(bogey))
			return 6;

		Matcher matcher = WHEEL_ARRANGEMENT.matcher(path);
		if (matcher.find()) {
			int wheels = Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2))
				+ Integer.parseInt(matcher.group(3));
			return Math.max(1, Math.min(6, wheels / 2));
		}

		for (int i = AXLE_WORDS.length - 1; i >= 0; i--)
			if (path.contains(AXLE_WORDS[i]))
				return i + 1;

		// Create's own large bogey is a single pair of big wheels, its small one two pairs.
		return bogey.getSize().wheelRadius() > 0.6f ? 1 : 2;
	}

	private static boolean isComicallyLarge(CarriageBogey bogey) {
		return RAILWAYS.equals(bogey.getStyle().id.getNamespace())
			&& "wide_comically_large".equals(bogey.getStyle().id.getPath());
	}

	private static String gauge(CarriageBogey bogey) {
		TrackMaterial.TrackType trackType = ((CarriageBogeyAccessor) bogey).moresteam$getType().getTrackType(bogey.getStyle());
		if (trackType == null || !RAILWAYS.equals(trackType.id.getNamespace()))
			return "standard";
		return trackType.id.getPath();
	}
}
