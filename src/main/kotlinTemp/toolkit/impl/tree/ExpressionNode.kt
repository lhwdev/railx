package com.lhwdev.asm.toolkit.impl.tree

import com.lhwdev.asm.toolkit.Type


interface ExpressionNode : InstructionNode {
	val type: Type
}
