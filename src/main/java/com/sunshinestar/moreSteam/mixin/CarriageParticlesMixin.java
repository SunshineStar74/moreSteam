package com.sunshinestar.moreSteam.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageParticles;
import com.sunshinestar.moreSteam.BogeyTraits;
import com.sunshinestar.moreSteam.MoreSteamConfig;
import com.sunshinestar.moreSteam.particle.MoreSteamParticles;
import com.sunshinestar.moreSteam.particle.SteamBurst;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
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
		int steam = Math.min(this.depressurise, MoreSteamConfig.VANILLA_STEAM_TICKS);
		int delay = this.depressurise - steam;
		this.depressurise = MoreSteamConfig.scaleSteamTicks(steam) + delay;
	}

	/**
	 * Adds the bursts of steam that shoot towards the back of the train: a strong one on every bogey
	 * while Create releases its own stop steam, and a weak one every now and then while standing.
	 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void moresteam$steamBursts(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
		// prevMotion already holds this tick's motion.
		if (!this.arrived || this.prevMotion >= 0.002)
			return;
		// Same window in which Create itself releases steam (its "!spark" case).
		boolean stopping = this.depressurise > 0 && this.depressurise <= MoreSteamConfig.steamTicks();
		boolean idle = this.depressurise == 0 && MoreSteamConfig.IDLE_STEAM.get();
		if (!stopping && !idle)
			return;

		Level level = this.entity.level();
		RandomSource random = level.random;
		int interval = MoreSteamConfig.IDLE_STEAM_INTERVAL.get();
		Vec3 trainBack = moresteam$trainBack();

		for (CarriageBogey bogey : this.entity.getCarriage().bogeys) {
			if (bogey == null)
				continue;
			if (BogeyTraits.isSilent(bogey.getStyle()))
				continue;

			// The bogey's own lengthwise axis, pointed at the back of the train.
			Vec3 back = moresteam$bogeyRotate(bogey, new Vec3(0, 0, 1));
			int backSign = back.dot(trainBack) >= 0 ? 1 : -1;
			back = back.scale(backSign);

			// Both sides always let off steam together.
			if (idle && random.nextInt(interval) != 0)
				continue;

			double halfWidth = BogeyTraits.halfWidth(bogey);
			float scale = BogeyTraits.steamScale(bogey);
			// Create's particle origin sits 0.6 above the rail; aim just below the axle line of the wheels.
			double height = -0.6 + bogey.getSize().wheelRadius() - 0.1;

			for (int side = -1; side <= 1; side += 2) {
				// Low on the side of the bogey, at its front end, so the jet runs back along the wheels.
				Vec3 pos = moresteam$bogeyToWorld(bogey, new Vec3(side * halfWidth, height, -backSign * 0.9));
				Vec3 out = moresteam$bogeyRotate(bogey, new Vec3(side, 0, 0));
				if (stopping)
					SteamBurst.emit(level, random, pos, back, out, Vec3.ZERO, 2, 0.52, MoreSteamParticles.BOGEY_STEAM.get(), scale);
				else
					SteamBurst.emit(level, random, pos, back, out, Vec3.ZERO, 1, 0.26, MoreSteamParticles.BOGEY_IDLE_STEAM.get(), scale);
			}
		}
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
