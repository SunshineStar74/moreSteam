package com.sunshinestar.moreSteam;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point: makes the config editable from the in-game mod list, with the same
 * config screen as Create (Catnip's, shipped in Ponder).
 */
@Mod(value = MoreSteam.MOD_ID, dist = Dist.CLIENT)
public final class MoreSteamClient {

	public MoreSteamClient(ModContainer modContainer) {
		BaseConfigScreen.setDefaultActionFor(MoreSteam.MOD_ID, base -> base
			.withButtonLabels("Client Settings", null, null)
			.withSpecs(MoreSteamConfig.SPEC, null, null));
		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
			(mc, parent) -> new BaseConfigScreen(parent, MoreSteam.MOD_ID));
	}
}
