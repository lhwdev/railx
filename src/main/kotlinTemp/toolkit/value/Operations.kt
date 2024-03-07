package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.BinaryOperationKind
import com.lhwdev.asm.toolkit.CodeContext


internal fun validateGroup(lhs: Value, rhs: Value) {
	require(lhs.type == rhs.type) { "lhs.type != rhs.type" }
}

context(CodeContext)
internal fun binaryOps(
	opcode: Int,
	lhs: Value,
	rhs: Value,
	kind: BinaryOperationKind = BinaryOperationKind.Group,
): StackValue = if(lhs is StackValue) {
	if(rhs is StackValue) {
		// this limit can be lifted by swap(), but this is to ensure consistency in code.
		require(rhs.isAfter(lhs)) { "rhs should be after lhs" }
		rhs.ensureTop(name = "rhs")
		
		instructions.binaryOperation(opcode, lhs, rhs, kind)
	} else {
		val rhsOnStack = rhs.push()
		lhs.ensureTop(name = "rhs")
		
		instructions.binaryOperation(opcode, lhs, rhsOnStack, kind)
	}
} else {
	if(rhs is StackValue) {
		println("NOTE: binaryOps called with (StackValue, StoredValue)")
		val lhsOnStack = lhs.push()
		val (a, b) = instructions.swap(rhs, lhsOnStack)
		instructions.binaryOperation(opcode, a, b, kind)
	} else {
		instructions.binaryOperation(opcode, lhs.push(), rhs.push(), kind)
	}
}

context(CodeContext)
internal fun commBinaryOps(opcode: Int, lhs: Value, rhs: Value): Value {
	val lhsStack = lhs.toStack()
	val rhsStack = rhs.toStack()
	
	when {
		rhsStack.isAfter(lhsStack) -> rhsStack.ensureTop()
		lhsStack.isAfter(rhsStack) -> lhsStack.ensureTop()
		else -> error("lhs and rhs must be adjacent")
	}
	
	return instructions.binaryOperation(opcode, lhsStack, rhsStack, BinaryOperationKind.CommutativeGroup)
}
