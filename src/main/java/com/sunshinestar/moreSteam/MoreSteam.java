package com.sunshinestar.moreSteam;

import com.sunshinestar.moreSteam.dev.TestTrains;
import com.sunshinestar.moreSteam.particle.MoreSteamParticles;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MoreSteam.MOD_ID)
public final class MoreSteam {

	public static final String MOD_ID = "moresteam";

	public MoreSteam(IEventBus modEventBus, ModContainer modContainer) {
		MoreSteamParticles.PARTICLE_TYPES.register(modEventBus);
		MoreSteamBlocks.register(modEventBus);
		MoreSteamSounds.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.CLIENT, MoreSteamConfig.SPEC);
		// The /moresteam testtrains command is a development aid and stays out of released builds.
		if (!FMLEnvironment.production)
			TestTrains.register();
	}
}
