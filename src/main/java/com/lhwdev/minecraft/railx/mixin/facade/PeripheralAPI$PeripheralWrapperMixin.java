package com.lhwdev.minecraft.railx.mixin.facade;


import com.lhwdev.minecraft.railx.cc.LuaMachineAccess;
import com.lhwdev.minecraft.railx.mixin.PeripheralWrapperAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(targets = "dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper", remap = false)
public abstract class PeripheralAPI$PeripheralWrapperMixin implements PeripheralWrapperAccess {
	@NotNull
	@Override
	public LuaMachineAccess getMachine() {
		return null;
	}
}
