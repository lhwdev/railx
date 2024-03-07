package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.cc.LuaComputerAccess
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.core.apis.IAPIEnvironment
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.core.methods.MethodSupplier
import dan200.computercraft.core.methods.PeripheralMethod


interface PeripheralAPIAccess {
	val peripheralMethods: MethodSupplier<PeripheralMethod>
	
	val peripherals: Array<out PeripheralWrapperAccess?>
	
	val environment: IAPIEnvironment
	
	val api: PeripheralAPI
}


interface PeripheralWrapperAccess : LuaComputerAccess {
	val peripheral: IPeripheral
	
	val type: String
	
	val additionalTypes: Set<String>
	
	val isAttached: Boolean
	
	val methods: Collection<String>
	
	
	fun attach()
	
	fun detach()
	
	fun unmountAll()
	
	fun call(context: ILuaContext, methodName: String, arguments: IArguments): MethodResult
}


val PeripheralWrapperAccess.types: Set<String>
	get() = additionalTypes + type
