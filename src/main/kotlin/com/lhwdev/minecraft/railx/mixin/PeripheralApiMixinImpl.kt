package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.api.lua.RawLuaValue
import com.lhwdev.minecraft.railx.cc.LuaMachineAccess
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.core.lua.CobaltLuaMachine
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral
import org.squiddev.cobalt.LuaTable


object PeripheralApiMixinImpl {
	val GetWrappedObject = "railx\$PeripheralApiMixinImpl_getWrappedObject"
	
	
	fun PeripheralAPIAccess.wrap(context: ILuaContext, args: IArguments): MethodResult {
		val machine = context.computer.executor.machine as LuaMachineAccess
		val name = args.getString(0)
		val handle = remoteHandle(context, name) ?: return MethodResult.of()
		return if(GetWrappedObject in handle.methods) {
			handle.call(context, GetWrappedObject, ObjectArguments())
		} else {
			wrapOld(context, machine, name, handle)
		}
	}
	
	private fun wrapOld(
		context: ILuaContext,
		machine: LuaMachineAccess,
		name: String,
		handle: RemoteHandle,
	): MethodResult {
		val table = LuaTable()
		for(method in handle.methods) {
			table.rawset(
				method,
				ResultInterpreterFunction(
					machine as CobaltLuaMachine,
					null,
					context,
					name
				) { _, ctx, args ->
					handle.call(ctx, name, args)
				}
			)
		}
		return MethodResult.of(RawLuaValue(table))
	}
}


private interface RemoteHandle {
	val instance: IPeripheral
	
	val types: Collection<String>
	
	val methods: Collection<String>
	
	fun call(context: ILuaContext, methodName: String, args: IArguments): MethodResult
}

private fun PeripheralAPIAccess.remoteHandle(context: ILuaContext, name: String): RemoteHandle? {
	val side = ComputerSide.valueOfInsensitive(name)
	val peripheral = side?.let { peripherals[it.ordinal] }
	if(peripheral != null) {
		return object : RemoteHandle {
			override val instance: IPeripheral
				get() = peripheral.peripheral
			
			override val types: Collection<String>
				get() = peripheral.types
			
			override val methods: Collection<String>
				get() = peripheral.methods
			
			override fun call(context: ILuaContext, methodName: String, args: IArguments): MethodResult =
				peripheral.call(context, methodName, args)
		}
	}
	
	for(wrapper in peripherals) {
		if(wrapper == null) continue
		val iPeripheral = wrapper.peripheral
		if(iPeripheral is WiredModemPeripheral) {
			val remote = iPeripheral.getWrapper(wrapper, name)
			
			if(remote != null) return object : RemoteHandle {
				override val instance: IPeripheral
					get() = remote.peripheral
				
				override val types: Collection<String>
					get() = remote.additionalTypes + remote.type
				
				override val methods: Collection<String>
					get() = remote.methodNames
				
				override fun call(context: ILuaContext, methodName: String, args: IArguments): MethodResult =
					remote.callMethod(context, methodName, args)
			} else {
				continue
			}
		}
		if(
			"peripheral_hub" in wrapper.types &&
			wrapper.call(context, "isPresentRemote", ObjectArguments(name)).result?.firstOrNull() == true
		) {
			TODO()
			// return { args -> wrapper.call(context, "callRemote", args) }
		}
	}
	return null
}
