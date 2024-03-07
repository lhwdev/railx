package com.lhwdev.minecraft.railx.cc

import com.lhwdev.minecraft.railx.api.lua.RawLuaTable
import com.lhwdev.minecraft.railx.lua.LuaExecutionContext
import com.lhwdev.minecraft.railx.lua.LuaMemberContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.squiddev.cobalt.LuaState
import org.squiddev.cobalt.LuaString
import org.squiddev.cobalt.LuaThread
import org.squiddev.cobalt.Varargs
import org.squiddev.cobalt.debug.DebugFrame
import org.squiddev.cobalt.function.LuaFunction
import org.squiddev.cobalt.function.ResumableVarArgFunction
import org.squiddev.cobalt.function.VarArgFunction
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn


private const val DoneEventId = "bridge_function.done"
private val DoneEventIdLua = LuaString.valueOf(DoneEventId)


class BridgeDispatcherImpl(private val context: LuaBridgeFunctionContextImpl) : CoroutineDispatcher() {
	override fun isDispatchNeeded(context: CoroutineContext): Boolean =
		this.context.context.isOnLuaThread
	
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		this.context.context.issueLuaThreadTask {
			block.run()
		}
	}
}

class BridgeContinuationImpl(
	override val context: CoroutineContext,
	private val parent: LuaBridgeFunction,
) : Continuation<Any?> {
	override fun resumeWith(result: Result<Any?>) {
		@Suppress("UNCHECKED_CAST")
		parent.result = Optional.of(result as Result<Varargs>)
		parent.context.context.queueEvent(DoneEventId, parent.id)
	}
}


interface LuaFunctionContext {
	val context: LuaContext
	
	val state: LuaState
}

fun LuaFunctionContext(context: LuaContext, state: LuaState): LuaFunctionContext = object : LuaFunctionContext {
	override val context: LuaContext = context
	override val state: LuaState = state
}

interface LuaBridgeFunctionContext {
	fun createFunction(block: suspend LuaFunctionContext.(args: Varargs) -> Varargs): LuaFunction
}

class LuaBridgeFunctionContextImpl(val context: LuaExecutionContext) : LuaBridgeFunctionContext {
	internal val dispatcher = BridgeDispatcherImpl(this)
	
	
	override fun createFunction(block: suspend LuaFunctionContext.(args: Varargs) -> Varargs): LuaFunction {
		return LuaBridgeFunction(this, block)
	}
}

private val MaxId = AtomicLong(0)

class LuaBridgeFunction(
	internal val context: LuaBridgeFunctionContextImpl,
	private val block: suspend LuaFunctionContext.(args: Varargs) -> Varargs,
) : ResumableVarArgFunction<BridgeContinuationImpl>() {
	internal val id = MaxId.getAndIncrement()
	internal var result: Optional<Result<Varargs>> = Optional.empty()
	
	private val coroutineContext = createCoroutineContext()
	
	private fun createCoroutineContext(): CoroutineContext {
		val memberContext = object : LuaMemberContext {
			override val luaSelf: RawLuaTable = // TODO
				object : RawLuaTable, Map<Any?, Any?> by mapOf() {
					override fun get(key: Any?) = TODO()
				}
		}
		return context.dispatcher + (LuaMemberContext provides memberContext)
	}
	
	override fun invoke(state: LuaState, di: DebugFrame, args: Varargs): Varargs {
		val functionContext = LuaFunctionContext(context.context, state)
		val function = suspend {
			functionContext.block(args)
		}
		val continuation = BridgeContinuationImpl(context = coroutineContext, parent = this)
		di.state = continuation
		
		val result = function.startCoroutineUninterceptedOrReturn(continuation)
		return if(result == COROUTINE_SUSPENDED) {
			LuaThread.yield(state, DoneEventIdLua)
		} else {
			result as Varargs
		}
	}
	
	override fun resumeThis(state: LuaState, continuation: BridgeContinuationImpl, value: Varargs): Varargs {
		val thisId = value.first().checkLong()
		if(thisId != id) {
			return LuaThread.yield(state, DoneEventIdLua)
		}
		
		return result.get().getOrThrow()
	}
}


inline fun luaSimpleFunction(crossinline block: (state: LuaState, args: Varargs) -> Varargs): LuaFunction =
	object : VarArgFunction() {
		override fun invoke(state: LuaState, args: Varargs): Varargs = block(state, args)
	}
