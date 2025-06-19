package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeed;
import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = Train.class, remap = false)
public class TrainMixin {
	@Unique
	private RealisticTrainSpeed railx$impl;
	
	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		railx$impl = new RealisticTrainSpeed((Train) (Object) this);
	}
	
	@Inject(method = "tickPassiveSlowdown", at = @At("HEAD"), cancellable = true)
	void onTickPassiveSlowdown(CallbackInfo ci) {
		boolean handled = railx$impl.handleTickSpeed();
		if(handled) ci.cancel();
	}
}
