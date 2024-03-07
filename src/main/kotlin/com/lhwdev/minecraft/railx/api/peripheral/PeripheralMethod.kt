package com.lhwdev.minecraft.railx.api.peripheral

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess

fun interface PeripheralMethod {
	@Throws(LuaException::class)
	fun run(iComputerAccess: IComputerAccess, iLuaContext: ILuaContext, iArguments: IArguments): MethodResult?
}
