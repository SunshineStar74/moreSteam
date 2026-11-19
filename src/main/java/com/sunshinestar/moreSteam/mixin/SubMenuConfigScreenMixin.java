package com.sunshinestar.moreSteam.mixin;

import java.util.ArrayList;
import java.util.List;

import com.sunshinestar.moreSteam.MoreSteam;

import net.createmod.catnip.config.ui.ConfigScreen;
import net.createmod.catnip.config.ui.ConfigScreenList;
import net.createmod.catnip.config.ui.SubMenuConfigScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create's config screen lists every page alphabetically. On More Steam's pages this moves the
 * "Enable ..." switches to the top, right after that sorting.
 */
@Mixin(SubMenuConfigScreen.class)
public abstract class SubMenuConfigScreenMixin {

	@Shadow protected ConfigScreenList list;

	@Inject(
		method = "init",
		at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ui/ConfigScreenList;search(Ljava/lang/String;)Z", ordinal = 0)
	)
	private void moresteam$enableSwitchesFirst(CallbackInfo ci) {
		if (!MoreSteam.MOD_ID.equals(ConfigScreen.modID))
			return;
		List<ConfigScreenList.Entry> entries = this.list.children();
		List<ConfigScreenList.Entry> switches = new ArrayList<>();
		for (ConfigScreenList.Entry entry : entries)
			if (entry instanceof ConfigScreenList.LabeledEntry labeled
				&& ((LabeledEntryAccessor) labeled).moresteam$getLabel().getComponent().getString().startsWith("Enable "))
				switches.add(entry);
		entries.removeAll(switches);
		entries.addAll(0, switches);
	}
}
