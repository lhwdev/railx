package com.lhwdev.minecraft.railx.mixin.other;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = VecHelper.class, remap = false)
public abstract class VecHelperFixMixin {
	@Shadow(remap = false)
	public static Vec3 lerp(float p, Vec3 from, Vec3 to) {return null;}
	
	@Inject(method = "slerp", at = @At("HEAD"), cancellable = true, remap = false)
	private static void slerp(float p, Vec3 from, Vec3 to, CallbackInfoReturnable<Vec3> cir) {
		if(Math.abs(from.dot(to) - 1) < 0.001) cir.setReturnValue(lerp(p, from, to));
	}
}
