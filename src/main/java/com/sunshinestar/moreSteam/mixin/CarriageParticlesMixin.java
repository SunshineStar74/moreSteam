package com.sunshinestar.moreSteam.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageParticles;
import com.sunshinestar.moreSteam.BogeyTraits;
import com.sunshinestar.moreSteam.MoreSteamBlocks;
import com.sunshinestar.moreSteam.MoreSteamConfig;
import com.sunshinestar.moreSteam.MoreSteamSounds;
import com.sunshinestar.moreSteam.Pistons;
import com.sunshinestar.moreSteam.particle.MoreSteamParticles;
import com.sunshinestar.moreSteam.particle.SteamBurst;
import com.sunshinestar.moreSteam.sound.BurstSound;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds more steam to the bogeys of Create trains. Everything here is written against
 * CarriageParticles#tick of Create 6.0.10. Create's own particles are left as they are; the
 * extra steam is spawned on top of them.
 *
 * <p>How Create times the steam: when a carriage stops, {@code depressurise} is set to
 * {@code 20 * train.accumulatedSteamRelease / 10} (0 to 20) and then counts down by one per tick.
 * Steam is only released while it is between 1 and 10; the ticks above 10 are a silent delay.
 */
@Mixin(CarriageParticles.class)
public abstract class CarriageParticlesMixin {

	@Shadow CarriageContraptionEntity entity;
	@Shadow boolean arrived;
	@Shadow int depressurise;
	@Shadow double prevMotion;

	/** Keeps Create's own steam off Steam 'n' Rails' invisible bogeys. */
	@WrapWithCondition(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")
	)
	private boolean moresteam$noSteamOnInvisibleBogeys(Level level, ParticleOptions particle, double x, double y, double z,
		double xSpeed, double ySpeed, double zSpeed, @Local CarriageBogey bogey) {
		BogeyStyle style = bogey.getStyle();
		return particle != style.smokeParticle || !BogeyTraits.isInvisible(style);
	}

	/** Widens the "depressurise > 10" check, i.e. the number of ticks during which steam is released. */
	@ModifyConstant(method = "tick", constant = @Constant(intValue = MoreSteamConfig.VANILLA_STEAM_TICKS))
	private int moresteam$widenSteamWindow(int vanillaSteamTicks) {
		return MoreSteamConfig.steamTicks();
	}

	/**
	 * Runs right after Create starts the countdown on arrival and stretches its steam part to fit
	 * the wider window. The silent delay before the steam keeps its original length.
	 */
	@Inject(
		method = "tick",
		at = @At(
			value = "FIELD",
			target = "Lcom/simibubi/create/content/trains/entity/CarriageParticles;depressurise:I",
			opcode = Opcodes.PUTFIELD,
			ordinal = 0,
			shift = At.Shift.AFTER
		)
	)
	private void moresteam$lengthenStopSteam(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
		// Create sets depressurise to 2 * accumulatedSteamRelease, and that grows by a tenth of a block
		// per block travelled since the last stop, up to 10. So this is 0..1 for 0..100 blocks.
		float distance = Mth.clamp(this.depressurise / 20f, 0f, 1f);
		float maxSpeed = this.entity.getCarriage().train.maxSpeed();
		float braking = maxSpeed > 0 ? Mth.clamp((float) (this.moresteam$brakeSpeed / maxSpeed), 0f, 1f) : 1f;
		this.moresteam$pressure = distance * (0.5f + 0.5f * braking);

		int steam = Math.min(this.depressurise, MoreSteamConfig.VANILLA_STEAM_TICKS);
		int delay = this.depressurise - steam;
		this.depressurise = MoreSteamConfig.scaleSteamTicks(steam) + delay;
	}

