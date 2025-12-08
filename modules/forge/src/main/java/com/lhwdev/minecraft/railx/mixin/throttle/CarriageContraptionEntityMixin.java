package com.lhwdev.minecraft.railx.mixin.throttle;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.realisticSpeed.RealisticTrainSpeedKt;
import com.lhwdev.minecraft.railx.throttle.ThrottleStubs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;


@Mixin(CarriageContraptionEntity.class)
public class CarriageContraptionEntityMixin {
	@Shadow private Carriage carriage;
	
	@WrapOperation(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains" +
		"/entity/Train;approachTargetSpeed(F)V"))
	void approachTargetSpeed(
		Train instance,
		float accelerationMod,
		Operation<Void> original,
		@Local(index = 2, argsOnly = true) Collection<Integer> heldControls
	) {
		if(!(heldControls instanceof ThrottleStubs.HeldControls controls)) {
			original.call(instance, accelerationMod);
			return;
		}
		
		var train = carriage.train;
		var throttle = controls.getThrottle();
		if(RailXConfig.Server.Value.getRealisticSpeed().getEnabled().isTrue() && train.navigation.destination == null) {
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
