package com.lhwdev.minecraft.railx.mixin.facade;

import com.lhwdev.minecraft.railx.mixin.CobaltLuaMachineMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(targets = "dan200.computercraft.core.computer.computerthread.ComputerThread$WorkerThread", remap = false)
public class ComputerThread$WorkerThreadMixin {
	@Inject(method = "run", at = @At("HEAD"))
	private void beforeRun(CallbackInfo ci) {
		CobaltLuaMachineMixinImpl.Companion.getIsLuaWorkerThread().set(true);
	}
}
