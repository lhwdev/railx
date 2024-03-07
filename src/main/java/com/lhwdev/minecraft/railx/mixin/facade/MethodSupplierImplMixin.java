package com.lhwdev.minecraft.railx.mixin.facade;

import com.lhwdev.minecraft.railx.mixin.LuaAllMethodSupplier;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.NamedMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;


@Mixin(targets = "dan200.computercraft.core.asm.MethodSupplierImpl", remap = false)
public abstract class MethodSupplierImplMixin<T> implements MethodSupplier<T> {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void afterInitialize(CallbackInfo ci) {
		Field generator;
		try {
			generator = getClass().getDeclaredField("generator");
		} catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		generator.setAccessible(true);
		try {
			System.out.println("MethodSupplierImpl(), generator=" + generator.get(this));
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Inject(method = "getMethods", at = @At("HEAD"), cancellable = true)
	private void onGetMethods(Class<?> klass, CallbackInfoReturnable<List<NamedMethod<T>>> ci) {
		var result = LuaAllMethodSupplier.INSTANCE.getMethods(this, klass);
		if(result != null) {
			ci.setReturnValue(result);
		}
	}
}
