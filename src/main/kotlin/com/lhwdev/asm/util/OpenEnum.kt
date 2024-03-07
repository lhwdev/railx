package com.lhwdev.asm.util

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


private fun wrapperName(name: String, kind: String): String =
	"com.lhwdev.asm.util.generated.$kind.${name.replace('.', '_')}"


interface OpenEnum

@OptIn(ExperimentalContracts::class)
fun <T : Enum<T>> T.ensureOpenEnum() {
	contract {
		returns() implies (this@ensureOpenEnum is OpenEnum)
	}
	
	if(this !is OpenEnum) error("$this is not OpenEnum")
}

@OptIn(ExperimentalContracts::class)
fun <T : Enum<T>> T.ensureNotOpenEnum() {
	contract {
		returns() implies (this@ensureNotOpenEnum !is OpenEnum)
	}
	
	if(this is OpenEnum) error("$this is OpenEnum")
}

fun <T : Enum<T>> Class<T>.getEnumConstructor(
	vararg constructorTypes: Class<*>,
): MethodHandle = BytecodeClassLoader.getOrCreateClass(wrapperName(name, "enum")) { className ->
	val klass = this
	val thisType = Type.getType(klass)
	
	require(!Modifier.isAbstract(modifiers)) { "Enum should not be abstract" }
	
	val writer = ClassWriter(0)
	
	writer.visit(
		V1_8,
		ACC_PUBLIC + ACC_FINAL,
		className,
		null,
		thisType.descriptor,
		arrayOf(Type.getDescriptor(OpenEnum::class.java))
	)
	
	
	val parameters = constructorTypes.map { Type.getType(it) }.toTypedArray()
	
	writer.visitMethod(
		ACC_PUBLIC,
		"<init>",
		Type.getMethodDescriptor(Type.VOID_TYPE, *parameters),
		null,
		null,
	)?.run {
		visitCode()
		
		visitLdcInsn("?")
		visitLdcInsn(Int.MAX_VALUE)
		var stackIndex = 2
		var varIndex = 0
		for(parameter in parameters) {
			visitVarInsn(parameter.getOpcode(ILOAD), varIndex)
			varIndex += parameter.size
			stackIndex += parameter.size
		}
		visitMethodInsn(
			INVOKESPECIAL,
			thisType.descriptor,
			"<init>",
			Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String::class.java), Type.INT_TYPE, *parameters),
			false
		)
		
		visitMaxs(stackIndex, varIndex)
		visitEnd()
	}
	
	ByteBuffer.wrap(writer.toByteArray())
}.declaredConstructors.single().let { MethodHandles.publicLookup().unreflectConstructor(it) }
