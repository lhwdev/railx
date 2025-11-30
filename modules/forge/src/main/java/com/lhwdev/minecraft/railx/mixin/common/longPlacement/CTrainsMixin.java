package com.lhwdev.minecraft.railx.mixin.common.longPlacement;

import com.simibubi.create.infrastructure.config.CTrains;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(value = CTrains.class, remap = false)
public class CTrainsMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/infrastructure/config" +
		"/CTrains;i(IIILjava/lang/String;[Ljava/lang/String;)Lnet/createmod/catnip/config/ConfigBase$ConfigInt;"),
		index = 2)
	int maxForTrackPlacementLength(int current, int min, int max, String name, String[] comment) {
		if(!name.equals("maxTrackPlacementLength"))
			throw new RuntimeException("railx broken due to Create update; call developer");
		
		return 1024;
	}
}
