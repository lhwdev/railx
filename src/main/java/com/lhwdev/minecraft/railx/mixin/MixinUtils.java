package com.lhwdev.minecraft.railx.mixin;


import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;


public class MixinUtils {
	public static <T> void onInject(@Nullable T result, CallbackInfoReturnable<T> ci) {
		if(result != null) {
			ci.setReturnValue(result);
		}
	}
	
	public static void onInject(boolean result, CallbackInfo ci) {
		if(result) {
			ci.cancel();
		}
	}
}
