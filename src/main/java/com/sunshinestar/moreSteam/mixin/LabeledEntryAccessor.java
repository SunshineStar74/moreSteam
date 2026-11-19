package com.sunshinestar.moreSteam.mixin;

import net.createmod.catnip.config.ui.ConfigScreenList;
import net.createmod.catnip.gui.element.TextStencilElement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the label of an entry on Create's (Catnip's) config screen. */
@Mixin(ConfigScreenList.LabeledEntry.class)
public interface LabeledEntryAccessor {

	@Accessor("label")
	TextStencilElement moresteam$getLabel();
}
