package com.sunshinestar.moreSteam;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point: makes the config editable from the in-game mod list. */
@Mod(value = MoreSteam.MOD_ID, dist = Dist.CLIENT)
public final class MoreSteamClient {

	public MoreSteamClient(ModContainer modContainer) {
		modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}
}
