package com.sunshinestar.moreSteam;

import com.sunshinestar.moreSteam.particle.MoreSteamParticles;

import net.minecraftforge.fml.common.Mod;

@Mod(MoreSteam.MOD_ID)
public final class MoreSteam {

	public static final String MOD_ID = "moresteam";

	public MoreSteam() {
		MoreSteamParticles.register();
	}
}
