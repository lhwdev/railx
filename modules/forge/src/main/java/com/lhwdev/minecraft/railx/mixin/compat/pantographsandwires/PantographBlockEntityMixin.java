package com.lhwdev.minecraft.railx.mixin.compat.pantographsandwires;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(value = PantographBlockEntity.class, remap = false)
public abstract class PantographBlockEntityMixin {
	@ModifyExpressionValue(method = "calculateWireContact", at = @At(value = "INVOKE", target = "Ljava/lang/Math;" +
		"sqrt(D)D", ordinal = 0), require = 0)
	private double railx$localContactHeight(
		double original,
		Vector3d worldPosition,
		Vector3d upVec,
		Vector3d rightVec,
		@Local(name = "d", index = 14) Vector3d intersection
	) {
		return new Vector3d(intersection).sub(worldPosition).dot(upVec) / upVec.length();
	}
}