	/**
	 * Per cylinder (two bogeys, up to two cylinders each): ticks left to wait before the next idle or
	 * pull-away burst while positive, ticks left of the running burst while negative. 0 means not
	 * started.
	 */
	@Unique private final int[] moresteam$burstTimers = new int[4];
	/** Whether the carriage carries a firebox; looked up once, since a carriage cannot change. */
	@Unique private Boolean moresteam$hasFirebox;
	/** Whether the stop sound has been played for the current stop. */
	@Unique private boolean moresteam$stopSounded;
	/** Ticks left of the stop burst. */
	@Unique private int moresteam$stopTicks;
	/** The stop sounds still playing; idle steam waits for them. */
	@Unique private final List<SoundInstance> moresteam$stopSounds = new ArrayList<>();
	/** Ticks left before idle steam may start after a stop. */
	@Unique private int moresteam$idleDelay;
	/** Whether the wait after a stop has just run out, so idle steam should start soon. */
	@Unique private boolean moresteam$idleDelayEnded;
	/** Full length in ticks of the current stop burst. */
	@Unique private int moresteam$stopTotal;
	/**
	 * Pressure of the current stop, 0..1: how far the train came since the last stop, weighed by how
	 * fast it was going when it started its final braking. Scales the stop burst.
	 */
	@Unique private float moresteam$pressure;
	/** Speed, in blocks per tick, at which the train started its final braking. */
	@Unique private double moresteam$brakeSpeed;
	/** A lower speed the train may be settling at, and for how many ticks it has held it. */
	@Unique private double moresteam$holdSpeed;
	@Unique private int moresteam$holdTicks;
	/** Whether the stop burst has been started for the current stop. */
	@Unique private boolean moresteam$stopStarted;
	/** Whether the carriage was standing still last tick. */
	@Unique private boolean moresteam$wasStanding;
	/** Whether the train left a standstill and has not reached the pull-away cutoff speed yet. */
	@Unique private boolean moresteam$pullingAway;

