@file:Suppress("HasPlatformType")

package com.lhwdev.minecraft.railx.ccAdvanced.handler.lua

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.reflect.KFunction


private object ReflectionInternal {
	val root = "kotlin.reflect.jvm.internal"
	val lookup = MethodHandles.lookup() // internal class is effectively public in jvm
	
	val KCallableImpl = Class.forName("$root.KCallableImpl")
	val Caller = Class.forName("$root.utils.Caller")
	
	val asKCallableImpl = lookup.findStatic(
		Class.forName("$root.UtilKt"),
		"asKCallableImpl",
		MethodType.methodType(KCallableImpl, Any::class.java),
	)
	
	val KCallableImpl_defaultCaller = lookup.findVirtual(
		KCallableImpl,
		"getDefaultCaller",
		MethodType.methodType(Caller, KCallableImpl),
	)
	
	val Caller_member = lookup.findVirtual(
		Caller,
		"getMember",
		MethodType.methodType(Member::class.java, Caller),
	)
}

internal val <R> KFunction<R>.defaultJavaMethod: Method?
	get() = with(ReflectionInternal) {
		val kCallableImpl = asKCallableImpl.invoke(this) ?: return null
		val defaultCaller = KCallableImpl_defaultCaller.invoke(kCallableImpl) ?: return null
		Caller_member.invoke(defaultCaller) as? Method
	}


/*
// Alternative reflection alternative


*/
