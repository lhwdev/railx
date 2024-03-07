package com.lhwdev.minecraft.railx.mixin.dynamicLuaTable

import com.lhwdev.minecraft.railx.cc.DynamicLuaTable
import com.lhwdev.minecraft.railx.cc.dynamicTable
import org.squiddev.cobalt.*
import org.squiddev.cobalt.function.LuaFunction


@Suppress("FunctionName")
object OperationHelperMixinImpl {
	private val LuaTable_trySet = LuaTable::class.java.getDeclaredMethod(
		"trySet",
		LuaValue::class.java, LuaValue::class.java
	).also { it.isAccessible = true }
	
	private val LuaTable_trySet_int = LuaTable::class.java.getDeclaredMethod(
		"trySet",
		Int::class.java, LuaValue::class.java
	).also { it.isAccessible = true }
	
	private fun LuaTable_trySet(table: LuaTable, key: LuaValue, value: LuaValue): Boolean =
		LuaTable_trySet.invoke(table, key, value) as Boolean
	
	private fun LuaTable_trySet(table: LuaTable, key: Int, value: LuaValue): Boolean =
		LuaTable_trySet_int.invoke(table, key, value) as Boolean
	
	
	fun length(value: LuaValue): LuaValue? {
		value.dynamicTable?.let { return ValueFactory.valueOf(it.size) }
		return null
	}
	
	// Note that original function is converted, via @AutoUnwind
	fun intLength(table: LuaValue): Int? {
		table.dynamicTable?.let {
			return it.size
		}
		return null
	}
	
	
	fun getTable(t: LuaValue, key: Int): LuaValue? {
		if(t is DynamicLuaTable) {
			val value = t.rawGet(key)
			if(!value.isNil) return value
		}
		return null
	}
	
	fun getTable(state: LuaState, t: LuaValue, key: LuaValue, stack: Int): LuaValue? {
		var loops = 0
		var main = t
		while(loops < Constants.MAXTAGLOOP) {
			main.dynamicTable?.let {
				val value = it.rawGet(key)
				if(!value.isNil) return value
			}
			
			val metaIndex: LuaValue
			if(main is LuaTable) {
				val value = main.rawget(key)
				if(!value.isNil) return value
				metaIndex = main.metatag(state, CachedMetamethod.INDEX)
				if(metaIndex.isNil) return Constants.NIL
			} else {
				metaIndex = main.metatag(state, CachedMetamethod.INDEX)
			}
			
			if(metaIndex.isNil) {
				throw ErrorFactory.operandError(state, main, "index", stack)
			}
			if(metaIndex is LuaFunction) {
				return metaIndex.call(state, main, key)
			}
			main = metaIndex
			loops++
		}
		
		throw LuaError("loop in gettable")
	}
	
	fun setTable(state: LuaState, t: LuaValue, key: LuaValue, value: LuaValue): Boolean =
		setTable(state, t, key, value, -1)
	
	fun setTable(state: LuaState, t: LuaValue, key: Int, value: LuaValue): Boolean {
		t.dynamicTable?.let {
			if(it.rawSet(key, value)) return true
		}
		if(t is LuaTable && LuaTable_trySet(t, key, value)) return true
		
		return setTable(state, t, ValueFactory.valueOf(key), value, -1)
	}
	
	fun setTable(state: LuaState, t: LuaValue, key: LuaValue, value: LuaValue, stack: Int): Boolean {
		var loops = 0
		var main = t
		while(loops++ < Constants.MAXTAGLOOP) {
			main.dynamicTable?.let {
				if(it.rawSet(key, value)) return true
			}
			if(main is LuaTable && LuaTable_trySet(main, key, value)) return true
			
			val metaNewIndex = main.metatag(state, CachedMetamethod.NEWINDEX)
			if(metaNewIndex.isNil) {
				throw ErrorFactory.operandError(state, main, "index", stack)
			}
			if(metaNewIndex is LuaFunction) {
				metaNewIndex.call(state, t, key, value)
				return true
			}
			main = metaNewIndex
		}
		
		throw LuaError("loop in settable")
	}
}