	/**
	 * Adds the bursts of steam that shoot from the cylinders towards the back of the train, on
	 * carriages with a firebox: a strong one from every cylinder for a few seconds after the train
	 * stops, a weaker one from each cylinder every so often while standing, and short ones while
	 * the train pulls away from a standstill until it reaches the cutoff speed.
	 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void moresteam$steamBursts(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
		// prevMotion already holds this tick's motion.
		boolean standing = this.arrived && this.prevMotion < 0.002;

		// The stop burst starts together with Create's own stop steam (its "!spark" case) but runs on
		// its own, longer clock.
		boolean createSteam = standing && this.depressurise > 0 && this.depressurise <= MoreSteamConfig.steamTicks();
		if (!standing) {
			this.moresteam$stopTicks = 0;
			this.moresteam$stopStarted = false;
			moresteam$trackBrakeSpeed();
		} else {
			// The pressure for this stop has been taken on arrival; start the next trip from scratch.
			this.moresteam$brakeSpeed = 0;
			this.moresteam$holdSpeed = 0;
			this.moresteam$holdTicks = 0;
			// A barely-moved train (where Create also skips its stop sound) gives no burst.
			if (createSteam && !this.moresteam$stopStarted) {
				this.moresteam$stopStarted = true;
				if (moresteam$hasFirebox() && this.moresteam$pressure > 0.1f) {
					this.moresteam$stopTotal = MoreSteamConfig.stopBurstTicks(MoreSteamConfig.curvePressure(this.moresteam$pressure));
					this.moresteam$stopTicks = this.moresteam$stopTotal;
				}
			}
		}
		boolean stopping = this.moresteam$stopTicks > 0;
		// Low pressures count for much less than high ones.
		float pressure = MoreSteamConfig.curvePressure(this.moresteam$pressure);
		// Full strength for the first half, then tapering off towards the end.
		float stopStrength = 1;
		if (stopping) {
			float progress = 1 - this.moresteam$stopTicks / (float) this.moresteam$stopTotal;
			stopStrength = progress < 0.5f ? 1 : Mth.lerp((progress - 0.5f) * 2, 1f, 0.3f);
			// Less pressure: less steam per tick.
			stopStrength *= 0.2f + 0.8f * pressure;
			this.moresteam$stopTicks--;
		}

		// Idle steam waits until the stop hiss has died away, and then a while longer.
		this.moresteam$stopSounds.removeIf(s -> !Minecraft.getInstance().getSoundManager().isActive(s));
		boolean hissing = !this.moresteam$stopSounds.isEmpty();
		if (!standing) {
			this.moresteam$stopSounds.clear();
			this.moresteam$idleDelay = 0;
			this.moresteam$idleDelayEnded = false;
		} else if (stopping || hissing) {
			this.moresteam$idleDelay = MoreSteamConfig.idleDelayAfterStopTicks();
		} else if (this.moresteam$idleDelay > 0 && --this.moresteam$idleDelay == 0) {
			this.moresteam$idleDelayEnded = true;
		}
		boolean idle = standing && this.depressurise == 0 && !stopping && !hissing && this.moresteam$idleDelay == 0
			&& MoreSteamConfig.IDLE_STEAM.get() && moresteam$hasFirebox();
		if (idle && this.moresteam$idleDelayEnded) {
			// Right after the delay the cylinders start within a few seconds of each other, instead of
			// first waiting a whole idle interval.
			this.moresteam$idleDelayEnded = false;
			for (int i = 0; i < this.moresteam$burstTimers.length; i++)
				this.moresteam$burstTimers[i] = 1 + this.entity.level().random.nextInt(MoreSteamConfig.idleSpreadTicks() + 1);
		}

		// Pull-away bursts only start when leaving a standstill, never when slowing down, and end for
		// good at the cutoff speed or when the train stands still again.
		boolean departing = this.moresteam$wasStanding && !standing && MoreSteamConfig.PULL_AWAY_STEAM.get()
			&& moresteam$hasFirebox();
		this.moresteam$wasStanding = standing;
		if (departing) {
			this.moresteam$pullingAway = true;
			// Every cylinder blows as the train sets off.
			Arrays.fill(this.moresteam$burstTimers, -MoreSteamConfig.pullAwayBurstTicks());
		}
		double cutoff = this.entity.getCarriage().train.maxSpeed() * MoreSteamConfig.PULL_AWAY_CUTOFF.get();
		if (this.moresteam$pullingAway && (standing || this.prevMotion >= cutoff)) {
			this.moresteam$pullingAway = false;
			departing = false;
		}
		boolean pullingAway = this.moresteam$pullingAway;

		if (!idle && !pullingAway)
			Arrays.fill(this.moresteam$burstTimers, 0);
		if (!stopping)
			this.moresteam$stopSounded = false;
		if (!stopping && !idle && !pullingAway)
			return;
		boolean playStopSound = stopping && !this.moresteam$stopSounded;
		this.moresteam$stopSounded = stopping;
		int burstTicks = idle ? MoreSteamConfig.idleBurstTicks() : MoreSteamConfig.pullAwayBurstTicks();

		Level level = this.entity.level();
		RandomSource random = level.random;
		Vec3 trainBack = moresteam$trainBack();
		// Steam leaving a moving train keeps some of its speed, as Create's own steam does.
		Vec3 carry = pullingAway
			? this.entity.position().subtract(this.entity.getPrevPositionVec()).scale(0.75)
			: Vec3.ZERO;

		List<CarriageBogey> bogeys = new ArrayList<>(2);
		this.entity.getCarriage().bogeys.forEach(bogeys::add);
		for (int b = 0; b < bogeys.size(); b++) {
			CarriageBogey bogey = bogeys.get(b);
			if (bogey == null)
				continue;
			if (BogeyTraits.isSilent(bogey.getStyle()))
				continue;

			// The bogey's own lengthwise axis, pointed at the back of the train.
			Vec3 back = moresteam$bogeyRotate(bogey, new Vec3(0, 0, 1));
			int backSign = back.dot(trainBack) >= 0 ? 1 : -1;
			back = back.scale(backSign);
			float scale = BogeyTraits.steamScale(bogey);

			// Only cylinders let off steam; both sides of a cylinder blow together. While stopping
			// every cylinder blows at once; otherwise each one keeps its own clock.
			List<Pistons.Piston> pistons = Pistons.of(bogey);
			for (int p = 0; p < pistons.size() && p < 2; p++) {
				int index = b * 2 + p;
				int before = this.moresteam$burstTimers[index];
				if (!stopping && !moresteam$tickBurstTimer(index, idle, burstTicks, random))
					continue;
				Pistons.Piston piston = pistons.get(p);

				// One sound per cylinder, as its burst begins.
				boolean burstStarts = !stopping && before == -burstTicks;
				if (playStopSound || burstStarts) {
					Vec3 middle = moresteam$bogeyToWorld(bogey, piston.offset(1).multiply(0, 1, 1));
					float volume = (piston.small() ? 0.6f : 1f) * (stopping ? 0.3f + 0.7f * pressure : 1f);
					float pitch = (piston.small() ? 1.15f : 1f) * (0.9f + random.nextFloat() * 0.2f);
					SoundEvent sound = stopping ? MoreSteamSounds.STOP_BURST.get()
						: idle ? MoreSteamSounds.IDLE_BURST.get()
						: departing ? MoreSteamSounds.DEPARTURE.get()
						: MoreSteamSounds.PULL_AWAY_BURST.get();
					// The stop sound quietens down once the stop burst has ended.
					int loudTicks = stopping ? this.moresteam$stopTotal : -1;
					// Weaker stops also cut the quieter tail of the hiss short; a full one plays the file out.
					int tailTicks = stopping ? Math.round(160 * pressure) : -1;
					BurstSound instance = new BurstSound(sound, this.entity, middle, volume, pitch, loudTicks, tailTicks,
						random);
					Minecraft.getInstance().getSoundManager().play(instance);
					if (stopping)
						this.moresteam$stopSounds.add(instance);
				}

				for (int side = -1; side <= 1; side += 2) {
					Vec3 pos = moresteam$bogeyToWorld(bogey, piston.offset(side));
					Vec3 out = moresteam$bogeyRotate(bogey, new Vec3(side, 0, 0));
					// Less pressure: the jet does not reach as far.
					double reach = 0.6 + 0.4 * pressure;
					if (stopping && piston.small())
						SteamBurst.emit(level, random, pos, back, out, carry, 2, 0.4 * reach, null, scale * 0.5f * stopStrength);
					else if (stopping)
						SteamBurst.emit(level, random, pos, back, out, carry, 2, 0.52 * reach,
							MoreSteamParticles.BOGEY_STEAM.get(), scale * stopStrength);
					else if (piston.small())
						SteamBurst.emit(level, random, pos, back, out, carry, 1, 0.2, null, scale * 0.5f);
					else
						SteamBurst.emit(level, random, pos, back, out, carry, 1, 0.26, MoreSteamParticles.BOGEY_IDLE_STEAM.get(), scale);
				}
			}
		}
	}

	/**
	 * Keeps track of the speed at which the train started its final braking. While the speed holds
	 * or rises it is taken over at once; a lower speed only counts once the train has held it for two
	 * seconds, so braking all the way to a stop keeps the speed it started braking from.
	 */
	@Unique
	private void moresteam$trackBrakeSpeed() {
		double speed = this.prevMotion;
		if (speed >= this.moresteam$brakeSpeed) {
			this.moresteam$brakeSpeed = speed;
			this.moresteam$holdSpeed = speed;
			this.moresteam$holdTicks = 0;
			return;
		}
		// Within 5% of the top speed counts as holding; anything more is still slowing down.
		double tolerance = 0.05 * this.entity.getCarriage().train.maxSpeed();
		if (Math.abs(speed - this.moresteam$holdSpeed) > tolerance) {
			this.moresteam$holdSpeed = speed;
			this.moresteam$holdTicks = 0;
		} else if (++this.moresteam$holdTicks >= 40) {
			this.moresteam$brakeSpeed = speed;
		}
	}

