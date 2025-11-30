package com.lhwdev.minecraft.railx.mixin.common.manualStation;

import com.lhwdev.minecraft.railx.common.ManualStation;
import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = Train.class)
public class TrainMixin {
	@Inject(method = "canDisassemble", at = @At("HEAD"), cancellable = true, remap = false)
	void canDisassemble(CallbackInfoReturnable<Boolean> cir) {
		if(!ManualStation.INSTANCE.canDisassemble((Train) (Object) this))
			cir.setReturnValue(false);
	}
}
