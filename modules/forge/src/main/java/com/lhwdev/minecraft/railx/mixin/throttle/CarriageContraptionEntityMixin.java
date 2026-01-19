package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeedKt;
import com.lhwdev.minecraft.railx.throttle.TrainThrottleUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(CarriageContraptionEntity.class)
public class CarriageContraptionEntityMixin {
	@Shadow(remap = false) private Carriage carriage;
	
	@WrapOperation(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/Train;approachTargetSpeed(F)V", remap = false), remap = false)
	void approachTargetSpeed(
		Train instance,
		float accelerationMod,
		Operation<Void> original
	) {
		if(!RailXConfig.Server.Value.getThrottle().getEnabled().get()) {
			original.call(instance, accelerationMod);
			return;
		}
		
		var train = carriage.train;
		var throttle = TrainThrottleUtils.getAbsoluteThrottle(train);
		if(RailXConfig.Server.Value.getRealisticSpeed().getEnabled().get() && train.navigation.destination == null) {
			var realisticSpeed = RealisticTrainSpeedKt.getRealisticSpeed(train);
			if(realisticSpeed != null) {
				realisticSpeed.handleManualThrottle(throttle, train.targetSpeed);
				return;
			}
		}
		
		if(train.navigation.destination == null) {
			accelerationMod = (float) Math.abs(throttle.getAcceleration());
		} else {
			// throttle according to train.navigation.distanceToDestination and breaking distance
		}
		original.call(instance, accelerationMod);
	}
}
