package com.lhwdev.minecraft.railx.ccAdvanced.handler.lua

import com.lhwdev.minecraft.railx.ccAdvanced.handler.ComputerApi
import com.lhwdev.minecraft.railx.ccAdvanced.handler.ComputerApiProxy
import com.lhwdev.minecraft.railx.ccAdvanced.handler.ComputerState
import com.lhwdev.minecraft.railx.ccAdvanced.handler.InvokeContext
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.squiddev.cobalt.Varargs
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod


private object C {
	val state = Type.getDescriptor(ComputerState::class.java)
	val context = Type.getDescriptor(InvokeContext::class.java)
	val proxy = Type.getDescriptor(ComputerApiProxy::class.java)
	
	val varargs = Type.getDescriptor(Varargs::class.java)
	
	val Function = "($context)$varargs"
}

private fun proxyName(className: String): String =
	"$className\$Proxy"


private fun ClassVisitor.addFunction(fn: KFunction<*>, context: ProcessContext) {
	val argumentCount = fn.parameters.size - 1
	if(argumentCount > 16) throw IllegalStateException("number of arguments cannot exceed 16")
	val optionalCount = fn.parameters.count { it.isOptional }
	val hasOptional = optionalCount > 0
	val method = if(hasOptional) {
		fn.defaultJavaMethod!!.also { check(it.parameterCount == argumentCount + 2) }
	} else fn.javaMethod!!
	
	if(Modifier.isStatic(method.modifiers)) {
		throw IllegalStateException("expected non-static kotlin function")
	}
	
	// Varargs $name(Self self, InvokeContext context) { ... }
	visitMethod(ACC_PUBLIC + ACC_STATIC, "$${method.name}", C.Function, null, null).apply {
		visitCode()
		
		val startLabel = Label()
		val endLabel = Label()
		visitLabel(startLabel)
		
		val selfIndex = 0
		val contextIndex = 1
		
		fun visitInvokeContextInsn(name: String, descriptor: String) {
			visitMethodInsn(INVOKEVIRTUAL, C.context, name, descriptor, false)
		}
		
		/// context.argumentsCount(n) / context.argumentsCount(min, max)
		if(hasOptional) {
			visitVarInsn(ALOAD, contextIndex)
			visitIntConstInsn(optionalCount)
			visitIntConstInsn(fn.parameters.size)
			visitMethodInsn(INVOKEVIRTUAL, C.context, "argumentsCount", "(II)V", false)
		} else {
			visitVarInsn(ALOAD, contextIndex)
			visitIntConstInsn(argumentCount)
			visitMethodInsn(INVOKEVIRTUAL, C.context, "argumentsCount", "(I)V", false)
		}
		
		val flagsIndex = contextIndex + if(hasOptional) 1 else 0
		if(hasOptional) { /// var flags = 0
			visitInsn(ICONST_0)
			visitVarInsn(ISTORE, flagsIndex)
			visitLocalVariable("flags", "I", null, startLabel, endLabel, flagsIndex)
		}
		
		val firstIndex = flagsIndex + 1
		var localIndex = firstIndex
		
		for(index in 0..<argumentCount) {
			val parameter = fn.parameters[index]
			val argument = method.parameters[index]
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
				visitInsn(if(parameter.type.isMarkedNullable) 1 else 0)
				visitInvokeContextInsn("hasOptional", "(IZ)Z")
				
				val continueLabel = Label()
				visitJumpInsn(IFNE, continueLabel)
				visitFrame(F_SAME, 0, null, 0, null)
				
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
				visitFrame(F_SAME, 0, null, 0, null)
				
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
					Long::class.java -> visitInvokeContextInsn("long", "(I)L")
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
				
				type.isAnnotationPresent(ComputerApi::class.java) -> {
					val name = proxyName(Type.getDescriptor(type))
					visitFieldInsn(GETSTATIC, name, "INSTANCE", name)
					visitInvokeContextInsn("apiInterface", "(I${C.proxy})Ljava/lang/Object;")
				}
				
				else -> IllegalStateException("unsupported argument type $type")
			}
			
			if(parameter.isOptional || parameter.type.isMarkedNullable) {
				visitLabel(argumentEndLabel)
				visitFrame(F_SAME1, 0, null, 1, arrayOf(asmType.frame))
			}
			visitVarInsn(asmType.getOpcode(ISTORE), localIndex)
			visitLocalVariable(argument.name, asmType.descriptor, null, startLabel, endLabel, localIndex)
			localIndex += asmType.size
		}
		
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
			visitInsn(RETURN)
		}
		
		visitLabel(endLabel)
		
		visitMaxs(1, firstIndex + method.parameterCount)
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
	val objectDescriptor = Type.getDescriptor(objectType)
	val boxDescriptor = "(${Type.getDescriptor(type.java)})$objectDescriptor"
	
	objectType to {
		visitMethodInsn(INVOKESTATIC, objectDescriptor, "valueOf", boxDescriptor, false)
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
