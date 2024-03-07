package com.lhwdev.minecraft.railx.mixin

import com.lhwdev.minecraft.railx.api.lua.DynamicLuaMap
import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.MutableDynamicLuaMap
import com.lhwdev.minecraft.railx.api.lua.RawLuaValue
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.cc.*
import com.lhwdev.minecraft.railx.lua.LuaExecutionContext
import com.lhwdev.minecraft.railx.utils.accHandle
import dan200.computercraft.api.lua.LuaTask
import dan200.computercraft.core.computer.Computer
import dan200.computercraft.core.lua.CobaltLuaMachine
import dan200.computercraft.core.lua.MachineResult
import kotlinx.coroutines.flow.MutableSharedFlow
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.squiddev.cobalt.LuaState
import org.squiddev.cobalt.LuaTable
import org.squiddev.cobalt.LuaValue
import org.squiddev.cobalt.debug.DebugHelpers
import java.util.*


private typealias EventHandler = CobaltLuaMachineMixinInterface.(args: Array<out Any>) -> Unit


interface CobaltLuaMachineMixinInterface : LuaMachineAccess {
	val computer: Computer
}

class CobaltLuaMachineMixinImpl(private val machine: CobaltLuaMachineMixinInterface) {
	companion object {
		val IsLuaWorkerThread = ThreadLocal<Boolean>()
	}
	
	private object Access {
		val CobaltLuaMachine_toObject = CobaltLuaMachine::class.java.getDeclaredMethod(
			"toObject",
			LuaValue::class.java,
			IdentityHashMap::class.java
		).accHandle
	}
	
	
	fun fromValue(value: LuaValue, cache: IdentityHashMap<LuaValue, Any?>?): Any? =
		Access.CobaltLuaMachine_toObject.invokeExact(value, cache)
	
	fun onToValue(value: Any?, ci: CallbackInfoReturnable<LuaValue>) {
		// TODO: handle () -> Unit and suspend () -> Unit option
		when(value) {
			is MutableDynamicLuaMap<*, *> -> ci.returnValue =
				object : MutableConvertedLuaMap(machine) {
					override val original: MutableMap<Any?, Any?> =
						@Suppress("UNCHECKED_CAST")
						(value as MutableMap<Any?, Any?>)
				}.asMutableDynamicLuaMap()
			
			is DynamicLuaMap<*, *> -> ci.returnValue = object : ConvertedLuaMap(machine) {
				override val original: Map<*, *> = value
			}.asDynamicLuaMap()
			
			is RawLuaValue -> ci.returnValue = value.value
			is LuaValue -> {
				println("unexpected LuaValue '$value': raw org.squiddev.cobalt.LuaValue types are expected to be wrapped in RawLuaValue")
				// (LuaValue::class.java.getDeclaredField("createdBy").get(value) as Throwable).printStackTrace()
				println(DebugHelpers.traceback(machine.state.currentThread, 0))
				Throwable().printStackTrace()
			}
			// is LuaValue -> ci.returnValue = value
		}
	}
	
	fun onWrapLuaObject(value: Any, ci: CallbackInfoReturnable<LuaTable>) {
		if(!value::class.java.isAnnotationPresent(LuaObject::class.java)) return
		
		ci.returnValue = objectProvider.createLuaObject(value)
	}
	
	
	object Events {
		val Handlers = IdentityHashMap<String, EventHandler>()
		
		private fun handler(name: String, handler: EventHandler): String {
			Handlers[name] = handler
			return name
		}
		
		
		val RunLuaThreadTask = handler("cobalt_mixin_lhwdev:run_lua_thread_task") { args ->
			@Suppress("UNCHECKED_CAST")
			val task = args[0] as (state: LuaState) -> Unit
			
			task(state)
		}
	}
	
	fun onHandleEvent(eventName: String?, arguments: Array<out Any>?, ci: CallbackInfoReturnable<MachineResult>) {
		if(eventName != null && arguments != null) {
			Events.Handlers[eventName]?.let { handler ->
				machine.handler(arguments)
				ci.returnValue = MachineResult.OK
			}
		}
		
		if(eventName != null) {
			eventSource.tryEmit(LuaVararg(eventName, *arguments ?: emptyArray()))
		}
	}
	
	
	val eventSource = MutableSharedFlow<LuaVararg>(extraBufferCapacity = 16)
	
	val executionContext = object : LuaExecutionContext {
		override val isOnLuaThread: Boolean
			get() = IsLuaWorkerThread.get() == true // is this right? IDK
		
		override fun issueLuaThreadTask(task: (state: LuaState) -> Unit) {
			queueEvent(Events.RunLuaThreadTask, task)
		}
		
		override fun issueMainThreadTask(task: LuaTask): Long =
			machine.context.issueMainThreadTask(task)
		
		
		override val bridge: LuaBridgeContext = object : LuaBridgeContext,
			LuaBridgeFunctionContext by LuaBridgeFunctionContextImpl(this) {
			
			override val computer: Computer = machine.computer
		}
		
		
		override val events = object : MutableSharedFlow<LuaVararg> by eventSource {
			override suspend fun emit(value: LuaVararg) {
				tryEmit(value)
			}
			
			override fun tryEmit(value: LuaVararg): Boolean {
				val event = value.values.first()
				val args = value.values.drop(1).toTypedArray()
				
				machine.computer.queueEvent(event as String, args)
				return true
			}
		}
		
		
		override fun toLuaValue(value: Any?): LuaValue = machine.convertToValue(value, null)
		
		override fun fromLuaValue(value: LuaValue): Any? = machine.convertFromValue(value, null)
	}
	
	
	val objectProvider = LuaObjectProvider(executionContext, machine)
}

