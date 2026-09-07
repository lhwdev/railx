package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.common.carriageTilt.CarriageBogeyWithTilt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntityRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CarriageContraptionEntityRenderer.class)
public class CarriageContraptionEntityRendererMixin {
	@Inject(method = "translateBogey", at = @At("RETURN"))
	private static void translateBogeyForRoll(
		PoseStack ms,
		CarriageBogey bogey,
		int bogeySpacing,
		float viewYRot,
		float viewXRot,
		float partialTicks,
		CallbackInfo ci
	) {
		float tilt = ((CarriageBogeyWithTilt) bogey).getRailx$tilt()
			.getValue(partialTicks);
		
		TransformStack.of(ms)
			.rotateZDegrees(-tilt);
	}
}
