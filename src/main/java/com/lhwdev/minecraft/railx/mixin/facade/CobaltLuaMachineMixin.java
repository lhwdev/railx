package com.lhwdev.minecraft.railx.mixin.facade;

import com.lhwdev.minecraft.railx.cc.LuaMachineAccess;
import com.lhwdev.minecraft.railx.cc.LuaObjectProvider;
import com.lhwdev.minecraft.railx.mixin.CcAccess;
import com.lhwdev.minecraft.railx.mixin.CobaltLuaMachineMixinImpl;
import com.lhwdev.minecraft.railx.mixin.CobaltLuaMachineMixinInterface;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.lua.MachineResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.util.IdentityHashMap;


@Mixin(value = CobaltLuaMachine.class, remap = false)
public abstract class CobaltLuaMachineMixin implements ILuaMachine, LuaMachineAccess, CobaltLuaMachineMixinInterface {
	@Unique
	private CobaltLuaMachineMixinImpl railx$_impl;
	
	@NotNull
	@Unique
	private CobaltLuaMachineMixinImpl railx$impl() {
		var current = railx$_impl;
		if(current != null) return current;
		
		current = new CobaltLuaMachineMixinImpl(this);
		railx$_impl = current;
		return current;
	}
	
	
	/// Accessors
	
	@Override
	@Accessor
	@NotNull
	public abstract ILuaContext getContext();
	
	@Override
	@Accessor
	@NotNull
	public abstract LuaState getState();
	
	@Override
	@NotNull
	public Computer getComputer() {
		return CcAccess.getComputer(getContext());
	}
	
	@Override
	@NotNull
	public LuaObjectProvider getObjectProvider() {
		return railx$impl().getObjectProvider();
	}
	
	@Override
	@Invoker("toValue")
	@NotNull
	public abstract LuaValue convertToValue(@Nullable Object value, @Nullable IdentityHashMap<Object, LuaValue> cache);
	
	@Nullable
	@Override
	public Object convertFromValue(@NotNull LuaValue value, @Nullable IdentityHashMap<LuaValue, Object> cache) {
		return railx$impl().fromValue(value, cache);
	}
	
	
	/// Injections
	
	@Inject(method = "handleEvent", at = @At("HEAD"), cancellable = true)
	private void onHandleEvent(@Nullable String eventName, @Nullable Object[] arguments, CallbackInfoReturnable<MachineResult> ci) {
		railx$impl().onHandleEvent(eventName, arguments, ci);
	}
	
	@Inject(method = "toValue", at = @At("HEAD"), cancellable = true)
	private void onToValue(@Nullable Object value, @Nullable IdentityHashMap<Object, LuaValue> values, CallbackInfoReturnable<LuaValue> ci) {
		railx$impl().onToValue(value, ci);
	}
	
	@Inject(method = "wrapLuaObject", at = @At("HEAD"), cancellable = true)
	private void onWrapLuaObject(Object value, CallbackInfoReturnable<LuaTable> ci) {
		railx$impl().onWrapLuaObject(value, ci);
	}
}
