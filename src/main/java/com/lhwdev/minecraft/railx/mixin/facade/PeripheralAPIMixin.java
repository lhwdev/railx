package com.lhwdev.minecraft.railx.mixin.facade;

import com.lhwdev.minecraft.railx.api.lua.RawLuaValue;
import com.lhwdev.minecraft.railx.mixin.CcAccess;
import com.lhwdev.minecraft.railx.mixin.PeripheralAPIAccess;
import com.lhwdev.minecraft.railx.mixin.PeripheralApiMixinImpl;
import com.lhwdev.minecraft.railx.mixin.PeripheralWrapperAccess;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.squiddev.cobalt.LuaValue;


@SuppressWarnings("RedundantThrows")
@Mixin(value = PeripheralAPI.class, remap = false)
public abstract class PeripheralAPIMixin implements PeripheralAPIAccess {
	@Override
	@Accessor
	@NotNull
	public abstract MethodSupplier<PeripheralMethod> getPeripheralMethods();
	
	@Override
	@Accessor
	@NotNull
	public abstract IAPIEnvironment getEnvironment();
	
	@Override
	@NotNull
	public PeripheralWrapperAccess @NotNull [] getPeripherals() {
		return CcAccess.getPeripherals(getApi());
	}
	
	@Override
	@NotNull
	public PeripheralAPI getApi() {
		return (PeripheralAPI) (Object) this;
	}
	
	@LuaFunction
	@Unique
	public final MethodResult wrap(ILuaContext context, IArguments args) throws LuaException {
		return PeripheralApiMixinImpl.INSTANCE.wrap(this, context, args);
	}
}
