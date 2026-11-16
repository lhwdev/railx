package com.lhwdev.minecraft.railx.mixin.compat.pantographsandwires;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(value = PantographMovementBehaviour.class, remap = false)
public abstract class PantographMovementBehaviourMixin {
	@ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lde/mrjulsen/paw/blockentity" +
		"/PantographBlockEntity;updateContraptionValues(Lorg/joml/Vector3d;Ljava/util/function/UnaryOperator;)V"),
		index = 0, require = 0)
	Vector3d getBaseOffset(
		Vector3d worldPosition,
		@Local(argsOnly = true, index = 1) MovementContext context
	) {
		double offset = PantographBlockEntity.MIN_HEIGHT - 0.5D;
		Vec3 rotatedOffset = context.rotation.apply(new Vec3(0, offset, 0));
		return new Vector3d(worldPosition).add(rotatedOffset.x, rotatedOffset.y - offset, rotatedOffset.z);
	}
}
