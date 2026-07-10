package com.sunshinestar.moreSteam.particle;

import com.sunshinestar.moreSteam.MoreSteam;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MoreSteam.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticleEvents {

	private ClientParticleEvents() {}

	@SubscribeEvent
	public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(MoreSteamParticles.BOGEY_STEAM.get(), BogeySteamParticle.Provider::new);
	}
}
