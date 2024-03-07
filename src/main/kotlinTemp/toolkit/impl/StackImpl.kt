package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.StackFrame
import com.lhwdev.asm.toolkit.impl.tree.ExpressionNode
import org.objectweb.asm.Opcodes


class StackImpl(private val code: AsmCodeContext) {
	class Frame(val start: Int, val results: Int) : StackFrame
	
	
	val stack = mutableListOf<IndexedStackValue>()
	
	private var currentStackSize = 0
	
	var maxStackSize = 0
	
	
	val top: IndexedStackValue
		get() = stack.last()
	
	fun pop(): IndexedStackValue =
		stack.removeLast()
	
	fun push(node: ExpressionNode, parameters: Int): StackNodeValue {
		val value = StackNodeValue(index = stack.size.toShort(), node)
		pushImpl(value, parameters)
		return value
	}
	
	private fun pushImpl(value: IndexedStackValue, parameters: Int) {
		repeat(parameters) { stack.removeLast() }
		stack += value
		
		currentStackSize += value.type.size
		if(currentStackSize > maxStackSize) {
			maxStackSize = currentStackSize
		}
	}
	
	fun dup(): IndexedStackValue = StackDupValue(index = stack.size.toShort(), type = top.type)
		.also { pushImpl(it, parameters = 0) }
	
	fun commitPush() {
		val value = stack.removeLast()
		value.node.accept(code.output)
	}
	
	fun popDiscard() {
		val value = pop()
		code.output.visitInsn(if(value.type.size == 1) Opcodes.POP else Opcodes.POP2)
	}
}
