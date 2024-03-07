package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.impl.tree.ExpressionNode
import com.lhwdev.asm.toolkit.value.StackValue


interface IndexedStackValue : StackValue {
	val index: Short
	
	override fun isAfter(previous: StackValue): Boolean =
		previous is IndexedStackValue && index.toInt() == previous.index + 1
}


class StackNodeValue(
	/**
	 * Logical index for stack value, regardless of `type.size`.
	 */
	override val index: Short,
	
	val node: ExpressionNode,
) : IndexedStackValue {
	override val type: Type
		get() = node.type
}

class StackDupValue(override val index: Short, override val type: Type) : IndexedStackValue
