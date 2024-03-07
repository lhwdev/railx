package com.lhwdev.minecraft.railx.cc

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaValues
import org.squiddev.cobalt.*
import java.util.*


internal class TableImpl(private val arguments: VarargArguments, table: LuaTable) :
	dan200.computercraft.api.lua.LuaTable<Any, Any?>, Map<Any, Any?> {
	private val backingMap: Map<Any, Any?> by lazy(LazyThreadSafetyMode.NONE) {
		@Suppress("UNCHECKED_CAST")
		toObject(table, null) as Map<Any, Any?>
	}
	
	private val table: LuaTable = table
		get() {
			checkValid()
			return field
		}
	
	override val size: Int
		get() = table.size()
	
	@Throws(LuaException::class)
	override fun getLong(index: Int): Long {
		val value = table.rawget(index)
		if(value !is LuaNumber) throw LuaValues.badTableItem(index, "number", value.typeName())
		if(value is LuaInteger) return value.toInteger().toLong()
		val number = value.toDouble()
		if(!java.lang.Double.isFinite(number)) throw LuaValues.badTableItem(
			index,
			"number",
			LuaValues.getNumericType(number)
		)
		return number.toLong()
	}
	
	override fun isEmpty(): Boolean {
		return try {
			table.next(Constants.NIL).first().isNil
		} catch(e: LuaError) {
			throw IllegalStateException(e)
		}
	}
	
	private fun getImpl(key: Any): LuaValue {
		val table = table
		return when(key) {
			is String -> table.rawget(key)
			is Int -> table.rawget(key)
			else -> Constants.NIL
		}
	}
	
	override operator fun get(key: Any) = toObject(getImpl(key), null)
	
	override fun containsKey(key: Any) = !getImpl(key).isNil
	
	override fun containsValue(value: Any?): Boolean {
		return backingMap.containsKey(value)
	}
	
	override val keys: MutableSet<Any>
		get() = backingMap.keys as MutableSet<Any>
	
	override val values: MutableCollection<Any?>
		get() = backingMap.values as MutableCollection<Any?>
	
	@Suppress("UNCHECKED_CAST")
	override val entries: MutableSet<MutableMap.MutableEntry<Any, Any?>>
		get() = backingMap.entries as MutableSet<MutableMap.MutableEntry<Any, Any?>>
	
	private fun checkValid() {
		check(!arguments.isClosed()) { "Cannot use LuaTable after IArguments has been released" }
	}
}
