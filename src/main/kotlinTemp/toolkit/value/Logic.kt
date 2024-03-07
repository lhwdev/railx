package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.BinaryOperationKind
import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.InstructionType
import com.lhwdev.asm.toolkit.ensureStackTop
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*


interface LogicBinaryOperator {
	val kind: BinaryOperationKind
		get() = BinaryOperationKind.Group
	
	context(CodeContext)
	fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label)
	
	context(CodeContext)
	fun jumpIfOr(lhs: StackValue, rhs: StackValue, ifTrueTo: Label, ifFalseTo: Label) {
		jumpIf(lhs, rhs, ifTrueTo)
		jumpTo(ifFalseTo)
	}
	
	fun inverse(): LogicBinaryOperator
	
	fun commutate(): LogicBinaryOperator
}


context(CodeContext)
internal fun logicBinaryOps(
	operator: LogicBinaryOperator,
	lhs: Value,
	rhs: Value,
): StackValue = if(lhs is StackValue) {
	if(rhs is StackValue) {
		// this limit can be lifted by swap(), but this is to ensure consistency in code.
		ensureStackTop(lhs, rhs)
		
		instructions.binaryOperation(operator, lhs, rhs)
	} else {
		lhs.ensureTop(name = "lhs")
		val rhsOnStack = rhs.push()
		
		instructions.binaryOperation(operator, lhs, rhsOnStack)
	}
} else {
	if(rhs is StackValue) {
		instructions.binaryOperation(operator.commutate(), rhs, lhs.toStackTop())
	} else {
		instructions.binaryOperation(operator, lhs.push(), rhs.push())
	}
}

enum class IntComparisonOperator(private val opcode: Int) : LogicBinaryOperator {
	Eq(IF_ICMPEQ) {
		override val kind: BinaryOperationKind
			get() = BinaryOperationKind.CommutativeGroup
		
		override fun inverse() = Ne
		override fun commutate() = this
	},
	Ne(IF_ICMPNE) {
		override val kind: BinaryOperationKind
			get() = BinaryOperationKind.CommutativeGroup
		
		override fun inverse() = Eq
		override fun commutate() = this
	},
	Lt(IF_ICMPLT) {
		override fun inverse() = GtEq
		override fun commutate() = Gt
	},
	Gt(IF_ICMPGT) {
		override fun inverse() = LtEq
		override fun commutate() = Lt
	},
	LtEq(IF_ICMPLE) {
		override fun inverse() = Gt
		override fun commutate() = Gt
	},
	GtEq(IF_ICMPGE) {
		override fun inverse() = LtEq
		override fun commutate() = Lt
	};
	
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		ensureStackTop(lhs, rhs)
		return instructions.jumpIf(opcode, to)
	}
}

private abstract class CJOperator(private val ifJump: Int, private val compare: Int) : LogicBinaryOperator {
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		instructions.binaryOperation(compare, lhs, rhs, BinaryOperationKind.Group).ensureTop()
		// stack: [resultOfCompareTo: int]
		instructions.jumpIf(opcode = ifJump, to)
	}
}

object LongComparisonOperator {
	val Eq: LogicBinaryOperator = object : CJOperator(IFEQ, LCMP) {
		override val kind: BinaryOperationKind get() = BinaryOperationKind.CommutativeGroup
		override fun inverse() = Ne
		override fun commutate() = this
	}
	val Ne: LogicBinaryOperator = object : CJOperator(IFNE, LCMP) {
		override val kind: BinaryOperationKind get() = BinaryOperationKind.CommutativeGroup
		override fun inverse() = Eq
		override fun commutate() = this
	}
	val Gt: LogicBinaryOperator = object : CJOperator(IFGT, LCMP) {
		override fun inverse() = LtEq
		override fun commutate() = Lt
	}
	val GtEq: LogicBinaryOperator = object : CJOperator(IFGT, LCMP) {
		override fun inverse() = Lt
		override fun commutate() = LtEq
	}
	val Lt: LogicBinaryOperator = object : CJOperator(IFGT, LCMP) {
		override fun inverse() = GtEq
		override fun commutate() = Gt
	}
	val LtEq: LogicBinaryOperator = object : CJOperator(IFGT, LCMP) {
		override fun inverse() = Gt
		override fun commutate() = GtEq
	}
}

