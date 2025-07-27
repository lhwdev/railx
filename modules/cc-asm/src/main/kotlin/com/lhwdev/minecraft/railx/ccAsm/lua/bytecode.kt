package com.lhwdev.minecraft.railx.ccAsm.lua

import com.lhwdev.minecraft.railx.ccAsm.ComputerApi
import com.lhwdev.minecraft.railx.ccAsm.ComputerApiItem
import com.lhwdev.minecraft.railx.ccAsm.InvokeContext
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.util.CheckClassAdapter
import org.squiddev.cobalt.Varargs
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod


val ProxyInstanceName = "INSTANCE"

private fun proxyFunctionName(methodName: String): String =
	"$$methodName"

private object C {
	val context = Type.getType(InvokeContext::class.java)
	val item = Type.getType(ComputerApiItem::class.java)
	
	object Item {
		val obj = Type.getType(ComputerApiItem.Object::class.java)
		val function = Type.getType(ComputerApiItem.Function::class.java)
	}
	
	val varargs = Type.getDescriptor(Varargs::class.java)
}

private fun proxyType(type: Class<*>): Type =
	Type.getType("L${Type.getInternalName(type)}\$Proxy;")


fun generateProxyFromApi(from: KClass<*>): GeneratedProxy {
	val writer = ClassWriter(0)
	CheckClassAdapter(writer, true).doGenerateProxyFromApi(from)
	
	return GeneratedProxy(
		name = "${from.java.name}\$Proxy",
		bytes = writer.toByteArray()
	)
}

class GeneratedProxy(val name: String, val bytes: ByteArray)

private fun ClassVisitor.doGenerateProxyFromApi(from: KClass<*>) {
	val proxyClass = proxyType(from.java)
	
	val functionTargets = from.memberFunctions.filter { it.javaMethod?.declaringClass != Any::class.java }
	
	visit(V12, ACC_PUBLIC, proxyClass.internalName, null, C.Item.obj.internalName, null)
	
	visitField(ACC_PUBLIC + ACC_STATIC, ProxyInstanceName, proxyClass.descriptor, null, null)
	
	visitField(ACC_PRIVATE, "items", "Ljava/util/List;", "Ljava/util/List<${C.item}>;", null)
	
	visitMethod(ACC_PRIVATE, "<init>", "()V", null, null)?.apply {
		visitCode()
		visitVarInsn(ALOAD, 0)
		visitMethodInsn(INVOKESPECIAL, C.Item.obj.internalName, "<init>", "()V", false)
		
		visitIntConstInsn(functionTargets.size)
		visitTypeInsn(ANEWARRAY, C.item.internalName)
		
		val proxyFunctionDescriptor = "($proxyClass${C.context})${C.varargs}"
		for((index, fn) in functionTargets.withIndex()) {
			visitInsn(DUP) // +array -> aastore
			visitIntConstInsn(index) // +index -> aastore
			
			visitTypeInsn(NEW, C.Item.function.internalName)
			visitInsn(DUP)
			visitLdcInsn(fn.name)
			visitLdcInsn(
				Handle(
					H_INVOKESTATIC,
					proxyClass.internalName,
					proxyFunctionName(fn.javaMethod!!.name),
					proxyFunctionDescriptor,
					false
				)
			)
			visitMethodInsn(
				INVOKESPECIAL,
				C.Item.function.internalName,
				"<init>",
				"(Ljava/lang/String;Ljava/lang/invoke/MethodHandle;)V",
				false
			) // +value -> aastore
			
			visitInsn(AASTORE)
		}
		
		visitInsn(RETURN)
		visitMaxs(1, 1)
		visitEnd()
	}
	
	visitMethod(ACC_STATIC, "<clinit>", "()V", null, null)?.apply {
		visitCode()
		visitTypeInsn(NEW, proxyClass.internalName)
		visitInsn(DUP)
		visitMethodInsn(INVOKESPECIAL, proxyClass.internalName, "<init>", "()V", false)
		visitFieldInsn(PUTSTATIC, proxyClass.internalName, ProxyInstanceName, proxyClass.descriptor)
		visitInsn(RETURN)
		visitMaxs(2, 0)
		visitEnd()
	}
	
	functionTargets.forEach { fn ->
		addFunction(from.java, fn)
	}
	
	
	visitEnd()
}

