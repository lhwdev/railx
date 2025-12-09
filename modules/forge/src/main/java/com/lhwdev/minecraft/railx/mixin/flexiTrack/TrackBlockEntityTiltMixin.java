package com.lhwdev.minecraft.railx.mixin.flexiTrack;

import com.lhwdev.minecraft.railx.flexiTrack.graph.TrackNodeLocationUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackBlockEntityTilt;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(TrackBlockEntityTilt.class)
public class TrackBlockEntityTiltMixin {
	@WrapOperation(method = "tryApplySmoothing", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data" +
		"/Couple;setFirst(Ljava/lang/Object;)V", ordinal = 1, remap = false), remap = false)
	void setHighStarts(
		Couple<Vec3> instance,
		Object o,
		Operation<Void> original,
		@Local(index = 23) int smoothingParam
	) {
		// ignore original value
		var start = instance.getFirst();
		start = start.add(0, (double) smoothingParam / 16.0, 0);
		original.call(instance, start);
	}
	
	@ModifyExpressionValue(method = "restoreToOriginalCurve", at = @At(value = "NEW", target = "(Lnet/minecraft" +
		"/world/phys/Vec3;)Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;", remap = false), remap = false)
	TrackNodeLocation restoreToOriginalStart(TrackNodeLocation original) {
		TrackNodeLocationUtils.restoreToOriginal(original);
		return original;
	}
}
