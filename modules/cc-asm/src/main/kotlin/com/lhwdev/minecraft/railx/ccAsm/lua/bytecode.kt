package com.lhwdev.minecraft.railx.ccAsm.lua

import com.lhwdev.minecraft.railx.ccAsm.ComputerApi
import com.lhwdev.minecraft.railx.ccAsm.ComputerApiProxy
import com.lhwdev.minecraft.railx.ccAsm.InvokeContext
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.util.CheckClassAdapter
import org.squiddev.cobalt.Varargs
import java.lang.reflect.Modifier
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod


private object C {
	val context = Type.getType(InvokeContext::class.java)
	val proxy = Type.getType(ComputerApiProxy::class.java)
	
	val varargs = Type.getDescriptor(Varargs::class.java)
}

private fun proxyName(type: Class<*>): String =
	"${Type.getInternalName(type)}\$Proxy"


fun generateProxyFromApi(from: KClass<*>): ByteArray {
	val writer = ClassWriter(0)
	CheckClassAdapter(writer, true).doGenerateProxyFromApi(from)
	
	return writer.toByteArray()
}

private fun ClassVisitor.doGenerateProxyFromApi(from: KClass<*>) {
	visit(V12, ACC_PUBLIC, proxyName(from.java), null, C.proxy.internalName, null)
	
	from.memberFunctions.filter { it.javaMethod?.declaringClass != Any::class.java }.forEach { fn ->
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
	val method = if(hasOptional) {
		fn.defaultJavaMethod!!.also { check(it.parameterCount == argumentCount + 2) }
	} else fn.javaMethod!!
	
	if(Modifier.isStatic(method.modifiers)) {
		throw IllegalStateException("expected non-static kotlin function")
	}
	
	// Varargs $name(Self self, InvokeContext context) { ... }
	val self = Type.getDescriptor(parent)
	visitMethod(ACC_PUBLIC + ACC_STATIC, "$${method.name}", "($self${C.context})${C.varargs}", null, null)?.apply {
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
		
		val flagsIndex = contextIndex + if(hasOptional) 1 else 0
		if(hasOptional) { /// var flags = 0
			visitInsn(ICONST_0)
			visitVarInsn(ISTORE, flagsIndex)
			visitLocalVariable("flags", "I", null, startLabel, endLabel, flagsIndex)
		}
		
		val firstIndex = flagsIndex + 1
		var localIndex = firstIndex
		var maxStack = 3
		
		var previousLocalFlushIndex = 0
		val localFrames = mutableListOf<Any>()
		val argumentStartLabels = mutableListOf<Label>()
		
		for((index, parameter) in parameters.withIndex()) {
			val argument = method.parameters[index]
			val type = argument.type
			val asmType = Type.getType(type)
			
			val argumentStartLabel = Label()
			val argumentEndLabel = Label()
			visitLabel(argumentStartLabel)
			argumentStartLabels += argumentStartLabel
			
			// checkIsOptional: same, () -> (resultToStore)
			if(parameter.isOptional) {
				/// if(context.hasOptional(index, nullable)) <...> else <defaultValueOfType>
				//  - into: if hasOptional -> :continue; push <defaultValueOfType>; goto -> :argumentEnd;
				//          :continue push (parse value); :argumentEnd store
				visitVarInsn(ALOAD, contextIndex)
				visitIntConstInsn(index)
				visitInsn(if(parameter.type.isMarkedNullable) 1 else 0)
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
			
			fun getPrimitive() {
				when(type) {
					Boolean::class.java -> visitInvokeContextInsn("boolean", "(I)Z")
					Byte::class.java -> visitInvokeContextInsn("byte", "(I)B")
					Short::class.java -> visitInvokeContextInsn("short", "(I)S")
					Int::class.java -> visitInvokeContextInsn("int", "(I)I")
					Long::class.java -> visitInvokeContextInsn("long", "(I)J")
					Float::class.java -> visitInvokeContextInsn("float", "(I)F")
					Double::class.java -> visitInvokeContextInsn("double", "(I)D")
					Char::class.java -> visitInvokeContextInsn("char", "(I)C")
					else -> NoWhenBranchMatchedException("unknown primitive type $type")
				}
			}
			
			when {
				/// context.<type>(index)
				type.isPrimitive -> getPrimitive()
				
				type in BoxedPrimitives.keys -> {
					getPrimitive()
					BoxedPrimitives[type]!!()
				}
				
				type == String::class.java -> visitInvokeContextInsn("string", "(I)Ljava/lang/String;")
				type == List::class.java -> visitInvokeContextInsn("list", "(I)Ljava/util/List;")
				
				type.isAnnotationPresent(ComputerApi::class.java) -> {
					val name = proxyName(type)
					visitFieldInsn(GETSTATIC, name, "INSTANCE", "L$name;")
					visitInvokeContextInsn("apiInterface", "(I${C.proxy})Ljava/lang/Object;")
				}
				
				else -> throw IllegalStateException("unsupported argument type $type for ${index}th parameter of $fn")
			}
			
			if(parameter.isOptional || parameter.type.isMarkedNullable) {
				visitLabel(argumentEndLabel)
				visitFrame(F_SAME1, 0, null, 1, arrayOf(asmType.frame))
			}
			visitVarInsn(asmType.getOpcode(ISTORE), localIndex)
			println("arg[$index] index=$localIndex name=${parameter.name} ?: ${argument.name}")
			localIndex += asmType.size
			localFrames += asmType.frame
		}
		val maxLocals = localIndex
		
		/// self.name(arg1, arg2, ...)
		visitVarInsn(ALOAD, selfIndex)
		
		localIndex = firstIndex
		for(index in 0 until argumentCount) { // argN
			val type = method.parameterTypes[index]
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
		
		localIndex = firstIndex
		for((index, parameter) in parameters.withIndex()) {
			val argument = method.parameters[index]
			visitLocalVariable(
				parameter.name ?: argument.name,
				Type.getDescriptor(argument.type),
				null,
				argumentStartLabels[index],
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

private fun MethodVisitor.visitFlushFrameLocals(locals: Array<Any>, delta: Int, previousStacks: Array<Any> = emptyArray()) {
	when {
		delta > 3 || delta < -3 -> visitFrame(F_FULL, locals.size, locals, previousStacks.size, previousStacks)
		delta > 0 -> visitFrame(F_APPEND, delta, locals.takeLast(delta).toTypedArray(), 0, null)
		delta < 0 -> visitFrame(F_CHOP, delta, null, previousStacks.size, previousStacks)
	}
}
