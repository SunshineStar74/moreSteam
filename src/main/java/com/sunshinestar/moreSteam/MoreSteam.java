package com.sunshinestar.moreSteam;

import com.sunshinestar.moreSteam.particle.MoreSteamParticles;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MoreSteam.MOD_ID)
public final class MoreSteam {

	public static final String MOD_ID = "moresteam";

	public MoreSteam(IEventBus modEventBus) {
		MoreSteamParticles.PARTICLE_TYPES.register(modEventBus);
	}
}
