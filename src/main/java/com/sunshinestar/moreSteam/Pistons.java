package com.sunshinestar.moreSteam;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simibubi.create.content.trains.entity.CarriageBogey;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Where the steam cylinders of a bogey are. Positions are offsets from the bogey's own origin, in
 * the bogey's model: x sideways (the right-hand cylinder; the left one mirrors it), y up to the
 * underside of the cylinder body, z along the track.
 *
 * <p>The numbers were measured from the bogey models of Create, Steam 'n' Rails and Blocks &amp;
 * Bogies. Scotch yokes have a cylinder at both ends; Walschaerts and gearless bogeys have one,
 * at the end that the model puts at -z.
 */
public final class Pistons {

	/**
	 * One cylinder: x is how far out the right one sits, y the underside, z the position along the
	 * bogey. Small cylinders let off a thinner burst without the big clouds.
	 */
	public record Piston(double x, double y, double z, boolean small) {
		/** Where the steam comes out on one side: just under the cylinder. */
		public Vec3 offset(int side) {
			return new Vec3(side * x, y - MODEL_TO_PARTICLE - 0.1, z);
		}
	}

	/**
	 * Bogey models are drawn this much higher than the point Create's bogey particles are measured
	 * from. Every bogey renderer of the three mods draws its frame without shifting it, so the same
	 * value holds for all of them; it was matched by eye on Create's large bogey.
	 */
	private static final double MODEL_TO_PARTICLE = 0.5625;

	private static final Pattern WHEEL_ARRANGEMENT = Pattern.compile("0_(\\d+)_0");
	private static final String[] AXLE_WORDS = { "single", "double", "triple", "quadruple", "quintuple", "sextuple" };

	/** Blocks &amp; Bogies scotch yokes on the small size (its "large" models), by number of axles; 1 has no model and borrows the 2-axle one of the big size. */
	private static final double[] BB_SCOTCH_SMALL_Z = { 0, 1.44, 1.94, 2.75, 3.69, 4.44, 5.41 };
	/** Steam 'n' Rails' Create-styled bogeys, by number of axles. */
	private static final double[] CREATE_STYLED_Z = { 0, 0, 1.94, 2.75, 3.69, 4.44, 5.44 };

	private Pistons() {}

	/** The cylinders of this bogey, or an empty list if it has none. */
	public static List<Piston> of(CarriageBogey bogey) {
		String namespace = bogey.getStyle().id.getNamespace();
		String path = bogey.getStyle().id.getPath();
		boolean large = "large".equals(bogey.getSize().id().getPath());

		return switch (namespace) {
			case "create" -> large ? scotch(1.31, 0.69, 1.06, true) : List.of();
			case "railways" -> railways(path, large);
			case "create_bb" -> blocksAndBogies(bogey, path, large);
			default -> List.of();
		};
	}

	private static List<Piston> railways(String path, boolean large) {
		if (path.startsWith("large_create_style")) {
			Matcher matcher = WHEEL_ARRANGEMENT.matcher(path);
			int axles = matcher.find() ? Integer.parseInt(matcher.group(1)) / 2 : 2;
			return scotch(1.31, 0.69, CREATE_STYLED_Z[Math.max(2, Math.min(6, axles))], true);
		}
		return switch (path) {
			case "narrow_double_scotch" -> scotch(0.81, 0.25, 1.63, true);
			// The large size of these two is the Scotch Yoke bogey.
			case "narrow_default" -> large ? scotch(0.81, 0.25, 0.88, true) : List.of();
			case "wide_default" -> large ? scotch(1.81, 0.69, 1.13) : List.of();
			case "wide_comically_large" -> scotch(1.94, 0.56, 2.13);
			default -> List.of();
		};
	}

	private static List<Piston> blocksAndBogies(CarriageBogey bogey, String path, boolean large) {
		int axles = 0;
		for (int i = 0; i < AXLE_WORDS.length; i++)
			if (path.startsWith(AXLE_WORDS[i] + "_"))
				axles = i + 1;
		if (axles == 0)
			return List.of();

		// Its small size uses the models it calls "large", its large size the "extra large" ones.
		if (path.endsWith("scotch_yoke")) {
			if (large)
				return scotch(1.38, 0.81, 1.44 + 1.125 * (axles - 1));
			return axles == 1 ? scotch(1.38, 0.81, 1.44, true) : scotch(1.31, 0.69, BB_SCOTCH_SMALL_Z[axles], true);
		}

		boolean extended = path.contains("extended") || path.contains("extra_long");
		boolean isShort = path.endsWith("short");
		boolean isLong = path.endsWith("long");
		if (!isShort && !isLong)
			return List.of(); // pistonless, rodless, trailing, offset and the plain bogies

		double z = -(Math.max(axles, 2) + 1 + (large ? 1 : 0));
		if (extended && axles == 2 && isLong)
			z -= 1;
		if (isShort)
			z += 0.87;
		if (!isForwards(bogey))
			z = -z;
		return large ? List.of(new Piston(1.56, 0.94, z, false)) : List.of(new Piston(1.56, 0.56, z, false));
	}

	private static List<Piston> scotch(double x, double y, double z) {
		return scotch(x, y, z, false);
	}

	private static List<Piston> scotch(double x, double y, double z, boolean small) {
		return List.of(new Piston(x, y, -z, small), new Piston(x, y, z, small));
	}

	/**
	 * Blocks &amp; Bogies lets a bogie be turned around; it then draws the model rotated by 180 degrees.
	 * This repeats its own check from BlocksBogiesBogieRenderer.
	 */
	private static boolean isForwards(CarriageBogey bogey) {
		CompoundTag data = bogey.bogeyData;
		if (data == null)
			return true;
		boolean flipped = data.contains("IsForwards") && data.getBoolean("IsForwards");
		Direction assembly = Direction.NORTH;
		if (data.contains("AssemblyDirection")) {
			Direction read = Direction.byName(data.getString("AssemblyDirection").toLowerCase(java.util.Locale.ROOT));
			if (read != null)
				assembly = read;
		}
		boolean positive = assembly == Direction.NORTH || assembly == Direction.WEST || assembly == Direction.UP;
		return positive != flipped;
	}
}
