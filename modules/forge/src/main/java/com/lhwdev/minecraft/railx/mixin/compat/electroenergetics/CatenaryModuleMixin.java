package com.lhwdev.minecraft.railx.mixin.compat.electroenergetics;

import com.george_vi.electroenergetics.simulation.infrastructure.CatenaryModule;
import com.lhwdev.minecraft.railx.common.carriageTilt.OrientedContraptionEntityWithRoll;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(value = CatenaryModule.class, remap = false)
public abstract class CatenaryModuleMixin {
	@WrapOperation(method = "buildCircuit", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/math" +
		"/VecHelper;rotate(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/core/Direction$Axis;)" +
		"Lnet/minecraft/world/phys/Vec3;", ordinal = 0), require = 0)
	Vec3 pantographPosToGlobal(
		Vec3 vec,
		double deg,
		Direction.Axis axis,
		Operation<Vec3> original,
		@Local(index = 11) Carriage.DimensionalCarriageEntity dce
	) {
		return original.call(VecHelper.rotate(vec, -railx$getRoll(dce), Direction.Axis.Z), deg, axis);
	}
	
	@WrapOperation(method = "buildCircuit", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/math" +
		"/VecHelper;rotate(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/core/Direction$Axis;)" +
		"Lnet/minecraft/world/phys/Vec3;", ordinal = 3), require = 0)
	Vec3 contactDistanceToLocal(
		Vec3 vec,
		double deg,
		Direction.Axis axis,
		Operation<Vec3> original,
		@Local(index = 11) Carriage.DimensionalCarriageEntity dce
	) {
		return original.call(vec, -deg + railx$getRoll(dce), Direction.Axis.Z);
	}
	
	@Unique
	private static float railx$getRoll(Carriage.DimensionalCarriageEntity dce) {
		var entity = dce.entity == null ? null : dce.entity.get();
		return entity == null ? 0 : ((OrientedContraptionEntityWithRoll) entity).getRailx$roll();
	}
}
