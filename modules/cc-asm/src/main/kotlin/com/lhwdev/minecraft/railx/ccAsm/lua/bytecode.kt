package com.lhwdev.minecraft.railx.ccAsm.lua

import com.lhwdev.minecraft.railx.ccAsm.*
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.util.CheckClassAdapter
import org.squiddev.cobalt.Varargs
import java.lang.reflect.*
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
	val runtime = Type.getType(ComputerApiRuntime::class.java)
	
	object Parse {
		val parseList = Type.getType(ParseContext.ParseList::class.java)
	}
	
	object Item {
		val named = Type.getType(ComputerApiItem.Named::class.java)
		val function = Type.getType(ComputerApiItem.Function::class.java)
		val obj = Type.getType(ComputerApiItem.Object::class.java)
	}
	
	val varargs = Type.getType(Varargs::class.java)
	
	val notNull = Type.getType(org.jetbrains.annotations.NotNull::class.java)
	val nullable = Type.getType(org.jetbrains.annotations.Nullable::class.java)
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
	val originalClass = Type.getType(from.java)
	val proxyClass = proxyType(from.java)
	
	val functionTargets = from.memberFunctions.filter { it.javaMethod?.declaringClass != Any::class.java }
	
	visit(V12, ACC_PUBLIC, proxyClass.internalName, null, C.Item.obj.internalName, null)
	visitSource("?", "whoosh")
	
	visitField(ACC_PUBLIC + ACC_STATIC, ProxyInstanceName, proxyClass.descriptor, null, null)?.apply {
		visitAnnotation(C.notNull.descriptor, false)
		visitEnd()
	}
	
	visitField(ACC_PRIVATE, "items", "Ljava/util/List;", "Ljava/util/List<${C.Item.named}>;", null)?.apply {
		visitAnnotation(C.notNull.descriptor, false)
		visitEnd()
	}
	
	visitMethod(ACC_PRIVATE, "<init>", "()V", null, null)?.apply {
		visitCode()
		visitVarInsn(ALOAD, 0)
		visitMethodInsn(INVOKESPECIAL, C.Item.obj.internalName, "<init>", "()V", false)
		
		visitVarInsn(ALOAD, 0) // this -> putfield
		
		visitIntConstInsn(functionTargets.size)
		visitTypeInsn(ANEWARRAY, C.Item.named.internalName)
		
		val proxyFunctionDescriptor = "($originalClass${C.context})${C.varargs}"
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
		
		visitMethodInsn(
			INVOKESTATIC,
			C.runtime.internalName,
			"namedApiItems",
			"([${C.Item.named})Ljava/util/List;",
			false
		) // -> putfield
		
		visitFieldInsn(PUTFIELD, proxyClass.internalName, "items", "Ljava/util/List;")
		
		visitInsn(RETURN)
		visitMaxs(8, 1) // Arr [Arr Ind I.F [I.F name handle]]
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
	
	visitMethod(ACC_PUBLIC, "getType", "()Ljava/lang/Class;", "()Ljava/lang/Class<${originalClass}>;", null)?.apply {
		visitAnnotation("Ljava/lang/Override;", false)
		visitAnnotation(C.notNull.descriptor, false)
		
		visitCode()
		visitLdcInsn(originalClass)
		visitInsn(ARETURN)
		visitMaxs(1, 1)
		visitEnd()
	}
	
	visitMethod(ACC_PUBLIC, "getItems", "()Ljava/util/List;", "()Ljava/util/List<${C.Item.named}>;", null)?.apply {
		visitAnnotation("Ljava/lang/Override;", false)
		visitAnnotation(C.notNull.descriptor, false)
		
		visitCode()
		visitVarInsn(ALOAD, 0)
		visitFieldInsn(GETFIELD, proxyClass.internalName, "items", "Ljava/util/List;")
		visitInsn(ARETURN)
		visitMaxs(1, 1)
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
	val proxyFunctionName = proxyFunctionName(originalMethod.name)
	val proxyFunctionDescriptor = "($self${C.context})${C.varargs}"
	visitMethod(ACC_PUBLIC + ACC_STATIC, proxyFunctionName, proxyFunctionDescriptor, null, null)?.apply {
		visitParameterAnnotation(0, C.notNull.descriptor, false)
		visitParameterAnnotation(1, C.notNull.descriptor, false)
		visitAnnotation(C.notNull.descriptor, false)
		
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
		var maxLocals = 0
		
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
				/// if(context.hasOptional(index, nullable)) <...> else { flags |= <0x1 shl index>; <defaultValueOfType> }
				//  - into: if hasOptional -> :continue; push <defaultValueOfType>; goto -> :argumentEnd;
				//          :continue push (parse value); :argumentEnd store
				visitVarInsn(ALOAD, contextIndex)
				visitIntConstInsn(if(parameter.type.isMarkedNullable) 1 else 0)
				visitInvokeContextInsn("hasOptional", "(Z)Z")
				
				val continueLabel = Label()
				visitJumpInsn(IFNE, continueLabel)
				visitFlushFrameLocals(locals = localFrames.toTypedArray(), delta = index - previousLocalFlushIndex)
				previousLocalFlushIndex = index
				
				/// else { flags |= <0x1 shl index>; <defaultValueOfType> } // stack = [] -> [argN]
				visitVarInsn(ILOAD, flagsIndex)
				visitIntConstInsn(0x1 shl index)
				visitInsn(IOR)
				visitVarInsn(ISTORE, flagsIndex)
				
				visitDefaultValueOfType(type)
				visitJumpInsn(GOTO, argumentEndLabel)
				
				/// <...>
				visitLabel(continueLabel) // stack = [] -> [argN]
				visitFrame(F_SAME, 0, null, 0, null)
			} else if(parameter.type.isMarkedNullable) {
				/// if(context.isNull()) null else <...>
				visitVarInsn(ALOAD, contextIndex)
				visitInvokeContextInsn("isNull", "()Z")
				
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
			
			// stack = [context] -> [result]
			fun parseValue(
				contextIndex: Int,
				type: Class<*>,
				parameterized: () -> java.lang.reflect.Type,
				stack: Int,
				local: Int,
			) {
				fun updateStack(add: Int) {
					maxStack = max(maxStack, stack + add)
				}
				
				fun updateLocal(add: Int) {
					maxLocals = max(maxLocals, local + add)
				}
				updateStack(1)
				updateLocal(0)
				
				// stack = [parseList] -> []
				fun parseListLike(
					index: Int,
					container: Class<*>,
					createContainer: () -> Unit,
					parseItem: (contextIndex: Int, itemType: java.lang.reflect.Type) -> Unit,
				) {
					val containerName = container.simpleName
					
					val signature = parameterized()
					check(signature is ParameterizedType && signature.rawType == container) {
						"parameters[$index]: should declare like '$containerName<T>'"
					}
					
					val innerType = signature.actualTypeArguments.single()
					
					visitFlushFrameLocals(
						locals = localFrames.toTypedArray(),
						delta = index - previousLocalFlushIndex
					)
					previousLocalFlushIndex = index
					
					val containerIndex = index
					val parseListIndex = index + 1
					
					createContainer()
					visitVarInsn(ASTORE, containerIndex)
					
					visitVarInsn(ALOAD, contextIndex)
					visitInvokeContextInsn("list", "()${C.Parse.parseList}")
					visitVarInsn(ASTORE, parseListIndex)
					
					visitFrame(F_APPEND, 2, arrayOf("Ljava/util/List;", C.Parse.parseList.descriptor), 0, null)
					
					/// <- while(list.hasNext()) parseItem(list)
					/// :parse
					val parseLabel = Label()
					val breakLabel = Label()
					visitLabel(parseLabel)
					visitFrame(F_SAME, 0, null, 0, null)
					
					/// if(!list.hasNext()) goto :breakLabel
					visitVarInsn(ALOAD, parseListIndex)
					visitMethodInsn(INVOKEVIRTUAL, C.Parse.parseList.internalName, "hasNext", "()Z", false)
					visitJumpInsn(IFEQ, breakLabel)
					
					visitVarInsn(ALOAD, containerIndex)
					parseItem(contextIndex, innerType)
					visitJumpInsn(GOTO, parseLabel)
					
					visitLabel(breakLabel)
					visitLocalVariable(
						"\$list_${parseListIndex}",
						C.Parse.parseList.descriptor,
						null,
						parseLabel,
						breakLabel,
						parseListIndex
					)
					visitFrame(F_CHOP, 2, null, 0, null)
				}
				
				when {
					/// context.<type>()
					type.isPrimitive -> {
						visitVarInsn(ALOAD, contextIndex)
						when(type) {
							Boolean::class.java -> visitInvokeContextInsn("boolean", "()Z")
							Byte::class.java -> visitInvokeContextInsn("byte", "()B")
							Short::class.java -> visitInvokeContextInsn("short", "()S")
							Int::class.java -> visitInvokeContextInsn("int", "()I")
							Long::class.java -> visitInvokeContextInsn("long", "()J")
							Float::class.java -> visitInvokeContextInsn("float", "()F")
							Double::class.java -> visitInvokeContextInsn("double", "()D")
							Char::class.java -> visitInvokeContextInsn("char", "()C")
							else -> throw NoWhenBranchMatchedException("unknown primitive type $type")
						}
					}
					
					type in Boxers.keys -> {
						visitVarInsn(ALOAD, contextIndex)
						when(type) {
							Boolean::class.javaObjectType -> visitInvokeContextInsn("boolean", "()Z")
							Byte::class.javaObjectType -> visitInvokeContextInsn("byte", "()B")
							Short::class.javaObjectType -> visitInvokeContextInsn("short", "()S")
							Int::class.javaObjectType -> visitInvokeContextInsn("int", "()I")
							Long::class.javaObjectType -> visitInvokeContextInsn("long", "()J")
							Float::class.javaObjectType -> visitInvokeContextInsn("float", "()F")
							Double::class.javaObjectType -> visitInvokeContextInsn("double", "()D")
							Char::class.javaObjectType -> visitInvokeContextInsn("char", "()C")
							else -> throw NoWhenBranchMatchedException("unknown primitive type $type")
						}
						Boxers[type]!!.box(this)
					}
					
					type == String::class.java -> {
						visitVarInsn(ALOAD, contextIndex)
						visitInvokeContextInsn("string", "()Ljava/lang/String;")
					}
					
					type == List::class.java -> {
						parseListLike(
							index = localIndex,
							container = List::class.java,
							createContainer = {
								visitMethodInsn(
									INVOKESTATIC,
									C.runtime.internalName,
									"list",
									"()Ljava/util/List;",
									false
								)
							},
							parseItem = { contextIndex, itemType ->
								parseValue(
									contextIndex,
									itemType.toClass(),
									parameterized = { itemType },
									stack = stack + 1,
									local = local + 2
								)
								visitMethodInsn(INVOKEVIRTUAL, "java/util/List", "add", "(Ljava/lang/Object;)Z", false)
								visitInsn(POP)
							},
						)
					}
					
					type.isAnnotationPresent(ComputerApi::class.java) -> {
						visitVarInsn(ALOAD, contextIndex)
						val argType = proxyType(type)
						visitFieldInsn(GETSTATIC, argType.internalName, ProxyInstanceName, argType.descriptor)
						visitInvokeContextInsn("apiInterface", "(${C.Item.obj})Ljava/lang/Object;")
					}
					
					else -> throw IllegalStateException("unsupported argument type $type for ${index}th parameter of $fn")
				}
			}
			
			parseValue(
				contextIndex,
				type,
				parameterized = { argument.parameterizedType },
				stack = 0,
				local = localIndex
			)
			
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
		visitFlushFrameLocals(locals = localFrames.toTypedArray(), delta = parameters.size - previousLocalFlushIndex)
		previousLocalFlushIndex = parameters.size
		maxLocals = max(maxLocals, localIndex)
		
		val afterSkipCurrentLabel = Label()
		visitVarInsn(ALOAD, contextIndex)
		visitInvokeContextInsn("skipCurrent", "()${C.varargs}")
		
		visitInsn(DUP)
		visitJumpInsn(IFNULL, afterSkipCurrentLabel)
		visitInsn(ARETURN)
		
		visitLabel(afterSkipCurrentLabel)
		visitFrame(F_SAME1, 0, null, 1, arrayOf(C.varargs.internalName))
		visitInsn(POP)
		
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
			visitVarInsn(ALOAD, contextIndex) // stack = [result, context]
			visitInsn(SWAP) // stack = [context, result]
			if(method.returnType.isPrimitive) {
				Boxers[method.returnType]!!.box(this)
			}
			visitInvokeContextInsn("wrapReturn", "(Ljava/lang/Object;)${C.varargs}")
			visitInsn(ARETURN)
		} else {
			visitVarInsn(ALOAD, contextIndex)
			visitInvokeContextInsn("wrapReturnVoid", "()${C.varargs}")
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
		in Byte.MIN_VALUE..Byte.MAX_VALUE -> visitIntInsn(BIPUSH, value)
		in Short.MIN_VALUE..Short.MAX_VALUE -> visitIntInsn(BIPUSH, value)
		else -> visitLdcInsn(value)
	}
}

private abstract class Boxer {
	abstract fun box(visitor: MethodVisitor)
	abstract fun unbox(visitor: MethodVisitor)
}

private val Boxers = buildMap {
	listOf(
		Boolean::class,
		Byte::class, Short::class, Int::class, Long::class,
		Float::class, Double::class,
		Char::class,
	).forEach { type ->
		val java = type.java
		val objectType = type.javaObjectType
		val asmObject = Type.getType(objectType)
		val boxDescriptor = "(${Type.getDescriptor(java)})${asmObject.descriptor}"
		
		val boxer = object : Boxer() {
			override fun box(visitor: MethodVisitor) {
				visitor.visitMethodInsn(INVOKESTATIC, asmObject.internalName, "valueOf", boxDescriptor, false)
			}
			
			override fun unbox(visitor: MethodVisitor) {
				visitor.visitMethodInsn(
					INVOKEVIRTUAL,
					asmObject.internalName,
					"${java.name}Value",
					"()${Type.getDescriptor(java)}",
					false
				)
			}
		}
		
		put(type.java, boxer)
		put(objectType, boxer)
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
		delta < 0 -> visitFrame(F_CHOP, delta, null, 0, null)
		else -> visitFrame(F_SAME, 0, null, 0, null)
	}
}

private fun java.lang.reflect.Type.toClass(): Class<*> = when(this) {
	is Class<*> -> this
	is GenericArrayType -> genericComponentType.toClass().arrayType()
	is ParameterizedType -> rawType.toClass()
	is WildcardType -> Any::class.java
	is TypeVariable<*> -> {
		val boundClass = bounds.map { it.toClass() }
		boundClass.firstOrNull { !it.isInterface } ?: boundClass.firstOrNull() ?: Any::class.java
	}
	
	else -> error("?")
}