	/**
	 * Advances one cylinder's idle or pull-away clock by a tick and says whether it is blowing steam
	 * this tick.
	 */
	@Unique
	private boolean moresteam$tickBurstTimer(int index, boolean idle, int burstTicks, RandomSource random) {
		int timer = this.moresteam$burstTimers[index];
		int wait = timer == 0 || timer == 1 || timer == -1
			? (idle ? MoreSteamConfig.idleWaitTicks(random) : MoreSteamConfig.pullAwayWaitTicks(random))
			: 0;
		if (timer == 0) {
			// Just stood still: wait first.
			this.moresteam$burstTimers[index] = wait;
			return false;
		}
		if (timer > 0) {
			this.moresteam$burstTimers[index] = timer == 1 ? -burstTicks : timer - 1;
			return false;
		}
		// Bursting; when the burst is over, wait again.
		this.moresteam$burstTimers[index] = timer == -1 ? wait : timer + 1;
		return true;
	}

	/** Whether any block of this carriage is a firebox. */
	@Unique
	private boolean moresteam$hasFirebox() {
		if (this.moresteam$hasFirebox == null) {
			CarriageContraption contraption = this.entity.getContraption() instanceof CarriageContraption c ? c : null;
			if (contraption == null)
				return false; // not synced yet; ask again next tick
			Block firebox = MoreSteamBlocks.FIREBOX.get();
			this.moresteam$hasFirebox = contraption.getBlocks().values().stream()
				.anyMatch(info -> info.state().is(firebox));
		}
		return this.moresteam$hasFirebox;
	}