class DecimalComparisonOperators(private val compareL: Int, private val compareG: Int) {
	val Eq: LogicBinaryOperator = object : CJOperator(IFEQ, compareL) {
		override val kind: BinaryOperationKind get() = BinaryOperationKind.CommutativeGroup
		override fun inverse() = NNe
		override fun commutate() = this
	}
	val Ne: LogicBinaryOperator = object : CJOperator(IFEQ, compareL) {
		override val kind: BinaryOperationKind get() = BinaryOperationKind.CommutativeGroup
		override fun inverse() = NEq
		override fun commutate() = this
	}
	val Gt: LogicBinaryOperator = object : CJOperator(IFGT, compareL) {
		override fun inverse() = NLtEq
		override fun commutate() = Lt
	}
	val GtEq: LogicBinaryOperator = object : CJOperator(IFGE, compareL) {
		override fun inverse() = NLtEq
		override fun commutate() = LtEq
	}
	val Lt: LogicBinaryOperator = object : CJOperator(IFLT, compareG) {
		override fun inverse() = NGtEq
		override fun commutate() = Gt
	}
	val LtEq: LogicBinaryOperator = object : CJOperator(IFLE, compareG) {
		override fun inverse() = NGt
		override fun commutate() = GtEq
	}
	
	/// NaN-inversed
	val NEq: LogicBinaryOperator = object : CJOperator(IFEQ, compareL) {
		override fun inverse() = Ne
		override fun commutate() = this
	}
	val NNe: LogicBinaryOperator = object : CJOperator(IFEQ, compareL) {
		override fun inverse() = Eq
		override fun commutate() = this
	}
	val NGt: LogicBinaryOperator = object : CJOperator(IFGT, compareG) {
		override fun inverse() = LtEq
		override fun commutate() = NLt
	}
	val NGtEq: LogicBinaryOperator = object : CJOperator(IFGE, compareG) {
		override fun inverse() = LtEq
		override fun commutate() = NLtEq
	}
	val NLt: LogicBinaryOperator = object : CJOperator(IFLT, compareL) {
		override fun inverse() = GtEq
		override fun commutate() = NGt
	}
	val NLtEq: LogicBinaryOperator = object : CJOperator(IFLE, compareL) {
		override fun inverse() = Gt
		override fun commutate() = NGtEq
	}
}

val FloatComparisonOperator = DecimalComparisonOperators(FCMPL, FCMPG)

val DoubleComparisonOperator = DecimalComparisonOperators(DCMPL, DCMPG)


context(CodeContext)
infix fun Value.gt(other: Value): StackValue {
	validateGroup(this, other)
	return when(type.instructionType) {
		InstructionType.Int -> logicBinaryOps(IntComparisonOperator.Gt, this, other)
		InstructionType.Long -> logicBinaryOps(LongComparisonOperator.Gt, this, other)
		InstructionType.Float -> logicBinaryOps(FloatComparisonOperator.Gt, this, other)
		InstructionType.Double -> logicBinaryOps(DoubleComparisonOperator.Gt, this, other)
		InstructionType.Reference, InstructionType.Void -> error("unexpected reference type")
	}
}

context(CodeContext)
infix fun Value.gtEq(other: Value): StackValue {
	validateGroup(this, other)
	return when(type.instructionType) {
		InstructionType.Int -> logicBinaryOps(IntComparisonOperator.GtEq, this, other)
		InstructionType.Long -> logicBinaryOps(LongComparisonOperator.GtEq, this, other)
		InstructionType.Float -> logicBinaryOps(FloatComparisonOperator.GtEq, this, other)
		InstructionType.Double -> logicBinaryOps(DoubleComparisonOperator.GtEq, this, other)
		InstructionType.Reference, InstructionType.Void -> error("unexpected reference type")
	}
}

