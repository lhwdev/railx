package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.Type
import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass


interface ClassDescriptor {
	val type: Type
	
	val name: String get() = type.descriptor
	
	val asmType: org.objectweb.asm.Type
		get() = type.asm
	
	val isInterface: Boolean
}

fun descriptor(type: Type, isInterface: Boolean): ClassDescriptor =
	SimpleClassDescriptor(type, isInterface)

fun descriptor(klass: Class<*>): ClassDescriptor =
	SimpleClassDescriptor(Type.getType(klass), klass.isInterface)

fun descriptor(klass: KClass<*>): ClassDescriptor =
	descriptor(klass.java)


fun ClassDescriptor.method(name: String, type: String): MethodDescriptor {
	val opcode = when {
		isInterface -> Opcodes.INVOKEINTERFACE
		else -> Opcodes.INVOKEVIRTUAL
	}
	return SimpleMethodDescriptor(opcode, this, name, Type.getMethodType(type))
}

fun ClassDescriptor.staticMethod(name: String, type: String): MethodDescriptor =
	SimpleMethodDescriptor(Opcodes.INVOKESTATIC, this, name, Type.getMethodType(type))

fun ClassDescriptor.field(name: String, type: String): FieldDescriptor =
	SimpleFieldDescriptor(FieldDescriptor.Kind.Instance, this, name, type)

fun ClassDescriptor.staticField(name: String, type: String): FieldDescriptor =
	SimpleFieldDescriptor(FieldDescriptor.Kind.Static, this, name, type)


class SimpleClassDescriptor(type: Type, override val isInterface: Boolean) : ClassDescriptor {
	override val type: Type
		get() = Type(this)
	
	override val asmType: org.objectweb.asm.Type = type.asm
}
