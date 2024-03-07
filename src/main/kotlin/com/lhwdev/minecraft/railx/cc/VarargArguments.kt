package com.lhwdev.minecraft.railx.cc

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaValues
import org.slf4j.LoggerFactory
import org.squiddev.cobalt.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.Volatile


class VarargArguments : IArguments {
	private val varargs: Varargs
	
	@Volatile
	private var closed = false
	private val root: VarargArguments
	private var cache: ArraySlice<Any?>? = null
	private var typeNames: ArraySlice<String?>? = null
	private var escapes = false
	
	private constructor(varargs: Varargs) {
		this.varargs = varargs
		root = this
	}
	
	private constructor(varargs: Varargs, root: VarargArguments, offset: Int) {
		this.varargs = varargs
		this.root = root
		escapes = root.escapes
		cache = if(root.cache == null) null else root.cache!!.drop(offset)
		typeNames = if(root.typeNames == null) null else root.typeNames!!.drop(offset)
	}
	
	fun isClosed(): Boolean {
		return root.closed
	}
	
	private fun checkAccessible() {
		if(isClosed() && !escapes) throwInaccessible()
	}
	
	private fun throwInaccessible() {
		val error = IllegalStateException("Function arguments have escaped their original scope.")
		if(!reportedIllegalGet) {
			reportedIllegalGet = true
			LOG.error(
				"A function attempted to access arguments outside the scope of the original function. This is probably " +
					"caused by the function scheduling work on the main thread. You may need to call IArguments.escapes().",
				error
			)
		}
		throw error
	}
	
	override fun count(): Int {
		return varargs.count()
	}
	
	override fun get(index: Int): Any? {
		checkAccessible()
		if(index < 0 || index >= varargs.count()) return null
		var cache = cache
		if(cache == null) {
			this.cache = ArraySlice(arrayOfNulls(varargs.count()), 0)
			cache = this.cache
		} else {
			val existing = cache[index]
			if(existing != null) return existing
		}
		val arg = varargs.arg(index + 1)
		assert(!isClosed() || arg !is LuaTable) { "Converting a LuaTable after arguments were closed." }
		val converted = toObject(arg, null)
		cache!![index] = converted
		return converted
	}
	
	override fun getStringCoerced(index: Int): String {
		checkAccessible()
		// This doesn't run __tostring, which is _technically_ wrong, but avoids a lot of complexity.
		return varargs.arg(index + 1).toString()
	}
	
	override fun getType(index: Int): String {
		checkAccessible()
		val value = varargs.arg(index + 1)
		
		// If we've escaped, read it from the precomputed list, otherwise get the custom name.
		val name = if(escapes) (if(typeNames == null) null else typeNames!![index]) else getCustomType(value)
		return if(name != null) name else value.typeName()
	}
	
	override fun drop(count: Int): IArguments {
		check(!(count < 0)) { "count cannot be negative" }
		if(count == 0) return this
		val newArgs = varargs.subargs(count + 1)
		return if(newArgs === Constants.NONE) EMPTY else VarargArguments(newArgs, this, count)
	}
	
	@Throws(LuaException::class)
	override fun getDouble(index: Int): Double {
		checkAccessible()
		val value = varargs.arg(index + 1)
		if(value !is LuaNumber) throw LuaValues.badArgument(index, "number", value.typeName())
		return value.toDouble()
	}
	
	@Throws(LuaException::class)
	override fun getLong(index: Int): Long {
		checkAccessible()
		val value = varargs.arg(index + 1)
		if(value !is LuaNumber) throw LuaValues.badArgument(index, "number", value.typeName())
		return (value as? LuaInteger)?.toInteger()?.toLong()
			?: LuaValues.checkFinite(index, value.toDouble()).toLong()
	}
	
	@Throws(LuaException::class)
	override fun getBytes(index: Int): ByteBuffer {
		checkAccessible()
		val value = varargs.arg(index + 1)
		if(value !is LuaString) throw LuaValues.badArgument(index, "string", value.typeName())
		return value.toBuffer()
	}
	
	@Throws(LuaException::class)
	override fun optBytes(index: Int): Optional<ByteBuffer> {
		checkAccessible()
		val value = varargs.arg(index + 1)
		if(value.isNil) return Optional.empty()
		if(value !is LuaString) throw LuaValues.badArgument(index, "string", value.typeName())
		return Optional.of(value.toBuffer())
	}
	
	@Throws(LuaException::class)
	override fun getTableUnsafe(index: Int): dan200.computercraft.api.lua.LuaTable<*, *> {
		if(isClosed()) throw IllegalStateException("Cannot use getTableUnsafe after IArguments has been closed.")
		val value = varargs.arg(index + 1)
		if(value !is LuaTable) throw LuaValues.badArgument(index, "table", value.typeName())
		return TableImpl(this, value)
	}
	
	@Throws(LuaException::class)
	override fun optTableUnsafe(index: Int): Optional<dan200.computercraft.api.lua.LuaTable<*, *>> {
		if(isClosed()) throw IllegalStateException("Cannot use optTableUnsafe after IArguments has been closed.")
		val value = varargs.arg(index + 1)
		if(value.isNil) return Optional.empty()
		if(value !is LuaTable) throw LuaValues.badArgument(index, "table", value.typeName())
		return Optional.of(TableImpl(this, value))
	}
	
	override fun escapes(): IArguments {
		if(escapes) return this
		if(isClosed()) throw IllegalStateException("Cannot call escapes after IArguments has been closed.")
		var cache = cache
		var typeNames = typeNames
		var i = 0
		val count = varargs.count()
		while(i < count) {
			val arg = varargs.arg(i + 1)
			
			// Convert tables.
			if(arg is LuaTable) {
				if(cache == null) cache = ArraySlice(arrayOfNulls(count), 0)
				cache.set(i, toObject(arg, null))
			}
			
			// Fetch custom type names.
			val typeName = getCustomType(arg)
			if(typeName != null) {
				if(typeNames == null) typeNames = ArraySlice(arrayOfNulls(count), 0)
				typeNames.set(i, typeName)
			}
			i++
		}
		escapes = true
		this.cache = cache
		this.typeNames = typeNames
		return this
	}
	
	fun close() {
		closed = true
	}
	
	private class ArraySlice<T>(val array: Array<T>, val offset: Int) {
		// FIXME: We should be able to remove the @Nullables if we update NullAway.
		operator fun get(index: Int): T {
			return array[offset + index]
		}
		
		operator fun set(index: Int, value: T) {
			array[offset + index] = value
		}
		
		fun drop(count: Int): ArraySlice<T> {
			return ArraySlice(array, offset + count)
		}
	}
	
	companion object {
		private val LOG = LoggerFactory.getLogger(VarargArguments::class.java)
		private val EMPTY = VarargArguments(Constants.NONE)
		private var reportedIllegalGet = false
		
		init {
			EMPTY.closed = true
			EMPTY.escapes = EMPTY.closed
		}
		
		fun of(values: Varargs): VarargArguments {
			return if(values === Constants.NONE) EMPTY else VarargArguments(values)
		}
		
		private fun getCustomType(arg: LuaValue): String? {
			if(arg !is LuaTable && arg !is LuaUserdata) return null
			val metatable = arg.getMetatable(null) ?: return null
			
			return (metatable.rawget(Constants.NAME) as? LuaString)?.toString()
		}
	}
}