context(CodeContext)
infix fun Value.lt(other: Value): StackValue {
	validateGroup(this, other)
	return when(type.instructionType) {
		InstructionType.Int -> logicBinaryOps(IntComparisonOperator.Lt, this, other)
		InstructionType.Long -> logicBinaryOps(LongComparisonOperator.Lt, this, other)
		InstructionType.Float -> logicBinaryOps(FloatComparisonOperator.Lt, this, other)
		InstructionType.Double -> logicBinaryOps(DoubleComparisonOperator.Lt, this, other)
		InstructionType.Reference, InstructionType.Void -> error("unexpected reference type")
	}
}

context(CodeContext)
infix fun Value.ltEq(other: Value): StackValue {
	validateGroup(this, other)
	return when(type.instructionType) {
		InstructionType.Int -> logicBinaryOps(IntComparisonOperator.LtEq, this, other)
		InstructionType.Long -> logicBinaryOps(LongComparisonOperator.LtEq, this, other)
		InstructionType.Float -> logicBinaryOps(FloatComparisonOperator.LtEq, this, other)
		InstructionType.Double -> logicBinaryOps(DoubleComparisonOperator.LtEq, this, other)
		InstructionType.Reference, InstructionType.Void -> error("unexpected reference type")
	}
}


context(CodeContext)
operator fun Value.not(): StackValue =
	instructions.not(toStackTop())


private object OrOperator : LogicBinaryOperator {
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		instructions.jumpIf(lhs, to)
		instructions.jumpIf(rhs, to)
	}
	
	override fun inverse(): LogicBinaryOperator = NotThenAndOperator
	override fun commutate(): LogicBinaryOperator = this
}

private object NotThenOrOperator : LogicBinaryOperator {
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		OrOperator.jumpIf(!lhs, !rhs, to)
	}
	
	override fun inverse(): LogicBinaryOperator = AndOperator
	override fun commutate(): LogicBinaryOperator = this
}

private object AndOperator : LogicBinaryOperator {
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		blockScope { scope ->
			instructions.jumpIf(!lhs, scope.breakLabel)
			instructions.jumpIf(!rhs, scope.breakLabel)
			jumpTo(to)
		}
	}
	
	context(CodeContext)
	override fun jumpIfOr(lhs: StackValue, rhs: StackValue, ifTrueTo: Label, ifFalseTo: Label) {
		instructions.jumpIf(!lhs, ifFalseTo)
		instructions.jumpIf(!rhs, ifFalseTo)
		jumpTo(ifTrueTo)
	}
	
	override fun inverse(): LogicBinaryOperator = NotThenOrOperator
	override fun commutate(): LogicBinaryOperator = this
}

private object NotThenAndOperator : LogicBinaryOperator {
	context(CodeContext)
	override fun jumpIf(lhs: StackValue, rhs: StackValue, to: Label) {
		AndOperator.jumpIf(!lhs, !rhs, to)
	}
	
	context(CodeContext)
	override fun jumpIfOr(lhs: StackValue, rhs: StackValue, ifTrueTo: Label, ifFalseTo: Label) {
		AndOperator.jumpIfOr(!lhs, !rhs, ifTrueTo, ifFalseTo)
	}
	
	override fun inverse(): LogicBinaryOperator = OrOperator
	override fun commutate(): LogicBinaryOperator = this
}

context(CodeContext)
infix fun Value.logicOr(other: Value): StackValue =
	logicBinaryOps(OrOperator, this, other)

context(CodeContext)
infix fun Value.logicAnd(other: Value): StackValue =
	logicBinaryOps(AndOperator, this, other)
