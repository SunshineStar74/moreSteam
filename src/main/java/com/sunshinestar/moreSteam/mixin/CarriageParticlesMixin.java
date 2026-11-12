package com.sunshinestar.moreSteam.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageParticles;
import com.sunshinestar.moreSteam.MoreSteamConfig;
import com.sunshinestar.moreSteam.particle.MoreSteamParticles;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds more steam to the bogeys of Create trains. Everything here is written against
 * CarriageParticles#tick of Create 6.0.10 and only touches particles spawned by that method;
 * the vanilla POOF particle type itself is left alone.
 *
 * <p>How Create times the steam: when a carriage stops, {@code depressurise} is set to
 * {@code 20 * train.accumulatedSteamRelease / 10} (0 to 20) and then counts down by one per tick.
 * Steam is only released while it is between 1 and 10; the ticks above 10 are a silent delay.
 */
@Mixin(CarriageParticles.class)
public abstract class CarriageParticlesMixin {

	/** Steam 'n' Rails hides bogey steam on its handcar; idle steam respects that. */
	@Unique
	private static final ResourceLocation MORESTEAM$HANDCAR = ResourceLocation.fromNamespaceAndPath("railways", "handcar");

	@Shadow CarriageContraptionEntity entity;
	@Shadow boolean arrived;
	@Shadow int depressurise;
	@Shadow double prevMotion;

	/** Swaps the default poof for the big steam cloud while a stopped bogey is releasing steam. */
	@ModifyArg(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
		index = 0
	)
	private ParticleOptions moresteam$replaceStoppedBogeyPoof(ParticleOptions particle) {
		// Bogey styles with their own smoke particle are left alone.
		return this.depressurise > 0 && particle == ParticleTypes.POOF
			? MoreSteamParticles.BOGEY_STEAM.get()
			: particle;
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
		int steam = Math.min(this.depressurise, MoreSteamConfig.VANILLA_STEAM_TICKS);
		int delay = this.depressurise - steam;
		this.depressurise = MoreSteamConfig.scaleSteamTicks(steam) + delay;
	}

	/** Lets the bogeys of a standing train release a small puff of steam every now and then. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void moresteam$idleSteam(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
		if (!MoreSteamConfig.IDLE_STEAM.get())
			return;
		// Standing still and done with the stop steam. prevMotion already holds this tick's motion.
		if (!this.arrived || this.depressurise > 0 || this.prevMotion >= 0.002)
			return;

		Level level = this.entity.level();
		RandomSource random = level.random;
		int interval = MoreSteamConfig.IDLE_STEAM_INTERVAL.get();

		for (CarriageBogey bogey : this.entity.getCarriage().bogeys) {
			if (bogey == null || random.nextInt(interval) != 0)
				continue;
			if (bogey.getStyle().smokeParticle != ParticleTypes.POOF || MORESTEAM$HANDCAR.equals(bogey.getStyle().id))
				continue;

			// One of the four corners Create uses for its own stop steam.
			int side = random.nextBoolean() ? 1 : -1;
			int end = random.nextBoolean() ? 1 : -1;
			Vec3 pos = moresteam$bogeyToWorld(bogey, new Vec3(side * 1.15, 0.32, end));
			// Create's steam direction (outwards and down), at a fraction of the speed.
			Vec3 motion = moresteam$bogeyRotate(bogey, new Vec3(side * 0.08, -0.06, 0));
			level.addParticle(MoreSteamParticles.BOGEY_IDLE_STEAM.get(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
		}
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
