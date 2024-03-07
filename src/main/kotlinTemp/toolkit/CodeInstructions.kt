package com.lhwdev.asm.toolkit

import com.lhwdev.asm.toolkit.value.LogicBinaryOperator
import com.lhwdev.asm.toolkit.value.StackValue
import org.objectweb.asm.Label


enum class BinaryOperationKind {
	CommutativeGroup {
		override fun getResultType(lhs: Type, rhs: Type): Type {
			if(lhs != rhs) error("lhs.type($lhs) != rhs.type($rhs)")
			return lhs
		}
	},
	Group {
		override fun getResultType(lhs: Type, rhs: Type): Type {
			if(lhs != rhs) error("lhs.type($lhs) != rhs.type($rhs)")
			return lhs
		}
	},
	Shift {
		override fun getResultType(lhs: Type, rhs: Type): Type = lhs
	};
	
	abstract fun getResultType(lhs: Type, rhs: Type): Type
}


interface CodeInstructions {
	fun unaryOperation(opcode: Int, operand: StackValue): StackValue
	
	fun binaryOperation(opcode: Int, lhs: StackValue, rhs: StackValue, kind: BinaryOperationKind): StackValue
	
	fun binaryOperation(operator: LogicBinaryOperator, lhs: StackValue, rhs: StackValue): StackValue
	
	fun iinc(variable: Variable, operand: Int)
	
	fun conversion(opcode: Int, operand: StackValue, resultType: Type): StackValue
	
	fun swap(lhs: StackValue, rhs: StackValue): Pair<StackValue, StackValue>
	
	
	fun checkCast(value: StackValue, type: Type): StackValue
	
	fun instanceOf(value: StackValue, type: Type): StackValue
	
	
	fun jumpIf(condition: StackValue, to: Label)
	
	fun jumpIf(opcode: Int, to: Label)
	
	fun not(condition: StackValue): StackValue
}
