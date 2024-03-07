package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.descriptor.ClassDescriptor


fun constant(value: Boolean): Value = constant(if(value) 1 else 0)

fun constant(value: Char): Value = constant(value.code)

fun constant(value: Byte): Value = constant(value.toInt())

fun constant(value: Short): Value = constant(value.toInt())

fun constant(value: Int): Value = IntConstant(value)

private class IntConstant(private val value: Int) : Value {
	override val type: Type
		get() = Type.INT_TYPE
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is IntConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}

fun constant(value: Long): Value = LongConstant(value)

private class LongConstant(private val value: Long) : Value {
	override val type: Type
		get() = Type.LONG_TYPE
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is LongConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}


fun constant(value: Float): Value = FloatConstant(value)

private class FloatConstant(private val value: Float) : Value {
	override val type: Type
		get() = Type.FLOAT_TYPE
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is FloatConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}

fun constant(value: Double): Value = DoubleConstant(value)

private class DoubleConstant(private val value: Double) : Value {
	override val type: Type
		get() = Type.DOUBLE_TYPE
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is DoubleConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}

private val ObjectType = Type.getObjectType("java/lang/Object")
private val StringType = Type.getObjectType("java/lang/String")
private val ClassType = Type.getObjectType("java/lang/Class")

fun constant(value: Nothing?): Value = NullConstant

private object NullConstant : Value {
	override val type: Type
		get() = ObjectType
	
	context(CodeContext)
	override fun push(): StackValue =
		push(null)
	
	override fun equals(other: Any?): Boolean =
		this === other
}

fun constant(value: String): Value = StringConstant(value)

private class StringConstant(private val value: String) : Value {
	override val type: Type
		get() = StringType
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is StringConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}

fun constant(value: Type): Value = ClassConstant(value)

fun constant(value: ClassDescriptor): Value = constant(value.type)

fun constant(value: Class<*>): Value = constant(Type.getType(value))


private class ClassConstant(private val value: Type) : Value {
	override val type: Type
		get() = ClassType
	
	context(CodeContext)
	override fun push(): StackValue =
		push(value)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is ClassConstant -> false
		else -> value == other.value
	}
	
	override fun hashCode(): Int =
		value.hashCode()
}
