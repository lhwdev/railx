package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.lhwdev.minecraft.railx.common.carriageTilt.OrientedContraptionEntityWithRoll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(OrientedContraptionEntity.class)
public abstract class ClientOrientedContraptionEntityMixin {
	@Shadow
	public abstract Direction getInitialOrientation();
	
	@Inject(method = "applyLocalTransforms", at = @At("RETURN"))
	void applyRollToLocalTransforms(PoseStack matrixStack, float partialTicks, CallbackInfo ci) {
		float degree = ((OrientedContraptionEntityWithRoll) this).getRailx$viewZRot(partialTicks);
		
		var stack = TransformStack.of(matrixStack);
		switch(getInitialOrientation()) {
			case Direction.EAST -> stack.rotateZCenteredDegrees(degree);
			case Direction.WEST -> stack.rotateZCenteredDegrees(-degree);
			case Direction.NORTH -> stack.rotateXCenteredDegrees(degree);
			default -> stack.rotateXCenteredDegrees(-degree);
		}
	}
}
