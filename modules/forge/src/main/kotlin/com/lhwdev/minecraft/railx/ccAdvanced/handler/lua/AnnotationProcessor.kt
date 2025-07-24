package com.lhwdev.minecraft.railx.ccAdvanced.handler.lua

import com.lhwdev.minecraft.railx.ccAdvanced.handler.ComputerApi
import org.squiddev.cobalt.LuaState
import org.squiddev.cobalt.Varargs
import org.squiddev.cobalt.function.LuaFunction
import org.squiddev.cobalt.function.VarArgFunction
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method


internal class ProcessContext() {
	companion object {
		fun from(annotation: ComputerApi) = ProcessContext(
		
		)
	}
}


object AnnotationProcessor {
	private fun convert(method: MethodHandle, context: ProcessContext): LuaFunction {
	
	}
}
