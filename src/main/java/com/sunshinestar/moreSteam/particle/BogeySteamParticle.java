package com.sunshinestar.moreSteam.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A large, slow-fading version of the normal poof used only for bogeys. */
public final class BogeySteamParticle extends TextureSheetParticle {

	private final SpriteSet sprites;
	private final float startAlpha;

	private BogeySteamParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
		double zSpeed, SpriteSet sprites, boolean idle) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		this.sprites = sprites;
		this.xd = xSpeed * .35;
		this.yd = ySpeed * .35 + .015;
		this.zd = zSpeed * .35;
		this.gravity = -0.0005F;
		this.friction = .94F;
		if (idle) {
			this.lifetime = 30 + this.random.nextInt(21);
			this.quadSize = .6F + this.random.nextFloat() * .3F;
			this.startAlpha = .5F;
		} else {
			this.lifetime = 70 + this.random.nextInt(31);
			this.quadSize = 1.5F + this.random.nextFloat() * .5F;
			this.startAlpha = .8F;
		}
		this.alpha = this.startAlpha;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
		this.alpha = this.startAlpha * (1.0F - (float) this.age / this.lifetime);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	/** Provider for the stop cloud. */
	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed) {
			return new BogeySteamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, false);
		}
	}

	/** Provider for the smaller, shorter-lived idle wisp. */
	public static final class IdleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public IdleProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed) {
			return new BogeySteamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, true);
		}
	}
}
