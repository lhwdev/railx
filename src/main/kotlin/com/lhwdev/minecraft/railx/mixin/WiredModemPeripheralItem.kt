package com.lhwdev.minecraft.railx.mixin

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import org.spongepowered.asm.mixin.Shadow


interface WiredModemPeripheralItem : IComputerAccess {
	val peripheral: IPeripheral
	
	@Shadow
	fun attach()
	
	@Shadow
	fun detach()
	
	@get:Shadow
	val type: String
	
	@get:Shadow
	val additionalTypes: Set<String>
	
	@get:Shadow
	val methodNames: Collection<String>
	
	@Shadow
	@Throws(LuaException::class)
	fun callMethod(context: ILuaContext, methodName: String, arguments: IArguments): MethodResult
}
