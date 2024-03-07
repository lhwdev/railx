package com.lhwdev.minecraft.railx.api.peripheral

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IDynamicPeripheral
import java.util.*

abstract class SmartPeripheral : IDynamicPeripheral {
	var names: MutableList<String> = LinkedList()
	var methods: MutableList<PeripheralMethod> = LinkedList()
	fun addMethod(name: String, method: PeripheralMethod) {
		names.add(name)
		methods.add(method)
	}
	
	fun removeMethod(name: String): PeripheralMethod {
		val i = names.indexOf(name)
		names.removeAt(i)
		return methods.removeAt(i)
	}
	
	override fun getMethodNames(): Array<String> {
		return names.toTypedArray<String>()
	}
	
	@Throws(LuaException::class)
	override fun callMethod(
		iComputerAccess: IComputerAccess,
		iLuaContext: ILuaContext,
		i: Int,
		iArguments: IArguments,
	): MethodResult {
		return methods[i].run(iComputerAccess, iLuaContext, iArguments)!!
	}
}
