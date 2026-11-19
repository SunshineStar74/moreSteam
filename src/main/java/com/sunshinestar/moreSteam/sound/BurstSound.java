package com.sunshinestar.moreSteam.sound;

import com.sunshinestar.moreSteam.MoreSteamConfig;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The sound of one cylinder's burst. It rides along with its carriage, can be heard as far as the
 * configured sound range, and can drop to a quieter tail after a while.
 */
public final class BurstSound extends AbstractTickableSoundInstance {

	/** Volume left once the loud part is over, relative to the full volume. */
	private static final float TAIL_VOLUME = 0.5f;
	/** How long the fade down to the tail volume takes. */
	private static final int FADE_TICKS = 10;
	/** How long the final fade-out after the tail takes. */
	private static final int OUT_TICKS = 20;

	private final Entity carriage;
	/** Where the sound sits relative to the carriage's position. */
	private final Vec3 offset;
	private final float fullVolume;
	/** Ticks at full volume before fading to the tail, or -1 to stay at full volume. */
	private final int loudTicks;
	/** Ticks the tail lasts before the sound fades out completely, or -1 to play the file to its end. */
	private final int tailTicks;
	private int age;

	public BurstSound(SoundEvent sound, Entity carriage, Vec3 pos, float volume, float pitch, int loudTicks,
		RandomSource random) {
		this(sound, carriage, pos, volume, pitch, loudTicks, -1, random);
	}

	public BurstSound(SoundEvent sound, Entity carriage, Vec3 pos, float volume, float pitch, int loudTicks,
		int tailTicks, RandomSource random) {
		super(sound, SoundSource.NEUTRAL, random);
		this.carriage = carriage;
		this.offset = pos.subtract(carriage.position());
		this.x = pos.x;
		this.y = pos.y;
		this.z = pos.z;
		this.volume = volume;
		this.pitch = pitch;
		this.fullVolume = volume;
		this.loudTicks = loudTicks;
		this.tailTicks = tailTicks;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
	}

	/**
	 * The client fades a sound out over the attenuation distance of the picked file, which comes from
	 * sounds.json and is 16 blocks unless set there. Swaps in a copy with the configured range.
	 */
	@Override
	public WeighedSoundEvents resolve(SoundManager manager) {
		WeighedSoundEvents events = super.resolve(manager);
		Sound picked = this.sound;
		if (picked != null && picked != SoundManager.EMPTY_SOUND && picked != SoundManager.INTENTIONALLY_EMPTY_SOUND)
			this.sound = new Sound(picked.getLocation(), picked.getVolume(), picked.getPitch(), picked.getWeight(),
				picked.getType(), picked.shouldStream(), picked.shouldPreload(), MoreSteamConfig.SOUND_RANGE.get());
		return events;
	}

	@Override
	public void tick() {
		if (this.carriage.isRemoved()) {
			stop();
			return;
		}
		Vec3 pos = this.carriage.position().add(this.offset);
		this.x = pos.x;
		this.y = pos.y;
		this.z = pos.z;

		this.age++;
		if (this.loudTicks >= 0) {
			float fade = Mth.clamp((this.age - this.loudTicks) / (float) FADE_TICKS, 0f, 1f);
			this.volume = this.fullVolume * Mth.lerp(fade, 1f, TAIL_VOLUME);
			if (this.tailTicks >= 0) {
				// After the tail, fade out completely over a second and stop.
				int tailEnd = this.loudTicks + FADE_TICKS + this.tailTicks;
				float out = Mth.clamp((this.age - tailEnd) / (float) OUT_TICKS, 0f, 1f);
				this.volume *= 1 - out;
				if (out >= 1)
					stop();
			}
		}
	}
}
