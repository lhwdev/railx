package com.lhwdev.minecraft.railx.lua

import com.lhwdev.minecraft.railx.api.lua.LuaVararg
import com.lhwdev.minecraft.railx.api.lua.RawLuaTable
import com.lhwdev.minecraft.railx.cc.LuaBridgeContext
import com.lhwdev.minecraft.railx.cc.LuaContext
import dan200.computercraft.api.peripheral.IComputerAccess
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.squiddev.cobalt.LuaValue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext


interface LuaExecutionContext : LuaContext {
	companion object : ThreadLocalContextKey<LuaExecutionContext>()
	
	val bridge: LuaBridgeContext
	
	val events: MutableSharedFlow<LuaVararg>
	
	suspend fun waitEvent(event: String): LuaVararg =
		events.first { it.values.first() == event }
	
	suspend fun waitEvent(event: String, filter: (event: LuaVararg) -> Boolean): LuaVararg =
		events.first { it.values.first() == event && filter(it) }
	
	fun queueEvent(event: String, vararg args: Any?) {
		events.tryEmit(LuaVararg(event, *args))
	}
	
	
	fun toLuaValue(value: Any?): LuaValue
	
	fun fromLuaValue(value: LuaValue): Any?
}

interface LuaPeripheralContext : LuaExecutionContext {
	companion object : ThreadLocalContextKey<LuaPeripheralContext>()
	
	val computer: IComputerAccess
}

interface LuaMemberContext {
	companion object : ThreadLocalContextKey<LuaMemberContext>()
	
	val luaSelf: RawLuaTable
}


open class ThreadLocalContextKey<T>(
	val defaultValue: () -> T = { error("not implemented") },
	val parent: (ThreadLocalContextKey<in T>)? = null,
) : CoroutineContext.Key<ThreadLocalElement<T>> {
	internal val threadLocal = ThreadLocal<T>()
	
	val current: T
		get() = threadLocal.get() ?: defaultValue()
	
	val currentOrNull: T?
		get() = threadLocal.get() ?: null
	
	suspend fun current(): T =
		currentOrNull() ?: defaultValue()
	
	suspend fun currentOrNull(): T? =
		coroutineContext[this]?.value
	
	infix fun provides(value: T): CoroutineContext =
		ThreadLocalElement(value, key = this).let {
			if(parent != null) {
				parent.provides(value) + it
			} else {
				it
			}
		}
}

internal class ThreadLocalElement<T>(
	val value: T,
	override val key: ThreadLocalContextKey<T>,
) : ThreadContextElement<T> {
	override fun updateThreadContext(context: CoroutineContext): T {
		val oldState = key.threadLocal.get()
		key.threadLocal.set(value)
		return oldState
	}
	
	override fun restoreThreadContext(context: CoroutineContext, oldState: T) {
		key.threadLocal.set(oldState)
	}
	
	override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
		return if(this.key == key) EmptyCoroutineContext else this
	}
	
	override operator fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? =
		@Suppress("UNCHECKED_CAST")
		if(this.key == key) this as E else null
	
	override fun toString(): String = "ThreadLocal(value=$value, key=$key)"
}
