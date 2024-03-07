@file:JvmName("CcAccess")
@file:Suppress("UNCHECKED_CAST")

package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.cc.LuaMachineAccess
import com.lhwdev.minecraft.railx.utils.acc
import com.lhwdev.minecraft.railx.utils.accGetterHandle
import com.lhwdev.minecraft.railx.utils.accHandle
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.core.apis.ComputerAccess
import dan200.computercraft.core.apis.IAPIEnvironment
import dan200.computercraft.core.apis.PeripheralAPI
import dan200.computercraft.core.computer.Computer
import dan200.computercraft.core.computer.Environment
import dan200.computercraft.core.computer.computerthread.ComputerScheduler
import dan200.computercraft.core.lua.CobaltLuaMachine
import dan200.computercraft.core.lua.ILuaMachine
import dan200.computercraft.core.methods.LuaMethod
import dan200.computercraft.core.methods.MethodSupplier
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral
import org.squiddev.cobalt.function.LuaFunction
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles


private object Access {
	val ComputerAccess = ComputerAccess::class.java
	val ComputerAccess_environment = ComputerAccess.getDeclaredField("environment").accGetterHandle
	
	val MethodSupplierImpl = Class.forName("dan200.computercraft.core.asm.MethodSupplierImpl")
	val MethodSupplierImpl_generator = MethodSupplierImpl.getDeclaredField("generator").accGetterHandle
	
	val Generator = Class.forName("dan200.computercraft.core.asm.Generator")
	val Generator_factory = Generator.getDeclaredField("factory").accGetterHandle
	
	val Environment = Environment::class.java
	val Environment_computer = Environment.getDeclaredField("computer").accGetterHandle
	
	val Computer = Computer::class.java
	val Computer_executor = Computer.getDeclaredField("executor").accGetterHandle
	
	val ComputerExecutor = Class.forName("dan200.computercraft.core.computer.ComputerExecutor")
	val ComputerExecutor_machine = ComputerExecutor.getDeclaredField("machine").accGetterHandle
	
	val WiredModemPeripheral = WiredModemPeripheral::class.java
	val WiredModemPeripheral_getWrapper =
		WiredModemPeripheral.getDeclaredMethod("getWrapper", IComputerAccess::class.java, String::class.java).accHandle
	val WiredModemPeripheral_RemotePeripheralWrapper =
		Class.forName("${WiredModemPeripheral.name}\$RemotePeripheralWrapper")
	val WiredModemPeripheral_RemotePeripheralWrapper_computer =
		WiredModemPeripheral_RemotePeripheralWrapper.getDeclaredField("computer").accGetterHandle
	
	val PeripheralAPI = PeripheralAPI::class.java
	val PeripheralAPI_peripherals = PeripheralAPI.getDeclaredField("peripherals").accGetterHandle
	
	val ResultInterpreterFunction = Class.forName("dan200.computercraft.core.lua.ResultInterpreterFunction")
	val ResultInterpreterFunction_ctor = ResultInterpreterFunction.getDeclaredConstructor(
		CobaltLuaMachine::class.java,
		LuaMethod::class.java,
		Any::class.java,
		ILuaContext::class.java,
		String::class.java
	).acc.let { MethodHandles.publicLookup().unreflectConstructor(it) }
	
	val LuaContext = Class.forName("dan200.computercraft.core.computer.LuaContext")
	val LuaContext_computer = LuaContext.getDeclaredField("computer").accGetterHandle
}


/// cc.asm ->

@JvmInline
value class Generator<@Suppress("unused") T>(val value: Any)

val <T> MethodSupplier<T>.generator: Generator<T>
	get() = Generator(Access.MethodSupplierImpl_generator.invoke(this))

val <T> Generator<T>.factory: java.util.function.Function<MethodHandle, T>
	get() = Access.Generator_factory.invoke(value) as java.util.function.Function<MethodHandle, T>


/// IComputerAccess ->

val ComputerAccess.environment: IAPIEnvironment
	get() = Access.ComputerAccess_environment.invokeExact(this) as IAPIEnvironment

val Environment.computer: Computer
	get() = Access.Environment_computer.invokeExact(this) as Computer

val Computer.executor: ComputerScheduler.Worker
	get() = Access.Computer_executor.invoke(this) as ComputerScheduler.Worker

val ComputerScheduler.Worker.machine: ILuaMachine
	get() = Access.ComputerExecutor_machine.invoke(this) as ILuaMachine

val IComputerAccess.computerAccess: ComputerAccess
	get() = when {
		this is ComputerAccess -> this
		
		Access.WiredModemPeripheral_RemotePeripheralWrapper.isInstance(this) ->
			Access.WiredModemPeripheral_RemotePeripheralWrapper_computer.invoke(this) as ComputerAccess
		
		else -> error("unexpected IComputerAccess instance ${this::class.java.simpleName}")
	}

val IComputerAccess.machine: LuaMachineAccess
	get() = (computerAccess.environment as Environment).computer.executor.machine as LuaMachineAccess


/// WiredModemPeripheral ->

fun WiredModemPeripheral.getWrapper(computer: IComputerAccess, name: String): WiredModemPeripheralItem? =
	Access.WiredModemPeripheral_getWrapper.invoke(this, computer, name) as WiredModemPeripheralItem?


/// PeripheralAPI ->

val PeripheralAPI.peripherals: Array<out PeripheralWrapperAccess>
	get() = Access.PeripheralAPI_peripherals.invoke(this) as Array<out PeripheralWrapperAccess>


/// ResultInterpreterFunction ->

fun ResultInterpreterFunction(
	machine: CobaltLuaMachine,
	instance: Any?,
	context: ILuaContext,
	name: String,
	method: LuaMethod,
): LuaFunction = Access.ResultInterpreterFunction_ctor.invoke(machine, method, instance, context, name) as LuaFunction


/// LuaContext ->

val ILuaContext.computer: Computer
	get() = Access.LuaContext_computer.invoke(this) as Computer
