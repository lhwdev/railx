package com.lhwdev.minecraft.railx.ccAsm.lua

import com.lhwdev.minecraft.railx.ccAsm.ComputerApiItem
import com.lhwdev.minecraft.railx.ccAsm.InvokeContext
import com.lhwdev.minecraft.railx.ccAsm.ParseContext
import org.squiddev.cobalt.*


class LuaInvokeContext : InvokeContext() {
	override fun argumentsCount(count: Int) {
		TODO("Not yet implemented")
	}
	
	override fun argumentsCount(minCount: Int, maxCount: Int) {
		TODO("Not yet implemented")
	}
	
	override fun skipCurrent(): Varargs? {
		TODO("Not yet implemented")
	}
	
	override fun hasOptional(maybeNull: Boolean): Boolean {
		TODO("Not yet implemented")
	}
	
	override fun wrapReturn(value: Any?): Varargs {
		TODO("Not yet implemented")
	}
	
	override fun wrapReturnVoid(): Varargs =
		Constants.NONE
	
	override fun isNull(): Boolean {
		TODO("Not yet implemented")
	}
	
	override fun boolean(): Boolean {
		TODO("Not yet implemented")
	}
	
	override fun byte(): Byte {
		TODO("Not yet implemented")
	}
	
	override fun short(): Short {
		TODO("Not yet implemented")
	}
	
	override fun int(): Int {
		TODO("Not yet implemented")
	}
	
	override fun long(): Long {
		TODO("Not yet implemented")
	}
	
	override fun float(): Float {
		TODO("Not yet implemented")
	}
	
	override fun double(): Double {
		TODO("Not yet implemented")
	}
	
	override fun char(): Char {
		TODO("Not yet implemented")
	}
	
	override fun string(): String {
		TODO("Not yet implemented")
	}
	
	override fun <T : Any> apiInterface(declaration: ComputerApiItem.Object<T>): T {
		TODO("Not yet implemented")
	}
	
	override fun list(): ParseList {
		TODO("Not yet implemented")
	}
}

private class LuaListContext(private val state: LuaState, private val list: LuaTable) : ParseContext.ParseList() {
	private var index = 1 // oh no why lua is not zero-based
	private val length = list.length()
	private var cache: LuaValue? = null
	
	private val current
		get() = cache ?: OperationHelper.getTable(state, list, index).also { cache = it }
	
	private fun next() = current.also {
		index++
		cache = null
	}
	
	override fun hasNext(): Boolean = index < length
	
	override fun isNull(): Boolean {
		TODO("Not yet implemented")
	}
	
	override fun boolean(): Boolean {
		TODO("Not yet implemented")
	}
	
	override fun byte(): Byte {
		TODO("Not yet implemented")
	}
	
	override fun short(): Short {
		TODO("Not yet implemented")
	}
	
	override fun int(): Int {
		TODO("Not yet implemented")
	}
	
	override fun long(): Long {
		TODO("Not yet implemented")
	}
	
	override fun float(): Float {
		TODO("Not yet implemented")
	}
	
	override fun double(): Double {
		TODO("Not yet implemented")
	}
	
	override fun char(): Char {
		TODO("Not yet implemented")
	}
	
	override fun string(): String {
		TODO("Not yet implemented")
	}
	
	override fun <T : Any> apiInterface(declaration: ComputerApiItem.Object<T>): T {
		TODO("Not yet implemented")
	}
	
	override fun list(): ParseList {
		TODO("Not yet implemented")
	}
}