	/**
	 * Unit vector from the leading towards the trailing bogey of this carriage, i.e. towards the back
	 * of the train. Uses the same steps with which CarriageParticles#tick places the trailing bogey.
	 */
	@Unique
	private Vec3 moresteam$trainBack() {
		Vec3 v = new Vec3(0, 0, -1);
		v = VecHelper.rotate(v, 180, Axis.Y);
		v = VecHelper.rotate(v, -this.entity.getViewXRot(0), Axis.X);
		return VecHelper.rotate(v, this.entity.getViewYRot(0) + 90, Axis.Y);
	}

	/** Rotates a vector by the bogey's own pitch and yaw, as Create does for particle offsets and motion. */
	@Unique
	private Vec3 moresteam$bogeyRotate(CarriageBogey bogey, Vec3 vec) {
		CarriageBogeyAccessor accessor = (CarriageBogeyAccessor) bogey;
		vec = VecHelper.rotate(vec, accessor.moresteam$getPitch().getValue(0), Axis.X);
		return VecHelper.rotate(vec, accessor.moresteam$getYaw().getValue(0), Axis.Y);
	}

	/** Turns an offset relative to a bogey into a world position, with the same steps as CarriageParticles#tick. */
	@Unique
	private Vec3 moresteam$bogeyToWorld(CarriageBogey bogey, Vec3 offset) {
		float yRot = this.entity.getViewYRot(0);
		float xRot = this.entity.getViewXRot(0);
		int bogeySpacing = this.entity.getCarriage().bogeySpacing;
		boolean leading = ((CarriageBogeyAccessor) bogey).moresteam$isLeading();

		Vec3 v = moresteam$bogeyRotate(bogey, offset);
		v = VecHelper.rotate(v, -yRot - 90, Axis.Y);
		v = VecHelper.rotate(v, xRot, Axis.X);
		v = VecHelper.rotate(v, -180, Axis.Y);
		v = v.add(0, 0, leading ? 0 : -bogeySpacing);
		v = VecHelper.rotate(v, 180, Axis.Y);
		v = VecHelper.rotate(v, -xRot, Axis.X);
		v = VecHelper.rotate(v, yRot + 90, Axis.Y);
		return v.add(this.entity.getPosition(0));
	}
}
