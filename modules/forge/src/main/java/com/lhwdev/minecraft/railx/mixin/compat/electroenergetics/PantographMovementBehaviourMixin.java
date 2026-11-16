package com.lhwdev.minecraft.railx.mixin.compat.electroenergetics;

import com.george_vi.electroenergetics.content.railway_electrification.pantograph.PantographMovementBehaviour;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(value = PantographMovementBehaviour.class, remap = false)
public abstract class PantographMovementBehaviourMixin {
	@WrapOperation(method = "checkCatenary", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/math" +
		"/VecHelper;rotate(Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/core/Direction$Axis;)" +
		"Lnet/minecraft/world/phys/Vec3;"), require = 0)
	Vec3 contactDistanceToLocal(
		Vec3 vec,
		double deg,
		Direction.Axis axis,
		Operation<Vec3> original,
		@Local(argsOnly = true, index = 4) MovementContext context
	) {
		Vec3 localDistance = context.contraption.entity.reverseRotation(vec, 1);
		double facingAngle = context.state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 90;
		return original.call(localDistance, facingAngle, axis);
	}
	
	@ModifyReturnValue(method = "checkCatenary", at = @At("RETURN"), require = 0)
	Vec3 wireContactToLocal(
		Vec3 contact,
		@Local(argsOnly = true, index = 4) MovementContext context
	) {
		if(contact == null) return null;
		if(context == null) return contact;
		return context.contraption.entity.reverseRotation(contact.subtract(context.position), 1);
	}
	
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lcom/george_vi/electroenergetics" +
		"/content/railway_electrification/pantograph/PantographMovementBehaviour;getConnectorPos" +
		"(FLcom/simibubi/create/content/contraptions/behaviour/MovementContext;)Lnet/minecraft/world/phys/Vec3;",
		ordinal = 0), require = 0)
	Vec3 connectorPos1ToLocal(Vec3 original, MovementContext context) {
		return context.contraption.entity.reverseRotation(original.subtract(context.position), 1);
	}
	
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lcom/george_vi/electroenergetics" +
		"/content/railway_electrification/pantograph/PantographMovementBehaviour;getConnectorPos" +
		"(FLcom/simibubi/create/content/contraptions/behaviour/MovementContext;)Lnet/minecraft/world/phys/Vec3;",
		ordinal = 1), require = 0)
	Vec3 connectorPos2ToLocal(Vec3 original, MovementContext context) {
		return context.contraption.entity.reverseRotation(original.subtract(context.position), 1);
	}
}
