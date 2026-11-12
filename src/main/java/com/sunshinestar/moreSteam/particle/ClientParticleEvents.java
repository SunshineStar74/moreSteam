package com.sunshinestar.moreSteam.particle;

import com.sunshinestar.moreSteam.MoreSteam;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = MoreSteam.MOD_ID, value = Dist.CLIENT)
public final class ClientParticleEvents {

	private ClientParticleEvents() {}

	@SubscribeEvent
	public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(MoreSteamParticles.BOGEY_STEAM.get(), BogeySteamParticle.Provider::new);
	}
}
