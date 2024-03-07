package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.descriptor.MethodDescriptor
import com.lhwdev.asm.toolkit.descriptor.descriptor
import com.lhwdev.asm.toolkit.stackFrame
import javax.annotation.CheckReturnValue


context(CodeContext)
private fun validateArguments(arguments: Array<out Value>) {
	for(index in 0..<arguments.lastIndex - 1) {
		val previous = arguments[index] as StackValue
		val next = arguments[index + 1] as StackValue
		require(next.isAfter(previous)) {
			"order of arguments in stack is not sequential; arguments[${index + 1}] = $next."
		}
	}
	(arguments.last() as StackValue).ensureTop()
}

context(CodeContext)
fun <R> withArguments(vararg arguments: Value, block: () -> R): R {
	val stackCount = arguments.count { it is StackValue }
	return when(stackCount) {
		0 -> stackFrame {
			arguments.forEach { it.push() }
			block()
		}
		
		arguments.size -> stackFrame(parameters = arguments.size, results = 1) {
			validateArguments(arguments)
			block()
		}
		
		else -> error("mixed form of StackValue and other value is not supported")
	}
}


context(CodeContext)
inline fun <reified T> newInstance(): StackValue =
	newInstance(descriptor(T::class.java))


context(CodeContext)
fun newInstance(constructor: MethodDescriptor, vararg arguments: Value): StackValue {
	val instance = newInstance(constructor.owner)
	withArguments(instance.dup(), *arguments) {
		constructor(instance, *arguments)
	}
	return instance
}


context(CodeContext)
operator fun MethodDescriptor.invoke(instance: Value, vararg arguments: Value): StackValue =
	withArguments(instance, *arguments) { invoke(this) }


context(CodeContext)
@CheckReturnValue
infix fun Value.cast(type: Type): StackValue =
	instructions.checkCast(toStackTop(), type)

context(CodeContext)
infix fun Value.isInstance(type: Type): StackValue =
	instructions.instanceOf(toStackTop(), type)

