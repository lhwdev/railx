package com.lhwdev.minecraft.railx.cc

import org.squiddev.cobalt.*
import java.util.*


internal fun toObjects(values: Varargs): Array<Any?> {
	val count = values.count()
	val objects = arrayOfNulls<Any>(count)
	for(i in 0 until count) objects[i] = toObject(values.arg(i + 1), null)
	return objects
}

internal fun toObject(value: LuaValue, objects: IdentityHashMap<LuaValue?, Any?>?): Any? {
	return when(value.type()) {
		Constants.TINT, Constants.TNUMBER -> value.toDouble()
		Constants.TBOOLEAN -> value.toBoolean()
		Constants.TSTRING -> value.toString()
		Constants.TTABLE -> {
			value as LuaTable
			
			// Table:
			// Start remembering stuff
			val cache = objects ?: IdentityHashMap(1)
			cache[value]?.let { return it }
			
			val table = HashMap<Any?, Any?>()
			cache[value] = table
			
			// Convert all keys
			var k = Constants.NIL
			while(true) {
				val keyValue = try {
					value.next(k)
				} catch(luaError: LuaError) {
					break
				}
				k = keyValue.first()
				if(k.isNil) break
				val v = keyValue.arg(2)
				val keyObject = toObject(k, cache)
				val valueObject = toObject(v, cache)
				if(keyObject != null && valueObject != null) {
					table[keyObject] = valueObject
				}
			}
			table
		}
		
		else -> null
	}
}
