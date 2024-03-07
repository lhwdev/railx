package com.lhwdev.minecraft.railx.mixin.facade;

import com.lhwdev.minecraft.railx.mixin.WiredModemPeripheralItem;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;
import java.util.Set;


@Mixin(targets = "dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral$RemotePeripheralWrapper", remap = false)
public abstract class WiredModemPeripheral$WrapperMixin implements WiredModemPeripheralItem {
	@Override
	@Accessor
	@NotNull
	public abstract IPeripheral getPeripheral();
	
	@Override
	@Shadow
	public abstract void attach();
	
	@Override
	@Shadow
	public abstract void detach();
	
	@Override
	@Shadow
	@Nullable
	public abstract String getType();
	
	@Override
	@Shadow
	@Nullable
	public abstract Set<String> getAdditionalTypes();
	
	@Override
	@Shadow
	@Nullable
	public abstract Collection<String> getMethodNames();
	
	@Override
	@Shadow
	@NotNull
	public abstract MethodResult callMethod(@NotNull ILuaContext context, @NotNull String methodName, IArguments arguments) throws LuaException;
}
