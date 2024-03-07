package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.BinaryOperationKind
import com.lhwdev.asm.toolkit.CodeInstructions
import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.Variable
import com.lhwdev.asm.toolkit.descriptor.FieldHandle
import com.lhwdev.asm.toolkit.descriptor.StaticFieldHandle
import com.lhwdev.asm.toolkit.value.*
import jdk.internal.org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Label


class AsmCodeOps(private val code: AsmCodeContext) : LoadContext, StoreContext, CodeInstructions {
	
	
	/// LoadContext
	
	override fun pushFromVariable(variable: Variable): StackValue =
		code.expression(variable.type) {
			variable as VariableImpl
			expression.visitVarInsn(variable.type.getOpcode(ILOAD), variable.index)
		}
	
	override fun pushFromStaticField(handle: StaticFieldHandle): StackValue =
		code.expression(handle.type) {
			expression.visitFieldInsn(GETSTATIC, handle.owner.name, handle.name, handle.type.descriptor)
		}
	
	override fun pushFromField(handle: FieldHandle, instance: StackValue): StackValue =
		code.expression(handle.type) {
			parameter(instance.toStack())
			expression.visitFieldInsn(GETFIELD, handle.owner.name, handle.name, handle.type.descriptor)
		}
	
	
	/// StoreContext
	
	override fun storeVariable(from: StackValue, to: Variable) = code.statement {
		to as VariableImpl
		parameter(from)
		expression.visitVarInsn(to.type.getOpcode(ISTORE), to.index)
	}
	
	override fun storeStaticField(handle: StaticFieldHandle, from: StackValue) = code.statement {
		parameter(from)
		expression.visitFieldInsn(PUTFIELD, handle.owner.name, handle.name, handle.type.descriptor)
	}
	
	override fun storeField(handle: FieldHandle, instance: StackValue, from: StackValue) = code.statement {
		parameter(instance, from)
		expression.visitFieldInsn(PUTFIELD, handle.owner.name, handle.name, handle.type.descriptor)
	}
	
	
	/// CodeInstructions
	
	override fun unaryOperation(opcode: Int, operand: StackValue): StackValue =
		code.expression(operand.type) {
			parameter(operand)
			expression.visitInsn(opcode)
		}
	
	override fun binaryOperation(opcode: Int, lhs: StackValue, rhs: StackValue, kind: BinaryOperationKind): StackValue =
		code.expression(kind.getResultType(lhs.type, rhs.type)) {
			parameter(lhs, rhs)
			expression.visitInsn(opcode)
		}
	
	override fun binaryOperation(operator: LogicBinaryOperator, lhs: StackValue, rhs: StackValue): StackValue =
		code.expression(Type.BOOLEAN_TYPE) {
			parameter(lhs)
			parameter(rhs)
			expression.instructions.add(LogicInsnNode(operator, lhs, rhs))
		}
	
	override fun iinc(variable: Variable, operand: Int) {
		TODO("Not yet implemented")
	}
	
	override fun conversion(opcode: Int, operand: StackValue, resultType: Type): StackValue {
		TODO("Not yet implemented")
	}
	
	override fun swap(lhs: StackValue, rhs: StackValue): Pair<StackValue, StackValue> {
		TODO("Not yet implemented")
	}
	
	
	override fun checkCast(value: StackValue, type: Type): StackValue {
		TODO("Not yet implemented")
	}
	
	override fun instanceOf(value: StackValue, type: Type): StackValue {
		TODO("Not yet implemented")
	}
	
	
	override fun jumpIf(condition: StackValue, to: Label) {
		TODO("Not yet implemented")
	}
	
	override fun jumpIf(opcode: Int, to: Label) {
		TODO("Not yet implemented")
	}
	
	override fun not(condition: StackValue): StackValue {
		TODO("Not yet implemented")
	}
}
