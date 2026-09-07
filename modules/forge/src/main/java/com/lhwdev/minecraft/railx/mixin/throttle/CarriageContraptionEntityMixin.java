package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(CarriageContraptionEntity.class)
public class CarriageContraptionEntityMixin {
	@ModifyVariable(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/Train;getCurrentStation()Lcom/simibubi/create/content/trains/station/GlobalStation;"), index = 9,
		expect = 0, order = 2000)
	boolean overwriteSlow(boolean slow) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isTrue())
			return false;
		
		return slow;
	}
	
	@WrapOperation(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/Train;approachTargetSpeed(F)V"))
	void approachTargetSpeed(
		Train instance,
		float accelerationMod,
		Operation<Void> original
	) {
		if(RailXConfig.Server.Value.getThrottle().getEnabled().isTrue() || instance.navigation.destination != null)
			return;
		
		original.call(instance, accelerationMod);
	}
}
