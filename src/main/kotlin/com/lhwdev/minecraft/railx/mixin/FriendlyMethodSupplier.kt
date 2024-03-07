package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.core.asm.LuaMethodSupplier
import dan200.computercraft.core.asm.PeripheralMethodSupplier
import dan200.computercraft.core.methods.MethodSupplier
import dan200.computercraft.core.methods.NamedMethod
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod


interface FriendlyMethodSupplier {
	fun <T> MethodSupplier<T>.getMethods(klass: Class<*>): List<NamedMethod<T>>?
}


abstract class FriendlyMethodSupplierBase : FriendlyMethodSupplier {
	override fun <T> MethodSupplier<T>.getMethods(klass: Class<*>): List<NamedMethod<T>>? {
		val factory = generator.factory
		
		return getMethods(klass, generator, factory = { factory.apply(it) })
	}
	
	protected abstract fun <T> MethodSupplier<T>.getMethods(
		klass: Class<*>,
		generator: Generator<T>,
		factory: (MethodHandle) -> T,
	): List<NamedMethod<T>>?
}

object LuaAllMethodSupplier : FriendlyMethodSupplierBase() {
	private object Generators {
		private fun supplierOf(klass: Class<*>) =
			klass.getDeclaredField("GENERATOR").also { it.isAccessible = true }.get(null)
		
		val PeripheralGenerator = supplierOf(PeripheralMethodSupplier::class.java)
		val MethodGenerator = supplierOf(LuaMethodSupplier::class.java)
		
		init {
			println("PeripheralGenerator=$PeripheralGenerator or, MethodGenerator=$MethodGenerator")
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	private object Handles {
		private val lookup = MethodHandles.lookup()
		
		val peripheral = lookup.unreflect(Handles::peripheralHandler.javaMethod!!)
		val method = lookup.unreflect(Handles::methodHandler.javaMethod!!)
		
		@JvmStatic
		private fun peripheralHandler(
			target: Any,
			context: ILuaContext,
			computer: IComputerAccess,
			args: IArguments,
		): MethodResult {
			return MethodResult.of(target)
		}
		
		@JvmStatic
		private fun methodHandler(
			target: Any,
			context: ILuaContext,
			args: IArguments,
		): MethodResult {
			return MethodResult.of(target)
		}
	}
	
	override fun <T> MethodSupplier<T>.getMethods(
		klass: Class<*>,
		generator: Generator<T>,
		factory: (MethodHandle) -> T,
	): List<NamedMethod<T>>? {
		if(klass.isAnnotationPresent(LuaObject::class.java)) {
			val handle = when(generator.value) {
				Generators.PeripheralGenerator -> Handles.peripheral
				Generators.MethodGenerator -> Handles.method
				else -> error("error in implementation; unknown generator ${generator.value}")
			}
			val stubMethod = NamedMethod(PeripheralApiMixinImpl.GetWrappedObject, factory(handle), true, null)
			return listOf(stubMethod)
		}
		return null
	}
}
