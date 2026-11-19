package com.sunshinestar.moreSteam.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The stop burst of one cylinder. It plays at full volume while the stop steam is coming out,
 * then fades down to a fraction of that for the rest of the file.
 */
public final class StopBurstSound extends AbstractTickableSoundInstance {

	/** Volume left once the steam has stopped, relative to the full volume. */
	private static final float TAIL_VOLUME = 0.5f;
	/** How long the fade down to the tail volume takes. */
	private static final int FADE_TICKS = 10;

	private final float fullVolume;
	private final int steamTicks;
	private int age;

	/**
	 * @param steamTicks how many ticks the stop steam has left; the sound stays at full volume
	 *                   for that long.
	 */
	public StopBurstSound(SoundEvent sound, double x, double y, double z, float volume, float pitch,
		int steamTicks, RandomSource random) {
		super(sound, SoundSource.NEUTRAL, random);
		this.x = x;
		this.y = y;
		this.z = z;
		this.volume = volume;
		this.pitch = pitch;
		this.fullVolume = volume;
		this.steamTicks = steamTicks;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
	}

	@Override
	public void tick() {
		this.age++;
		float fade = Mth.clamp((this.age - this.steamTicks) / (float) FADE_TICKS, 0f, 1f);
		this.volume = this.fullVolume * Mth.lerp(fade, 1f, TAIL_VOLUME);
	}
}
