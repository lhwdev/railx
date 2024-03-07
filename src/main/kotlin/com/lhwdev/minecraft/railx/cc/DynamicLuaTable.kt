package com.lhwdev.minecraft.railx.cc

import org.squiddev.cobalt.Constants
import org.squiddev.cobalt.LuaValue
import org.squiddev.cobalt.ValueFactory
import org.squiddev.cobalt.Varargs


private val EndOfIterator = object : LuaValue(0) {}


interface DynamicLuaTable {
	val size: Int
	
	fun rawGet(key: LuaValue): LuaValue
	
	fun rawGet(key: Int): LuaValue =
		rawGet(ValueFactory.valueOf(key))
	
	fun pairs(table: LuaValue): Iterator<Pair<LuaValue, LuaValue>> = object : Iterator<Pair<LuaValue, LuaValue>> {
		private var index = Constants.NIL
		private var value = Constants.NIL
		
		private fun read() {
			val result = next(table, index)
			if(result.isNoneOrNil(1)) {
				index = EndOfIterator
			} else {
				index = result.checkValue(1)
				value = result.checkValue(2)
			}
		}
		
		init {
			read()
		}
		
		override fun hasNext(): Boolean = index != EndOfIterator
		
		override fun next(): Pair<LuaValue, LuaValue> =
			(index to value).also { read() }
	}
	
	fun indexedPairs(table: LuaValue): Iterator<LuaValue> = object : Iterator<LuaValue> {
		private var index = 0
		private var value = Constants.NIL
		
		private fun read() {
			val result = next(table, index)
			if(result != null) {
				index++
				value = result
			} else {
				index = -1
				value = EndOfIterator
			}
		}
		
		init {
			read()
		}
		
		override fun hasNext(): Boolean = index != -1
		
		override fun next(): LuaValue =
			value.also { read() }
	}
	
	fun next(table: LuaValue, index: LuaValue): Varargs
	
	fun next(table: LuaValue, index: Int): LuaValue? {
		val result = next(table, ValueFactory.valueOf(index))
		return if(result.isNoneOrNil(1)) {
			null
		} else {
			result.checkValue(2)
		}
	}
	
	
	// Write
	
	fun rawSet(key: LuaValue, value: LuaValue): Boolean
	
	fun trySet(key: LuaValue, value: LuaValue): Boolean =
		rawSet(key, value)
	
	fun rawSet(key: Int, value: LuaValue): Boolean =
		rawSet(ValueFactory.valueOf(key), value)
	
	// Write List
	
	fun remove(index: Int): LuaValue {
		return moveItems(initial = Constants.NIL, fromStart = index, fromEnd = size, step = -1)
	}
	
	fun insert(index: Int, value: LuaValue) {
		moveItems(initial = value, fromStart = index, fromEnd = size, step = 1)
	}
	
	fun duplicate(from: Int, to: Int, count: Int) {
		if(to >= from + count || to <= from) {
			for(i in 0..<count) rawSet(to + i, rawGet(from + i))
		} else {
			for(i in count - 1 downTo 0) rawSet(to + i, rawGet(from + i))
		}
	}
}

private fun DynamicLuaTable.moveItems(initial: LuaValue, fromStart: Int, fromEnd: Int, step: Int): LuaValue {
	var previous = initial
	for(fromIndex in fromStart..<fromEnd step step) {
		val next = rawGet(fromIndex)
		rawSet(fromIndex, previous)
		previous = next
	}
	return previous
}

