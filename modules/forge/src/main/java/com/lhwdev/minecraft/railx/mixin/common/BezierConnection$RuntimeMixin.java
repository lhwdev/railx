package com.lhwdev.minecraft.railx.mixin.common;

import com.lhwdev.minecraft.railx.RailXConfig;
import com.lhwdev.minecraft.railx.utils.ConfigKt;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;


@Mixin(targets = "com.simibubi.create.content.trains.track.BezierConnection$Runtime")
public class BezierConnection$RuntimeMixin {
	@Definition(id = "intersect", local = @Local(type = double[].class, index = 18))
	@Expression("intersect[?]")
	@ModifyExpressionValue(method = "determineHandles", at = @At(value = "MIXINEXTRAS:EXPRESSION"),
		slice = @Slice(from = @At(value = "RETURN", ordinal = 1)))
	double makeRadiusSymmetric(double original, @Local(index = 18) double[] intersect) {
		if(!ConfigKt.getOrFalse(RailXConfig.Server.Value.getCommon().getFixTrackBezierAsymmetry())) return original;
		return Math.max(intersect[0], intersect[1]);
	}
}
