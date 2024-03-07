package com.lhwdev.minecraft.railx.mixin.facade.dynamicLuaTable;

import com.lhwdev.minecraft.railx.mixin.MixinUtils;
import com.lhwdev.minecraft.railx.mixin.dynamicLuaTable.BaseLibMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.lib.BaseLib;


@SuppressWarnings({"SpellCheckingInspection", "RedundantThrows"})
@Mixin(value = BaseLib.class, remap = false)
public abstract class BaseLibMixin {
	/**
	 * @author lhwdev
	 * @reason to support DynamicTable
	 */
	@Overwrite
	private static LuaValue rawget(LuaState state, Varargs args) throws LuaError {
		return BaseLibMixinImpl.INSTANCE.rawget(args);
	}
	
	/**
	 * @author lhwdev
	 * @reason to support DynamicTable
	 */
	@Overwrite
	private static LuaValue rawset(LuaState state, Varargs args) throws LuaError {
		return BaseLibMixinImpl.INSTANCE.rawset(args);
	}
	
	/**
	 * @author lhwdev
	 * @reason to support DynamicTable
	 */
	@Overwrite
	private static LuaValue rawlen(LuaState state, LuaValue arg) throws LuaError {
		return BaseLibMixinImpl.INSTANCE.rawlen(arg);
	}
	
	@Inject(method = "next", at = @At("HEAD"), cancellable = true)
	private static void next(LuaState state, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(BaseLibMixinImpl.INSTANCE.next(args), ci);
	}
	
	@Inject(method = "pairs", at = @At("HEAD"), cancellable = true)
	private void pairs(LuaState state, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(BaseLibMixinImpl.INSTANCE.pairs(args), ci);
	}
	
	@Inject(method = "ipairs", at = @At("HEAD"), cancellable = true)
	private void ipairs(LuaState state, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(BaseLibMixinImpl.INSTANCE.ipairs(args), ci);
	}
	
	@Inject(method = "inext", at = @At("HEAD"), cancellable = true)
	private static void inext(LuaState state, DebugFrame di, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(BaseLibMixinImpl.INSTANCE.inext(args), ci);
	}
}
