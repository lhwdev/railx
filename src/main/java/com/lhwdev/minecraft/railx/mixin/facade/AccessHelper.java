package com.lhwdev.minecraft.railx.mixin.facade;

import com.google.errorprone.annotations.Keep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.squiddev.cobalt.Lua;


// See mixinInit.kt for detail.
@Mixin(value = Lua.class, remap = false)
public class AccessHelper {
	@SuppressWarnings("unused")
	@Keep
	@Unique
	private static void railx$addReadsTo(Module module) {
		AccessHelper.class.getModule().addReads(module);
	}
}