private fun ClassVisitor.addFunction(parent: Class<*>, fn: KFunction<*>) {
	val parameters = fn.parameters.drop(1)
	val argumentCount = parameters.size
	if(argumentCount > 16) throw IllegalStateException("number of arguments cannot exceed 16")
	val optionalCount = parameters.count { it.isOptional }
	val hasOptional = optionalCount > 0
	
	val originalMethod: Method
	val method: Method
	val methodArgumentIndex: Int
	if(hasOptional) {
		// Some description around default function: These are all possible shapes.
		// 1. class FileKt { static void hello(a: Int = 3); static void hello$default(a, flags, marker) }
		// 2. abstract? class Abc { abstract void hello(a: Int = 3); void hello$default(a, flags, marker) }
		// 3. interface Def { void hello(a: Int = 3); public static class DefaultImpls { static void hello$default(self: Def, a, flags, marker) } }
		var default = fn.defaultJavaMethod
		originalMethod = fn.javaMethod!!
		// // uncomment if KT-36854 is fixed: https://youtrack.jetbrains.com/issue/KT-36854
		// //   - KotlinReflectionInternalError on invoking callBy on interface member with default argument value
		// //   - I wonder why they didn't fix such an easy thingy
		// if(!Modifier.isStatic(original.modifiers) && Modifier.isStatic(method.modifiers)) {
		// 	check(method.parameterCount == original.parameterCount + 3) { "unexpected default method shape; kotlin version updated" }
		// 	check(
		// 		method.parameterTypes.contentEquals(
		// 			arrayOf(parent, *original.parameterTypes, Int::class.java, Any::class.java)
		// 		)
		// 	) { "unexpected default method shape; kotlin version updated" }
		// 	1
		// } else {
		// 	0
		// }
		if(default == null) {
			check(parent.isInterface)
			default = Class.forName("${parent.name}\$DefaultImpls")
				.getDeclaredMethod(
					"${originalMethod.name}\$default",
					parent, *originalMethod.parameterTypes, Int::class.java, Any::class.java
				)
			methodArgumentIndex = 1
		} else {
			methodArgumentIndex = 0
		}
		method = default
		
	} else {
		method = fn.javaMethod!!
		originalMethod = method
		methodArgumentIndex = 0
	}
	
	if(Modifier.isStatic(originalMethod.modifiers)) {
		throw IllegalStateException("expected non-static kotlin function")
	}
	
	// Varargs $name(Self self, InvokeContext context) { ... }
	val self = Type.getDescriptor(parent)
	val proxyFunctionName = proxyFunctionName(method.name)
	val proxyFunctionDescriptor = "($self${C.context})${C.varargs}"
	visitMethod(ACC_PUBLIC + ACC_STATIC, proxyFunctionName, proxyFunctionDescriptor, null, null)?.apply {
		visitCode()
		
		val startLabel = Label()
		val endLabel = Label()
		visitLabel(startLabel)
		
		val selfIndex = 0
		val contextIndex = 1
		
		
		fun visitInvokeContextInsn(name: String, descriptor: String) {
			visitMethodInsn(INVOKEVIRTUAL, C.context.internalName, name, descriptor, false)
		}
		
		/// context.argumentsCount(n) / context.argumentsCount(min, max)
		if(hasOptional) {
			visitVarInsn(ALOAD, contextIndex)
			visitIntConstInsn(optionalCount)
			visitIntConstInsn(argumentCount)
			visitInvokeContextInsn("argumentsCount", "(II)V")
		} else {
			visitVarInsn(ALOAD, contextIndex)
			visitIntConstInsn(argumentCount)
			visitInvokeContextInsn("argumentsCount", "(I)V")
		}
		
		val defineFlagLabel = Label()
		val flagsIndex = contextIndex + if(hasOptional) 1 else 0
		if(hasOptional) { /// var flags = 0
			visitInsn(ICONST_0)
			visitVarInsn(ISTORE, flagsIndex)
			visitLabel(defineFlagLabel)
			visitFrame(F_APPEND, 1, arrayOf(INTEGER), 0, null)
		}
		
		val firstIndex = flagsIndex + 1
		var localIndex = firstIndex
		var maxStack = 3
		
		var previousLocalFlushIndex = 0
		val localFrames = mutableListOf<Any>()
		val argumentEndLabels = mutableListOf<Label>()
		
		for((index, parameter) in parameters.withIndex()) {
			val argument = method.parameters[methodArgumentIndex + index]
			val type = argument.type
			val asmType = Type.getType(type)
			
			val argumentEndLabel = Label()
			
			// checkIsOptional: same, () -> (resultToStore)
			if(parameter.isOptional) {
				/// if(context.hasOptional(index, nullable)) <...> else <defaultValueOfType>
				//  - into: if hasOptional -> :continue; push <defaultValueOfType>; goto -> :argumentEnd;
				//          :continue push (parse value); :argumentEnd store
				visitVarInsn(ALOAD, contextIndex)
				visitIntConstInsn(index)
				visitIntConstInsn(if(parameter.type.isMarkedNullable) 1 else 0)
				visitInvokeContextInsn("hasOptional", "(IZ)Z")
				
				val continueLabel = Label()
				visitJumpInsn(IFNE, continueLabel)
				visitFlushFrameLocals(locals = localFrames.toTypedArray(), delta = index - previousLocalFlushIndex)
				previousLocalFlushIndex = index
				
				/// else <defaultValueOfType> // stack = [] -> [argN]
				visitDefaultValueOfType(type)
				visitJumpInsn(GOTO, argumentEndLabel)
				
				/// <...>
				visitLabel(continueLabel) // stack = [] -> [argN]
				visitFrame(F_SAME, 0, null, 0, null)
			} else if(parameter.type.isMarkedNullable) {
				/// if(context.isNull(index)) null else <...>
				visitVarInsn(ALOAD, contextIndex)
				visitIntConstInsn(index)
				visitInvokeContextInsn("isNull", "(I)Z")
				
				val continueLabel = Label()
				visitJumpInsn(IFEQ, continueLabel)
				visitFlushFrameLocals(locals = localFrames.toTypedArray(), delta = index - previousLocalFlushIndex)
				previousLocalFlushIndex = index
				
				/// else <defaultValueOfType> // stack = [] -> [argN]
				visitInsn(ACONST_NULL)
				visitJumpInsn(GOTO, argumentEndLabel)
				
				/// <...>
				visitLabel(continueLabel) // stack = [] -> [argN]
				visitFrame(F_SAME, 0, null, 0, null)
			}
			
			visitVarInsn(ALOAD, contextIndex)
			visitIntConstInsn(index)
			
			when {
				/// context.<type>(index)
				type.isPrimitive -> when(type) {
					Boolean::class.java -> visitInvokeContextInsn("boolean", "(I)Z")
					Byte::class.java -> visitInvokeContextInsn("byte", "(I)B")
					Short::class.java -> visitInvokeContextInsn("short", "(I)S")
					Int::class.java -> visitInvokeContextInsn("int", "(I)I")
					Long::class.java -> visitInvokeContextInsn("long", "(I)J")
					Float::class.java -> visitInvokeContextInsn("float", "(I)F")
					Double::class.java -> visitInvokeContextInsn("double", "(I)D")
					Char::class.java -> visitInvokeContextInsn("char", "(I)C")
					else -> throw NoWhenBranchMatchedException("unknown primitive type $type")
				}
				
				type in BoxedPrimitives.keys -> {
					when(type) {
						Boolean::class.javaObjectType -> visitInvokeContextInsn("boolean", "(I)Z")
						Byte::class.javaObjectType -> visitInvokeContextInsn("byte", "(I)B")
						Short::class.javaObjectType -> visitInvokeContextInsn("short", "(I)S")
						Int::class.javaObjectType -> visitInvokeContextInsn("int", "(I)I")
						Long::class.javaObjectType -> visitInvokeContextInsn("long", "(I)J")
						Float::class.javaObjectType -> visitInvokeContextInsn("float", "(I)F")
						Double::class.javaObjectType -> visitInvokeContextInsn("double", "(I)D")
						Char::class.javaObjectType -> visitInvokeContextInsn("char", "(I)C")
						else -> throw NoWhenBranchMatchedException("unknown primitive type $type")
					}
					BoxedPrimitives[type]!!()
				}
				
				type == String::class.java -> visitInvokeContextInsn("string", "(I)Ljava/lang/String;")
				type == List::class.java -> visitInvokeContextInsn("list", "(I)Ljava/util/List;")
				
				type.isAnnotationPresent(ComputerApi::class.java) -> {
					val argType = proxyType(type)
					visitFieldInsn(GETSTATIC, argType.internalName, ProxyInstanceName, argType.descriptor)
					visitInvokeContextInsn("apiInterface", "(I${C.Item.obj})Ljava/lang/Object;")
				}
				
				else -> throw IllegalStateException("unsupported argument type $type for ${index}th parameter of $fn")
			}
			
			if(parameter.isOptional || parameter.type.isMarkedNullable) {
				visitLabel(argumentEndLabel)
				visitFrame(F_SAME1, 0, null, 1, arrayOf(asmType.frame))
			}
			visitVarInsn(asmType.getOpcode(ISTORE), localIndex)
			localIndex += asmType.size
			localFrames += asmType.frame
			
			val localScopeStartLabel = Label()
			visitLabel(localScopeStartLabel)
			argumentEndLabels += localScopeStartLabel
		}
		val maxLocals = localIndex
		
		/// self.name(arg1, arg2, ...)
		visitVarInsn(ALOAD, selfIndex)
		
		localIndex = firstIndex
		for(index in 0 until argumentCount) { // argN
			val type = method.parameterTypes[methodArgumentIndex + index]
			val asmType = Type.getType(type)
			visitVarInsn(asmType.getOpcode(ILOAD), localIndex)
			localIndex += asmType.size
		}
		
		if(hasOptional) {
			visitVarInsn(ILOAD, flagsIndex) // flags
			visitInsn(ACONST_NULL) // marker
			maxStack = max(maxStack, 1 + (localIndex - firstIndex) + 2) // self + args + optional
		} else {
			maxStack = max(maxStack, 1 + (localIndex - firstIndex))
		}
		
		val invokeInsn = when {
			Modifier.isStatic(method.modifiers) -> INVOKESTATIC
			method.declaringClass.isInterface -> INVOKEINTERFACE
			else -> INVOKEVIRTUAL
		}
		visitMethodInsn(
			invokeInsn,
			Type.getInternalName(method.declaringClass),
			method.name,
			Type.getMethodDescriptor(method),
			invokeInsn == INVOKEINTERFACE,
		)
		
		if(method.returnType != Void.TYPE) {
			/// context.wrapReturn(result)
			val type = method.returnType.let { if(it.isPrimitive) it else Any::class.java }
			val asmType = Type.getType(type)
			visitVarInsn(ALOAD, contextIndex) // stack = [result, context]
			visitInsn(SWAP) // stack = [context, result]
			visitInvokeContextInsn("wrapReturn", "(${asmType.descriptor})${C.varargs}")
			visitInsn(ARETURN)
		} else {
			visitInsn(ACONST_NULL)
			visitInsn(ARETURN)
		}
		
		visitLabel(endLabel)
		
		visitLocalVariable("self", self, null, startLabel, endLabel, selfIndex)
		visitLocalVariable("context", C.context.descriptor, null, startLabel, endLabel, contextIndex)
		if(hasOptional) visitLocalVariable("flags", "I", null, defineFlagLabel, endLabel, flagsIndex)
		
		localIndex = firstIndex
		for((index, parameter) in parameters.withIndex()) {
			val argument = method.parameters[methodArgumentIndex + index]
			visitLocalVariable(
				parameter.name ?: argument.name,
				Type.getDescriptor(argument.type),
				null,
				argumentEndLabels[index],
				endLabel,
				localIndex
			)
			localIndex += Type.getType(argument.type).size
		}
		
		visitMaxs(maxStack, maxLocals)
		visitEnd()
	}
}

