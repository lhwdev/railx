package com.lhwdev.asm.toolkit

import com.lhwdev.asm.toolkit.descriptor.ClassDescriptor
import com.lhwdev.asm.toolkit.descriptor.MethodDescriptor
import com.lhwdev.asm.toolkit.value.*
import org.objectweb.asm.Label
import java.lang.reflect.Method
import javax.annotation.CheckReturnValue


interface StackFrame


@ContextMarker
interface CodeContext {
	interface Scope
	
	interface FlowScope : Scope {
		val continueLabel: Label
		
		val breakLabel: Label
	}
	
	
	/// Scopes
	
	fun pushScope(): Scope
	
	fun popScope(previous: Scope)
	
	fun variable(type: Type, initialValue: StackValue): Variable
	
	
	/// Stacks
	
	val stackTop: StackValue
	
	fun pushStackFrame(parameters: Int, results: Int): StackFrame
	
	fun popStackFrame(previous: StackFrame)
	
	
	val loadContext: LoadContext
	
	fun commitPush(value: StackValue)
	
	fun push(constant: Nothing?): StackValue
	
	fun push(constant: Boolean): StackValue =
		push(if(constant) 1 else 0)
	
	fun push(constant: Int): StackValue
	
	fun push(constant: Long): StackValue
	
	fun push(constant: Float): StackValue
	
	fun push(constant: Double): StackValue
	
	fun push(constant: String): StackValue
	
	fun push(constant: Type): StackValue
	
	fun push(from: Value): StackValue =
		from.push()
	
	fun dup(target: StackValue): StackValue
	
	
	val storeContext: StoreContext
	
	// Note: @CheckReturnValue is not warned by IntelliJ Kotlin so far
	@CheckReturnValue
	fun pop(): StackValue
	
	fun popDiscard()
	
	
	/// Ops
	
	val instructions: CodeInstructions
	
	
	fun newInstance(klass: ClassDescriptor): StackValue
	
	fun invoke(descriptor: MethodDescriptor): StackValue
	
	fun invoke(method: Method): StackValue =
		invoke(com.lhwdev.asm.toolkit.descriptor.descriptor(method))
	
	
	/// Ops: flow
	
	val currentFlowScope: FlowScope
	
	fun pushFlowScope(allowContinue: Boolean, allowBreak: Boolean): FlowScope
	
	fun popFlowScope(previous: FlowScope)
	
	fun jumpTo(label: Label)
	
	
	fun returnVoid()
	
	fun returnValue(value: StackValue)
	
	fun throwValue(value: StackValue)
}


context(CodeContext)
inline fun <R> stackFrame(parameters: Int = -1, results: Int = -1, block: context(CodeContext) () -> R): R {
	val previous = pushStackFrame(parameters, results)
	return try {
		block(this@CodeContext)
	} finally {
		popStackFrame(previous)
	}
}

context(CodeContext)
inline fun <R> scope(block: context(CodeContext) () -> R): R {
	val previous = pushScope()
	return try {
		block(this@CodeContext)
	} finally {
		popScope(previous)
	}
}

context(CodeContext)
fun ensureStackTop(value: StackValue) {
	value.ensureTop()
}

context(CodeContext)
fun ensureStackTop(vararg values: StackValue) {
	if(values.isEmpty()) return
	for(index in 0..<values.lastIndex) {
		require(values[index + 1].isAfter(values[index]))
	}
	ensureStackTop(values.last())
}
