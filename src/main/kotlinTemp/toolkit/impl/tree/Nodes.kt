package com.lhwdev.asm.toolkit.impl.tree

import com.lhwdev.asm.toolkit.Type
import jdk.internal.org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*


object Nodes {
	object Dup : ExpressionNode {
		override val type: Type
			get() = Type.NothingType
		
		override fun accept(output: MethodVisitor) {
			error("special primitives")
		}
	}
	
	abstract class Constant : ExpressionNode {
		object NullConstant : Constant() {
			override val type: Type get() = Type.NothingType
			
			override fun accept(output: MethodVisitor) {
				output.visitInsn(ACONST_NULL)
			}
		}
		
		abstract class LdcConstant : Constant() {
			abstract val value: Any?
			
			override fun accept(output: MethodVisitor) {
				output.visitLdcInsn(value)
			}
		}
		
		class StringConstant(override val value: String) : LdcConstant() {
			override val type: Type get() = Type.StringType
		}
		
		class ClassConstant(override val value: org.objectweb.asm.Type) : LdcConstant() {
			override val type: Type get() = Type.ClassType
		}
		
		class IntConstant(val value: Int) : Constant() {
			override val type: Type get() = Type.INT_TYPE
			
			override fun accept(output: MethodVisitor) {
				when(value) {
					0 -> output.visitInsn(Opcodes.ICONST_0)
					in -1..5 -> when(value) {
						1 -> output.visitInsn(Opcodes.ICONST_1)
						-1 -> output.visitInsn(Opcodes.ICONST_M1)
						2 -> output.visitInsn(Opcodes.ICONST_2)
						3 -> output.visitInsn(Opcodes.ICONST_3)
						4 -> output.visitInsn(Opcodes.ICONST_4)
						5 -> output.visitInsn(Opcodes.ICONST_5)
					}
					
					in Byte.MIN_VALUE..Byte.MAX_VALUE -> output.visitIntInsn(Opcodes.BIPUSH, value)
					in Short.MIN_VALUE..Short.MAX_VALUE -> output.visitIntInsn(Opcodes.SIPUSH, value)
					else -> output.visitLdcInsn(value)
				}
			}
		}
		
		class LongConstant(val value: Long) : Constant() {
			override val type: Type get() = Type.LONG_TYPE
			
			override fun accept(output: MethodVisitor) {
				when(value) {
					0L -> output.visitInsn(Opcodes.LCONST_0)
					1L -> output.visitInsn(Opcodes.LCONST_1)
					else -> output.visitLdcInsn(value)
				}
			}
		}
		
		class FloatConstant(val value: Float) : Constant() {
			override val type: Type get() = Type.FLOAT_TYPE
			
			override fun accept(output: MethodVisitor) {
				when(value) {
					0f -> output.visitInsn(Opcodes.FCONST_0)
					1f -> output.visitInsn(Opcodes.FCONST_1)
					2f -> output.visitInsn(Opcodes.FCONST_2)
					else -> output.visitLdcInsn(value)
				}
			}
		}
		
		class DoubleConstant(val value: Double) : Constant() {
			override val type: Type get() = Type.DOUBLE_TYPE
			
			override fun accept(output: MethodVisitor) {
				when(value) {
					0.0 -> output.visitInsn(Opcodes.DCONST_0)
					1.0 -> output.visitInsn(Opcodes.DCONST_1)
					else -> output.visitLdcInsn(value)
				}
			}
		}
	}
	
	
}
