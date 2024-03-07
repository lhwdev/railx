package com.lhwdev.minecraft.railx.api.lua

import java.nio.ByteBuffer


@Suppress("unused")
interface LuaArguments {
	val size: Int
	
	
	fun getNullable(index: Int): Boolean = true
	fun getDefault(index: Int): Boolean = true
	fun getDefaultNullable(index: Int): Boolean = true
	
	
	fun isNull(index: Int): Boolean = get(index).isNull()
	
	fun isDefault(index: Int): Boolean = index < size
	
	
	fun get(index: Int): LuaArgument
	
	fun getAny(index: Int): Any? = get(index).getAny()
	
	
	fun getInt(index: Int): Int = get(index).getInt()
	
	fun getBoolean(index: Int): Boolean = get(index).getBoolean()
	
	fun getDouble(index: Int): Double = get(index).getDouble()
	
	fun getLong(index: Int): Long = get(index).getLong()
	
	
	fun getString(index: Int): String = get(index).getString()
	
	fun getBytes(index: Int): ByteBuffer = get(index).getBytes()
	
	fun <T : Enum<*>> getEnum(index: Int, klass: Class<out T>): T = get(index).getEnum(klass)
	
	fun <T> getList(index: Int): LuaListArgument<T> = get(index).getList()
	
	fun getMap(index: Int): LuaMapArgument<*, *> = get(index).getMap()
	
	fun <T> deserialize(index: Int, klass: Class<out T>): T = get(index).deserialize(klass)
}


interface LuaArgument {
	fun isNull(): Boolean
	
	fun getAny(): Any?
	
	fun getInt(): Int
	fun getBoolean(): Boolean
	fun getDouble(): Double
	fun getLong(): Long
	fun getString(): String
	fun getBytes(): ByteBuffer
	fun <T : Enum<*>> getEnum(klass: Class<out T>): T
	
	fun <T> getList(): LuaListArgument<T>
	fun getMap(): LuaMapArgument<*, *>
	
	fun <T> deserialize(klass: Class<out T>): T
}

interface LuaListArgument<T> : LuaArgument {
	fun hasNext(): Boolean
	
	fun add(value: T)
	
	fun build(): List<T>
}

interface LuaMapArgument<K, V> : LuaArgument {
	fun hasNext(): Boolean
	fun add(key: K, value: V)
	
	fun build(): Map<K, V>
}
