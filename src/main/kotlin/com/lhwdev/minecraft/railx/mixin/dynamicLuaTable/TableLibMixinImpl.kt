@file:Mixin(TableLib::class)
@file:JvmName("TableLibMixin")
@file:Suppress("NonJavaMixin", "SpellCheckingInspection")

package com.lhwdev.minecraft.railx.mixin.dynamicLuaTable

import com.lhwdev.minecraft.railx.cc.dynamicTable
import com.lhwdev.minecraft.railx.utils.asIterable
import org.spongepowered.asm.mixin.Mixin
import org.squiddev.cobalt.*
import org.squiddev.cobalt.lib.TableLib


object TableLibMixinImpl {
	fun checkTableLike(state: LuaState, args: Varargs, index: Int, flags: Int): LuaValue? {
		val value = args.arg(index)
		if(value.dynamicTable != null) return value
		if(value !is LuaTable) {
			val metatable = value.getMetatable(state)
			if(metatable != null // For each operation, check (flag => metaTag!=nil).
				&& (flags and /* TableLib.TABLE_LEN */ 1 shl 2 == 0 || !metatable.rawget(CachedMetamethod.LEN).isNil)
				&& (flags and /* TableLib.TABLE_READ */ 1 == 0 || metatable.rawget(CachedMetamethod.INDEX).isNil)
				&& (flags and /* TableLib.TABLE_WRITE */ 1 shl 1 == 0 || metatable.rawget(CachedMetamethod.NEWINDEX).isNil)
			) {
				return value
			}
		}
		return value.checkTable()
	}
	
	fun getn(arg: LuaValue): LuaValue {
		// getn(table) -> number
		arg.dynamicTable?.let { return ValueFactory.valueOf(it.size) }
		return ValueFactory.valueOf(arg.checkTable().length())
	}
	
	fun maxn(arg: LuaValue): LuaValue? {
		// maxn(table) -> number
		arg.dynamicTable?.let {
			val max = it.pairs(arg).asIterable().maxOf { (_, value) ->
				if(value.isNumber) {
					value.toDouble()
				} else {
					Double.NEGATIVE_INFINITY
				}
			}
			return ValueFactory.valueOf(max)
		}
		
		return null
	}
	
	fun remove(args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let { dynamic ->
			val position = args.arg(2).optInteger(dynamic.size)
			return dynamic.remove(position - 1)
		}
		return null
	}
	
	
	fun insert(args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let { dynamic ->
			val position: Int
			val value: LuaValue
			
			when(args.count()) {
				2 -> {
					position = dynamic.size
					value = args.arg(2)
				}
				
				3 -> {
					position = args.arg(2).checkInteger()
					value = args.arg(3)
				}
				
				else -> throw LuaError("wrong number of argumments to \"insert\"")
			}
			
			dynamic.insert(position - 1, value)
			return Constants.NONE
		}
		return null
	}
	
	fun move(args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let {
			val from = args.arg(2).checkInteger()
			val end = args.arg(3).checkInteger()
			val to = args.arg(4).checkInteger()
			
			val count = end - from + 1
			
			// Some additional overflow sanity checks.
			if(from < 1 && end >= Int.MAX_VALUE + from) {
				throw ErrorFactory.argError(3, "too many elements to move")
			}
			if(to > Int.MAX_VALUE - count + 1) {
				throw ErrorFactory.argError(4, "destination wrap around")
			}
			
			it.duplicate(from, to, count)
			return table
		}
		return null
	}
	
	fun foreach(state: LuaState, args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let {
			val function = args.arg(2).checkFunction()
			for((key, value) in it.pairs(table)) {
				val result = OperationHelper.call(state, function, key, value)
				if(!result.isNil) return result
			}
			Constants.NIL
		}
		return null
	}
	
	fun foreachi(state: LuaState, args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let {
			val function = args.arg(2).checkFunction()
			for((key, value) in it.indexedPairs(table).withIndex()) {
				val result = OperationHelper.call(state, function, ValueFactory.valueOf(key), value)
				if(!result.isNil) {
					return result
				}
			}
			return Constants.NIL
		}
		return null
	}
	
	fun unpack(args: Varargs): Varargs? {
		val table = args.arg(1)
		table.dynamicTable?.let { dynamic ->
			val array = arrayOfNulls<LuaValue>(dynamic.size)
			for((index, item) in dynamic.indexedPairs(table).withIndex()) {
				array[index] = item
			}
			@Suppress("UNCHECKED_CAST")
			return ValueFactory.varargsOf(array as Array<LuaValue>, Constants.NONE)
		}
		return null
	}
}
