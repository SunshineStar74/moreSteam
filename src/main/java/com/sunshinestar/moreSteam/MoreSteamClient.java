package com.sunshinestar.moreSteam;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.sunshinestar.moreSteam.client.FireboxCTBehaviour;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point: makes the config editable from the in-game mod list, with the same
 * config screen as Create (Catnip's, shipped in Ponder), and gives the firebox Create's connected
 * train casing textures.
 */
@Mod(value = MoreSteam.MOD_ID, dist = Dist.CLIENT)
public final class MoreSteamClient {

	public MoreSteamClient(IEventBus modEventBus, ModContainer modContainer) {
		BaseConfigScreen.setDefaultActionFor(MoreSteam.MOD_ID, base -> base
			.withButtonLabels("Client Settings", null, null)
			.withSpecs(MoreSteamConfig.SPEC, null, null));
		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
			(mc, parent) -> new BaseConfigScreen(parent, MoreSteam.MOD_ID));

		// Create swaps the firebox's baked model for a connected-texture one when models load.
		FireboxCTBehaviour behaviour = new FireboxCTBehaviour();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
			ResourceLocation.fromNamespaceAndPath(MoreSteam.MOD_ID, "firebox"), model -> new CTModel(model, behaviour));
		modEventBus.addListener(MoreSteamClient::clientSetup);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		// Lets Create's encased blocks treat the firebox like a train casing too.
		event.enqueueWork(() -> CreateClient.CASING_CONNECTIVITY.makeCasing(MoreSteamBlocks.FIREBOX.get(),
			AllSpriteShifts.RAILWAY_CASING_SIDE));
	}
}
