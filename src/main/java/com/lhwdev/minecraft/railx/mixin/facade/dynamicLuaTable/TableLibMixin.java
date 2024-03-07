package com.lhwdev.minecraft.railx.mixin.facade.dynamicLuaTable;

import com.lhwdev.minecraft.railx.mixin.MixinUtils;
import com.lhwdev.minecraft.railx.mixin.dynamicLuaTable.TableLibMixinImpl;
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
import org.squiddev.cobalt.lib.TableLib;


@SuppressWarnings({"OverwriteAuthorRequired", "RedundantThrows", "SpellCheckingInspection"})
@Mixin(value = TableLib.class, remap = false)
public class TableLibMixin {
	@Overwrite
	private static LuaValue checkTableLike(LuaState state, Varargs args, int index, int flags) throws LuaError {
		return TableLibMixinImpl.INSTANCE.checkTableLike(state, args, index, flags);
	}
	
	@Overwrite
	private static LuaValue getn(LuaState state, LuaValue arg) throws LuaError {
		return TableLibMixinImpl.INSTANCE.getn(arg);
	}
	
	@Inject(method = "maxn", at = @At("HEAD"), cancellable = true)
	private static void maxn(LuaState state, LuaValue arg, CallbackInfoReturnable<LuaValue> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.maxn(arg), ci);
	}
	
	@Inject(method = "remove", at = @At("HEAD"), cancellable = true)
	private static void remove(LuaState state, DebugFrame frame, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.remove(args), ci);
	}
	
	@Inject(method = "insert", at = @At("HEAD"), cancellable = true)
	private static void insert(LuaState state, DebugFrame frame, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.insert(args), ci);
	}
	
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private static void move(LuaState state, DebugFrame di, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.move(args), ci);
	}
	
	@Inject(method = "foreach", at = @At("HEAD"), cancellable = true)
	private static void foreach(LuaState state, DebugFrame di, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.foreach(state, args), ci);
	}
	
	@Inject(method = "foreachi", at = @At("HEAD"), cancellable = true)
	private static void foreachi(LuaState state, DebugFrame di, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.foreachi(state, args), ci);
	}
	
	@Inject(method = "unpack", at = @At("HEAD"), cancellable = true)
	private static void unpack(LuaState state, DebugFrame di, Varargs args, CallbackInfoReturnable<Varargs> ci) {
		MixinUtils.onInject(TableLibMixinImpl.INSTANCE.unpack(args), ci);
	}
}
