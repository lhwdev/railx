package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.*
import org.objectweb.asm.Opcodes.*


/// Unary operator

context(CodeContext)
operator fun Value.unaryMinus(): Value =
	instructions.unaryOperation(INEG, toStackTop())


/// Binary operator

context(CodeContext)
operator fun Value.plus(operand: Value): Value {
	validateGroup(this, operand)
	return commBinaryOps(type.getOpcode(IADD), this, operand)
}

context(CodeContext)
operator fun Value.minus(operand: Value): Value {
	validateGroup(this, operand)
	return binaryOps(type.getOpcode(ISUB), this, operand, BinaryOperationKind.Group)
}

context(CodeContext)
operator fun Value.times(operand: Value): Value {
	validateGroup(this, operand)
	return commBinaryOps(type.getOpcode(IMUL), this, operand)
}

context(CodeContext)
operator fun Value.div(operand: Value): Value {
	validateGroup(this, operand)
	return binaryOps(type.getOpcode(IDIV), this, operand, BinaryOperationKind.Group)
}

context(CodeContext)
operator fun Value.rem(operand: Value): Value {
	validateGroup(this, operand)
	return binaryOps(type.getOpcode(IREM), this, operand, BinaryOperationKind.Group)
}


private fun validateShift(lhs: Value, rhs: Value) {
	require(lhs.type == Type.INT_TYPE || lhs.type == Type.LONG_TYPE) { "lhs.type must be int or long" }
	require(rhs.type == Type.INT_TYPE) { "rhs.type != int" }
}

context(CodeContext)
infix fun Value.shl(operand: Value): Value {
	validateShift(this, operand)
	return binaryOps(type.getOpcode(ISHL), this, operand, BinaryOperationKind.Shift)
}

context(CodeContext)
infix fun Value.shr(operand: Value): Value {
	validateShift(this, operand)
	return binaryOps(type.getOpcode(ISHR), this, operand, BinaryOperationKind.Shift)
}

context(CodeContext)
infix fun Value.ushr(operand: Value): Value {
	validateShift(this, operand)
	return binaryOps(type.getOpcode(IUSHR), this, operand, BinaryOperationKind.Shift)
}


private fun validateBitOp(lhs: Value, rhs: Value) {
	validateGroup(lhs, rhs)
	require(lhs.type == Type.INT_TYPE || lhs.type == Type.LONG_TYPE) { "type of operands must be int or long" }
}

context(CodeContext)
infix fun Value.bitOr(operand: Value): Value {
	validateBitOp(this, operand)
	return commBinaryOps(type.getOpcode(IOR), this, operand)
}

context(CodeContext)
infix fun Value.bitAnd(operand: Value): Value {
	validateBitOp(this, operand)
	return commBinaryOps(type.getOpcode(IAND), this, operand)
}

context(CodeContext)
infix fun Value.bitXor(operand: Value): Value {
	validateBitOp(this, operand)
	return commBinaryOps(type.getOpcode(IXOR), this, operand)
}


context(CodeContext)
operator fun Variable.plusAssign(operand: Int) {
	require(type == Type.INT_TYPE) { "this.type != int" }
	instructions.iinc(this, operand)
}
