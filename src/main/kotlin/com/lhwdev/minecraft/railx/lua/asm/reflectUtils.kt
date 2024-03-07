package com.lhwdev.minecraft.railx.lua.asm

import com.lhwdev.minecraft.railx.utils.accHandle
import java.lang.reflect.Method
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KFunction


private object ReflectionUtilHandles {
	val KFunctionImpl = Class.forName("kotlin.reflect.jvm.internal.KFunctionImpl")
	val Caller = Class.forName("kotlin.reflect.jvm.internal.calls.Caller")
	
	val KFunctionImpl_defaultCaller = KFunctionImpl.getDeclaredMethod("getDefaultCaller").accHandle
	
	val Caller_member = Caller.getDeclaredMethod("getMember", Any::class.java).accHandle
}


private fun KFunction<*>.asKFunctionImpl() =
	ReflectionUtilHandles.KFunctionImpl.cast((this as? FunctionReference)?.compute() ?: this)

val KFunction<*>.javaDefaultMethod: Method?
	get() = asKFunctionImpl()?.let {
		ReflectionUtilHandles.Caller_member.invoke(ReflectionUtilHandles.KFunctionImpl_defaultCaller.invoke(it)) as Method?
	}
