package com.lhwdev.minecraft.railx.mixin.facade.dynamicLuaTable;

import com.lhwdev.minecraft.railx.mixin.MixinUtils;
import com.lhwdev.minecraft.railx.mixin.dynamicLuaTable.OperationHelperMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.OperationHelper;


@Mixin(value = OperationHelper.class, remap = false)
public abstract class OperationHelperMixin {
	@Inject(method = "length(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;I)Lorg/squiddev/cobalt/LuaValue;", at = @At("HEAD"), cancellable = true)
	private static void onLength(LuaState state, LuaValue value, int stack, CallbackInfoReturnable<LuaValue> ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.length(value), ci);
	}
	
	@Inject(method = "intLength", at = @At("HEAD"), cancellable = true)
	private static void onIntLength(LuaState state, LuaValue table, Object $state, CallbackInfoReturnable<Integer> ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.intLength(table), ci);
	}
	
	// @Inject(method = "getTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/LuaValue;", at = @At("HEAD"))
	// private static void getTable(LuaState state, LuaValue t, LuaValue key, CallbackInfoReturnable<LuaValue> ci) {
	//     MixinUtils.onInject(railx$impl.getTable(t, key), ci);
	// }
	
	@Inject(method = "getTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;I)Lorg/squiddev/cobalt/LuaValue;", at = @At("HEAD"), cancellable = true)
	private static void onGetTable(LuaState state, LuaValue t, int key, CallbackInfoReturnable<LuaValue> ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.getTable(t, key), ci);
	}
	
	@Inject(method = "getTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;I)Lorg/squiddev/cobalt/LuaValue;", at = @At("HEAD"), cancellable = true)
	private static void onGetTable(LuaState state, LuaValue t, LuaValue key, int stack, CallbackInfoReturnable<LuaValue> ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.getTable(state, t, key, stack), ci);
	}
	
	@Inject(method = "setTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;)V", at = @At("HEAD"), cancellable = true)
	private static void onSetTable(LuaState state, LuaValue t, LuaValue key, LuaValue value, CallbackInfo ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.setTable(state, t, key, value), ci);
	}
	
	@Inject(method = "setTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;ILorg/squiddev/cobalt/LuaValue;)V", at = @At("HEAD"), cancellable = true)
	private static void onSetTable(LuaState state, LuaValue t, int key, LuaValue value, CallbackInfo ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.setTable(state, t, key, value), ci);
	}
	
	@Inject(method = "setTable(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/LuaValue;I)V", at = @At("HEAD"), cancellable = true)
	private static void onSetTable(LuaState state, LuaValue t, LuaValue key, LuaValue value, int stack, CallbackInfo ci) {
		MixinUtils.onInject(OperationHelperMixinImpl.INSTANCE.setTable(state, t, key, value, stack), ci);
	}
	
	
}
