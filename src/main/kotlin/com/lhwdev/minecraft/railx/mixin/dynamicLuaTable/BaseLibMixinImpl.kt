@file:Suppress("SpellCheckingInspection")

package com.lhwdev.minecraft.railx.mixin.dynamicLuaTable

import com.lhwdev.minecraft.railx.cc.dynamicTable
import com.lhwdev.minecraft.railx.cc.luaSimpleFunction
import org.squiddev.cobalt.*


private val NeverEquals = object : LuaValue(0) {}


object BaseLibMixinImpl {
	fun rawget(args: Varargs): LuaValue {
		val table = args.checkValue(1)
		val key = args.checkValue(2)
		table.dynamicTable?.let {
			return it.rawGet(key)
		}
		return table.checkTable().rawget(key)
	}
	
	fun rawset(args: Varargs): LuaValue {
		val table = args.checkValue(1)
		val key = args.checkValue(2)
		val value = args.checkValue(3)
		
		table.dynamicTable?.let {
			if(key.isNil) throw LuaError("table index is nil")
			it.rawSet(key, value)
			return table
		}
		
		table.checkTable().rawset(key.checkValidKey(), value)
		return table
	}
	
	
	fun rawlen(value: LuaValue): LuaValue {
		// rawlen( table | string ) -> int
		value.dynamicTable?.let {
			return ValueFactory.valueOf(it.size)
		}
		
		return when(value.type()) {
			Constants.TTABLE -> ValueFactory.valueOf(value.checkTable().length())
			Constants.TSTRING -> ValueFactory.valueOf(value.checkLuaString().length())
			else -> throw ErrorFactory.argError(1, "table or string expected")
		}
	}
	
	
	fun next(args: Varargs): Varargs? {
		val table = args.checkValue(1)
		table.dynamicTable?.let {
			val key = args.checkValue(2)
			return it.next(table, key)
		}
		return null
	}
	
	fun inext(args: Varargs): Varargs? {
		// inext( table, [int-index] ) -> next-index, next-value
		val table = args.checkValue(1)
		table.dynamicTable?.let {
			val key = args.arg(2).checkInteger() + 1
			return it.next(table, key)
		}
		return null
	}
	
	
	fun pairs(args: Varargs): Varargs? {
		// pairs(t) -> iter-func, t, firstIndex
		val table = args.checkValue(1)
		table.dynamicTable?.let { dynamic ->
			val iterator = dynamic.pairs(table)
			var previous = Constants.NIL
			
			return ValueFactory.varargsOf(
				luaSimpleFunction { _, args ->
					// val table = args.checkValue(1)
					val index = args.arg(2)
					if(index == previous) {
						if(iterator.hasNext()) {
							val (key, value) = iterator.next()
							previous = key
							ValueFactory.varargsOf(key, value)
						} else {
							ValueFactory.varargsOf()
						}
					} else {
						// geneerally never happens, but just a fallback
						dynamic.next(table, index).also { previous = NeverEquals }
					}
				},
				table,
				Constants.NIL,
			)
		}
		return null
	}
	
	
	fun ipairs(args: Varargs): Varargs? {
		// ipairst) -> iter-func, t, firstIndex
		val table = args.checkValue(1)
		table.dynamicTable?.let { dynamic ->
			val iterator = dynamic.indexedPairs(table)
			var previous = Constants.NIL
			
			return ValueFactory.varargsOf(
				luaSimpleFunction { _, args ->
					val index = args.arg(2)
					val intIndex = index.checkInteger()
					if(index == previous) {
						if(iterator.hasNext()) {
							val value = iterator.next()
							val nextIndex = ValueFactory.valueOf(intIndex + 1)
							ValueFactory.varargsOf(nextIndex, value)
						} else {
							Constants.NONE
						}
					} else {
						previous = NeverEquals
						val value = dynamic.next(table, intIndex)
						if(value == null) {
							Constants.NONE
						} else {
							ValueFactory.varargsOf(ValueFactory.valueOf(intIndex + 1), value)
						}
					}
				},
				table,
				Constants.ZERO
			)
		}
		return null
	}
}

