package com.sunshinestar.moreSteam.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One tick's worth of a steam burst: a fast jet of Create's own small poofs with a slower, big
 * cloud billowing along behind it. Called every tick for as long as the burst lasts. Client only.
 */
public final class SteamBurst {

	private SteamBurst() {}

	/**
	 * @param pos     where the steam comes out
	 * @param back    unit vector pointing to the back of the train
	 * @param out     unit vector pointing away from the bogey, sideways
	 * @param carry   motion of the train, added on top so the steam does not lag behind it
	 * @param poofs   number of small poofs in the jet, before scaling
	 * @param speed   speed of the jet in blocks per tick, before scaling
	 * @param cloud   the big cloud particle to add, or null for none
	 * @param scale   amount of steam for this bogey, 1 for a standard-gauge two-axle one
	 */
	public static void emit(Level level, RandomSource random, Vec3 pos, Vec3 back, Vec3 out, Vec3 carry,
		int poofs, double speed, ParticleOptions cloud, float scale) {
		// Bigger bogeys get a much denser but only somewhat longer jet. A fractional poof is spawned
		// that fraction of the time.
		speed *= Math.cbrt(scale);
		float wanted = poofs * scale;
		int count = (int) wanted + (random.nextFloat() < wanted - (int) wanted ? 1 : 0);

		for (int n = 0; n < count; n++) {
			// Spread the poofs along the jet so that it reads as a continuous stream.
			double s = speed * (0.6 + random.nextDouble() * 0.6);
			Vec3 m = back.scale(s)
				.add(out.scale(0.03 + random.nextDouble() * 0.05))
				.add(spread(random, 0.03))
				.add(carry);
			level.addParticle(ParticleTypes.POOF, pos.x, pos.y, pos.z, m.x, m.y, m.z);
		}

		if (cloud != null) {
			// The clouds keep their size on every bogey; bigger bogeys get more of them instead.
			int clouds = (int) scale + (random.nextFloat() < scale - (int) scale ? 1 : 0);
			for (int n = 0; n < clouds; n++) {
				// The cloud particle slows its own start speed down, so it trails the jet and puffs up.
				Vec3 m = back.scale(speed * (1.1 + random.nextDouble() * 0.4))
					.add(out.scale(0.08))
					.add(spread(random, 0.05))
					.add(carry);
				level.addParticle(cloud, pos.x, pos.y, pos.z, m.x, m.y, m.z);
			}
		}
	}

	private static Vec3 spread(RandomSource random, double amount) {
		return new Vec3((random.nextDouble() * 2 - 1) * amount, (random.nextDouble() * 2 - 1) * amount,
			(random.nextDouble() * 2 - 1) * amount);
	}
}
