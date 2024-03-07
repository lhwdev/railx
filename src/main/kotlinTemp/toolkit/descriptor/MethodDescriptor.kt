package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.Type
import org.objectweb.asm.Opcodes
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod


interface MethodDescriptor {
	val opcode: Int
	
	val owner: ClassDescriptor
	
	val name: String
	
	val methodType: Type
	
	val returnType: Type
}


fun descriptor(method: Method): MethodDescriptor {
	val opcode = when {
		Modifier.isStatic(method.modifiers) -> Opcodes.INVOKESTATIC
		method.declaringClass.isInterface -> Opcodes.INVOKEINTERFACE
		else -> Opcodes.INVOKEVIRTUAL
	}
	return SimpleMethodDescriptor(
		opcode = opcode,
		owner = descriptor(method.declaringClass),
		name = method.name,
		methodType = Type.getType(Type.getMethodDescriptor(method)),
	)
}

fun descriptor(method: KFunction<*>): MethodDescriptor =
	descriptor(method.javaMethod!!)

class SimpleMethodDescriptor(
	override val opcode: Int,
	override val owner: ClassDescriptor,
	override val name: String,
	override val methodType: Type,
) : MethodDescriptor {
	override val returnType: Type = methodType.returnType
}
