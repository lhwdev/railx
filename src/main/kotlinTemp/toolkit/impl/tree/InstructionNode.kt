package com.lhwdev.asm.toolkit.impl.tree

import org.objectweb.asm.MethodVisitor


interface InstructionNode {
	fun accept(output: MethodVisitor)
}
