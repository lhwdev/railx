package com.lhwdev.minecraft.railx.mixin.common.carriageTilt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.Rotate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@SuppressWarnings("rawtypes")
@Mixin(TrackBlockOutline.class)
public class TrackBlockOutlineMixin {
	@WrapOperation(method = "drawCurveSelection", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib" +
		"/transform/PoseTransformStack;rotateX(F)Ldev/engine_room/flywheel/lib/transform/Rotate;"))
	private static Rotate drawCurveSelection(
		PoseTransformStack instance,
		float v,
		Operation<Rotate> original,
		@Local(index = 7) Vec3 angles
	) {
		return instance.rotateZ((float) (angles.z - Math.PI / 2));
	}
}