private fun MethodVisitor.visitIntConstInsn(value: Int) {
	when(value) {
		-1 -> visitInsn(ICONST_M1)
		0 -> visitInsn(ICONST_0)
		1 -> visitInsn(ICONST_1)
		2 -> visitInsn(ICONST_2)
		3 -> visitInsn(ICONST_3)
		4 -> visitInsn(ICONST_4)
		5 -> visitInsn(ICONST_5)
		in 0..Byte.MAX_VALUE -> visitIntInsn(BIPUSH, value)
		in 0..Short.MAX_VALUE -> visitIntInsn(BIPUSH, value)
		else -> visitLdcInsn(value)
	}
}

private val BoxedPrimitives: Map<Class<*>, MethodVisitor.() -> Unit> = listOf(
	Boolean::class,
	Byte::class, Short::class, Int::class, Long::class,
	Float::class, Double::class,
	Char::class,
).associate { type ->
	val objectType = type.javaObjectType
	val asmObject = Type.getType(objectType)
	val boxDescriptor = "(${Type.getDescriptor(type.java)})${asmObject.descriptor}"
	
	objectType to {
		visitMethodInsn(INVOKESTATIC, asmObject.internalName, "valueOf", boxDescriptor, false)
	}
}

private fun MethodVisitor.visitDefaultValueOfType(type: Class<*>) {
	when(type) {
		Boolean::class.java, Byte::class.java, Short::class.java, Int::class.java, Char::class.java ->
			visitInsn(ICONST_0)
		
		Long::class.java -> visitInsn(LCONST_0)
		Float::class.java -> visitInsn(FCONST_0)
		Double::class.java -> visitInsn(DCONST_0)
		else -> visitInsn(ACONST_NULL)
	}
}

private val Type.frame: Any
	get() = when(this) {
		Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.CHAR_TYPE -> INTEGER
		Type.LONG_TYPE -> LONG
		Type.FLOAT_TYPE -> FLOAT
		Type.DOUBLE_TYPE -> DOUBLE
		else -> internalName
	}

private fun MethodVisitor.visitFlushFrameLocals(
	locals: Array<Any>,
	delta: Int,
	previousStacks: Array<Any> = emptyArray(),
) {
	when {
		delta > 3 || delta < -3 -> visitFrame(F_FULL, locals.size, locals, previousStacks.size, previousStacks)
		delta > 0 -> visitFrame(F_APPEND, delta, locals.takeLast(delta).toTypedArray(), 0, null)
		delta < 0 -> visitFrame(F_CHOP, delta, null, previousStacks.size, previousStacks)
	}
}
