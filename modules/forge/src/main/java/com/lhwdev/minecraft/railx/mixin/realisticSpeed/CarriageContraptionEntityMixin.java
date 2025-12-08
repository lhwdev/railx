package com.lhwdev.minecraft.railx.mixin.realisticSpeed;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(CarriageContraptionEntity.class)
public class CarriageContraptionEntityMixin {
	@ModifyArg(method = "control", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity" +
		"/Train;approachTargetSpeed(F)V"), index = 0)
	float modifyAccelerationMod(float original) {
		if(RailXConfig.Server.Value.getRealisticSpeed().getEnabled().isFalse())
			return original;
		
		return 1;
	}
}
