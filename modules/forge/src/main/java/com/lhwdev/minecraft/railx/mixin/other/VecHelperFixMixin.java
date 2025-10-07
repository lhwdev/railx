package com.lhwdev.minecraft.railx.mixin.other;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(VecHelper.class)
public class VecHelperFixMixin {
	/**
	 * @author lhwdev
	 * @reason semantics of function is so simple that no one will overwrite it (if it is not bugfix as same as me)
	 */
	@Overwrite
	public static Vec3 slerp(float p, Vec3 from, Vec3 to) {
		double theta = Math.acos(from.dot(to));
		if(Math.abs(theta) < 0.001) return VecHelper.lerp(p, from, to);
		return from.scale(Mth.sin(1 - p) * theta)
			.add(to.scale(Mth.sin((float) (theta * p))))
			.scale(1 / Mth.sin((float) theta));
	}
}
