package com.lhwdev.asm.toolkit

import kotlin.reflect.KClass


@ContextMarker
interface MethodContext {
	fun define(access: Int, name: String)
	
	fun self(): SelfVariable
	
	fun parameter(klass: Class<*>): Variable
	
	fun parameter(type: Type): Variable
	
	fun returnValue(klass: Class<*>)
	
	fun returnValue(type: Type)
	
	
	fun body(): CodeContext
}


fun MethodContext.parameter(klass: KClass<*>): Variable =
	parameter(klass.java)

inline fun <reified T> MethodContext.parameter(): Variable =
	parameter(T::class.java)

fun MethodContext.returnValue(klass: KClass<*>) =
	returnValue(klass.java)

inline fun <reified T> MethodContext.returnValue() =
	returnValue(T::class.java)

inline fun MethodContext.body(block: context(CodeContext) () -> Unit) {
	block(body())
}
