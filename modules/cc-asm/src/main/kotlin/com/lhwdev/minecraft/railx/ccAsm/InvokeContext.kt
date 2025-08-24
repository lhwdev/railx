package com.lhwdev.minecraft.railx.ccAsm

import org.squiddev.cobalt.Varargs


abstract class ParseContext {
	abstract fun isNull(): Boolean
	
	abstract fun boolean(): Boolean
	abstract fun byte(): Byte
	abstract fun short(): Short
	abstract fun int(): Int
	abstract fun long(): Long
	abstract fun float(): Float
	abstract fun double(): Double
	abstract fun char(): Char
	abstract fun string(): String
	
	abstract fun <T : Any> apiInterface(declaration: ComputerApiItem.Object<T>): T
	
	abstract fun list(): ParseList
	
	abstract class ParseList : ParseContext() {
		abstract fun hasNext(): Boolean
	}
}

abstract class InvokeContext : ParseContext() {
	abstract fun argumentsCount(count: Int)
	abstract fun argumentsCount(minCount: Int, maxCount: Int)
	
	abstract fun skipCurrent(): Varargs?
	
	abstract fun hasOptional(maybeNull: Boolean): Boolean
	
	abstract fun wrapReturn(value: Any?): Varargs
	abstract fun wrapReturnVoid(): Varargs
}
