package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type
import org.objectweb.asm.Opcodes.*


context(CodeContext)
fun Value.toInt(): StackValue {
	val value = toStackTop()
	
	return when(type.sort) {
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> value
		Type.FLOAT -> instructions.conversion(F2I, value, Type.INT_TYPE)
		Type.LONG -> instructions.conversion(L2I, value, Type.INT_TYPE)
		Type.DOUBLE -> instructions.conversion(D2I, value, Type.INT_TYPE)
		else -> throw UnsupportedOperationException()
	}
}

context(CodeContext)
fun Value.toLong(): StackValue {
	val value = toStackTop()
	
	return when(type.sort) {
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> instructions.conversion(I2L, value, Type.LONG_TYPE)
		Type.FLOAT -> instructions.conversion(F2L, value, Type.LONG_TYPE)
		Type.LONG -> value
		Type.DOUBLE -> instructions.conversion(D2L, value, Type.LONG_TYPE)
		else -> throw UnsupportedOperationException()
	}
}

context(CodeContext)
fun Value.toFloat(): StackValue {
	val value = toStackTop()
	
	return when(type.sort) {
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> instructions.conversion(I2F, value, Type.FLOAT_TYPE)
		Type.FLOAT -> value
		Type.LONG -> instructions.conversion(L2F, value, Type.FLOAT_TYPE)
		Type.DOUBLE -> instructions.conversion(D2F, value, Type.FLOAT_TYPE)
		else -> throw UnsupportedOperationException()
	}
}

context(CodeContext)
fun Value.toDouble(): StackValue {
	val value = toStackTop()
	
	return when(type.sort) {
		Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> instructions.conversion(
			I2D,
			value,
			Type.DOUBLE_TYPE
		)
		
		Type.FLOAT -> instructions.conversion(F2D, value, Type.DOUBLE_TYPE)
		Type.LONG -> instructions.conversion(L2D, value, Type.DOUBLE_TYPE)
		Type.DOUBLE -> value
		else -> throw UnsupportedOperationException()
	}
}
