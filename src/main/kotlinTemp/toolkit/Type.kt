// Modified from org.ow2.asm:asm:9.5

// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE", "unused")

package com.lhwdev.asm.toolkit

import com.lhwdev.asm.toolkit.descriptor.ClassDescriptor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod
import org.objectweb.asm.Type as AsmType


enum class InstructionType {
	Int {
		override fun getOpcode(opcode: kotlin.Int) = opcode
	},
	Float {
		override fun getOpcode(opcode: kotlin.Int) =
			opcode + (Opcodes.FRETURN - Opcodes.IRETURN)
	},
	Long {
		override fun getOpcode(opcode: kotlin.Int) =
			opcode + (Opcodes.LRETURN - Opcodes.IRETURN)
	},
	Double {
		override fun getOpcode(opcode: kotlin.Int) =
			opcode + (Opcodes.DRETURN - Opcodes.IRETURN)
	},
	Reference {
		override fun getOpcode(opcode: kotlin.Int): kotlin.Int {
			if(opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
				throw UnsupportedOperationException()
			}
			return opcode + (Opcodes.ARETURN - Opcodes.IRETURN)
		}
	},
	Void {
		override fun getOpcode(opcode: kotlin.Int): kotlin.Int {
			if(opcode != Opcodes.IRETURN) {
				throw UnsupportedOperationException()
			}
			return Opcodes.RETURN
		}
	};
	
	abstract fun getOpcode(opcode: kotlin.Int): kotlin.Int
}


fun Type(asmType: AsmType): Type =
	Type(asmType as Any)

fun Type(descriptor: ClassDescriptor): Type =
	Type(descriptor as Any)


/**
 * A Java field or method type. This class can be used to make it easier to manipulate type and
 * method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
@JvmInline
value class Type(@PublishedApi internal val backing: Any) {
	inline val asm: AsmType
		get() = if(backing is AsmType) backing else asmInternal
	
	@PublishedApi
	internal val asmInternal: AsmType
		get() = when(backing) {
			is ClassDescriptor -> backing.asmType
			else -> error("unexpected backing $backing")
		}
	
	
	/**
	 * The type of the elements of this array type. This method should only be used for an
	 * array type.
	 */
	inline val elementType: Type
		get() = Type(asm.elementType)
	
	val instructionType: InstructionType
		get() = when(this) {
			INT_TYPE, BYTE_TYPE, SHORT_TYPE, BOOLEAN_TYPE, CHAR_TYPE -> InstructionType.Int
			LONG_TYPE -> InstructionType.Long
			FLOAT_TYPE -> InstructionType.Float
			DOUBLE_TYPE -> InstructionType.Double
			VOID_TYPE -> InstructionType.Void
			else -> InstructionType.Reference
		}
	
	/**
	 * The argument types of methods of this type. This method should only be used for method types.
	 */
	inline val argumentTypes: Array<out Type>
		get() = asm.argumentTypes.mapArray { Type(it) }
	
	/**
	 * Returns the return type of methods of this type. This method should only be used for method
	 * types.
	 *
	 * @return the return type of methods of this type.
	 */
	inline val returnType: Type
		get() = Type(asm.returnType)
	
	
	/**
	 * The binary name of the class corresponding to this type. This method must not be used
	 * on method types.
	 */
	inline val className: String
		get() = asm.className
	
	/**
	 * The internal name of the class corresponding to this object or array type. The internal
	 * name of a class is its fully qualified name (as returned by Class.getName(), where '.' are
	 * replaced by '/'). This method should only be used for an object or array type.
	 */
	inline val internalName: String
		get() = asm.internalName
	
	/**
	 * The descriptor corresponding to this type.
	 */
	inline val descriptor: String
		get() = asm.descriptor
	
	/**
	 * The sort of this type.
	 */
	inline val sort: Int
		get() = asm.sort
	
	/**
	 * The number of dimensions of this array type. This method should only be used for an
	 * array type.
	 */
	inline val dimensions: Int
		get() = asm.dimensions
	
	/**
	 * The size of values of this type, i.e., 2 for `long` and `double`, 0 for
	 * `void` and 1 otherwise. This method must not be used for method types.
	 */
	inline val size: Int
		get() = asm.size
	
	/**
	 * The size of the arguments and of the return value of methods of this type. This method
	 * should only be used for method types.
	 *
	 * @return the size of the arguments of the method (plus one for the implicit this argument),
	 * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
	 * `(argumentsSize &lt;&lt; 2) | returnSize` (argumentsSize is therefore equal to `i &gt;&gt; 2`, and returnSize to `i &amp; 0x03`).
	 */
	inline val argumentsAndReturnSizes: Int
		get() = asm.argumentsAndReturnSizes
	
	/**
	 * Returns a JVM instruction opcode adapted to this [Type]. This method must not be used for
	 * method types.
	 *
	 * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD,
	 * IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and
	 * IRETURN.
	 * @return an opcode that is similar to the given opcode, but adapted to this [Type]. For
	 * example, if this type is `float` and `opcode` is IRETURN, this method returns
	 * FRETURN.
	 */
	inline fun getOpcode(opcode: Int): Int =
		asm.getOpcode(opcode)
	
	/**
	 * Returns a string representation of this type.
	 *
	 * @return the descriptor of this type.
	 */
	override inline fun toString(): String =
		asm.toString()
	
	companion object {
		/** The sort of the `void` type. See [.getSort].  */
		const val VOID = 0
		
		/** The sort of the `boolean` type. See [.getSort].  */
		const val BOOLEAN = 1
		
		/** The sort of the `char` type. See [.getSort].  */
		const val CHAR = 2
		
		/** The sort of the `byte` type. See [.getSort].  */
		const val BYTE = 3
		
		/** The sort of the `short` type. See [.getSort].  */
		const val SHORT = 4
		
		/** The sort of the `int` type. See [.getSort].  */
		const val INT = 5
		
		/** The sort of the `float` type. See [.getSort].  */
		const val FLOAT = 6
		
		/** The sort of the `long` type. See [.getSort].  */
		const val LONG = 7
		
		/** The sort of the `double` type. See [.getSort].  */
		const val DOUBLE = 8
		
		/** The sort of array reference types. See [.getSort].  */
		const val ARRAY = 9
		
		/** The sort of object reference types. See [.getSort].  */
		const val OBJECT = 10
		
		/** The sort of method types. See [.getSort].  */
		const val METHOD = 11
		
		/** The (private) sort of object reference types represented with an internal name.  */
		private const val INTERNAL = 12
		
		/** The `void` type.  */
		val VOID_TYPE: Type = Type(AsmType.VOID_TYPE)
		
		/** The `boolean` type.  */
		val BOOLEAN_TYPE: Type = Type(AsmType.BOOLEAN_TYPE)
		
		/** The `char` type.  */
		val CHAR_TYPE: Type = Type(AsmType.CHAR_TYPE)
		
		/** The `byte` type.  */
		val BYTE_TYPE: Type = Type(AsmType.BYTE_TYPE)
		
		/** The `short` type.  */
		val SHORT_TYPE: Type = Type(AsmType.VOID_TYPE)
		
		/** The `int` type.  */
		val INT_TYPE: Type = Type(AsmType.VOID_TYPE)
		
		/** The `float` type.  */
		val FLOAT_TYPE: Type = Type(AsmType.FLOAT_TYPE)
		
		/** The `long` type.  */
		val LONG_TYPE: Type = Type(AsmType.LONG_TYPE)
		
		/** The `double` type.  */
		val DOUBLE_TYPE: Type = Type(AsmType.DOUBLE_TYPE)
		
		val AnyType: Type = Type(AsmType.getType("Ljava/lang/Object;"))
		
		val ClassType: Type = Type(AsmType.getType("Ljava/lang/Class;"))
		
		val StringType: Type = Type(AsmType.getType("Ljava/lang/String;"))
		
		val NothingType: Type = Type(AsmType.getType("Lkotlin/Nothing;"))
		
		/*
		 * Returns the [Type] corresponding to the given type descriptor.
		 *
		 * @param typeDescriptor a field or method type descriptor.
		 * @return the [Type] corresponding to the given type descriptor.
		 */
		inline fun getType(typeDescriptor: String): Type =
			Type(AsmType.getType(typeDescriptor))
		
		/**
		 * Returns the [Type] corresponding to the given class.
		 *
		 * @param klass a class.
		 * @return the [Type] corresponding to the given class.
		 */
		inline fun getType(klass: Class<*>): Type =
			Type(AsmType.getType(klass))
		
		inline fun getType(klass: KClass<*>): Type =
			getType(klass.java)
		
		/**
		 * Returns the method [Type] corresponding to the given constructor.
		 *
		 * @param constructor a [Constructor] object.
		 * @return the method [Type] corresponding to the given constructor.
		 */
		inline fun getType(constructor: Constructor<*>): Type =
			Type(AsmType.getType(constructor))
		
		/**
		 * Returns the method [Type] corresponding to the given method.
		 *
		 * @param method a [Method] object.
		 * @return the method [Type] corresponding to the given method.
		 */
		inline fun getType(method: Method): Type =
			Type(AsmType.getType(method))
		
		inline fun getType(function: KFunction<*>): Type =
			function.javaMethod?.let { getType(it) } ?: getType(function.javaConstructor!!)
		
		/**
		 * Returns the [Type] corresponding to the given internal name.
		 *
		 * @param internalName an internal name (see [Type.getInternalName]).
		 * @return the [Type] corresponding to the given internal name.
		 */
		inline fun getObjectType(internalName: String): Type =
			Type(AsmType.getObjectType(internalName))
		
		/**
		 * Returns the [Type] corresponding to the given method descriptor. Equivalent to `
		 * Type.getType(methodDescriptor)`.
		 *
		 * @param methodDescriptor a method descriptor.
		 * @return the [Type] corresponding to the given method descriptor.
		 */
		inline fun getMethodType(methodDescriptor: String): Type =
			Type(AsmType.getMethodType(methodDescriptor))
		
		/**
		 * Returns the [Type] values corresponding to the argument types of the given method
		 * descriptor.
		 *
		 * @param methodDescriptor a method descriptor.
		 * @return the [Type] values corresponding to the argument types of the given method
		 * descriptor.
		 */
		inline fun getArgumentTypes(methodDescriptor: String): Array<out Type> =
			AsmType.getArgumentTypes(methodDescriptor).mapArray { Type(it) }
		
		/**
		 * Returns the [Type] values corresponding to the argument types of the given method.
		 *
		 * @param method a method.
		 * @return the [Type] values corresponding to the argument types of the given method.
		 */
		inline fun getArgumentTypes(method: Method): Array<out Type> =
			AsmType.getArgumentTypes(method).mapArray { Type(it) }
		
		/**
		 * Returns the [Type] corresponding to the return type of the given method descriptor.
		 *
		 * @param methodDescriptor a method descriptor.
		 * @return the [Type] corresponding to the return type of the given method descriptor.
		 */
		inline fun getReturnType(methodDescriptor: String): Type =
			Type(AsmType.getReturnType(methodDescriptor))
		
		/**
		 * Returns the [Type] corresponding to the return type of the given method.
		 *
		 * @param method a method.
		 * @return the [Type] corresponding to the return type of the given method.
		 */
		inline fun getReturnType(method: Method): Type =
			Type(AsmType.getReturnType(method))
		
		
		/**
		 * Returns the internal name of the given class. The internal name of a class is its fully
		 * qualified name, as returned by Class.getName(), where '.' are replaced by '/'.
		 *
		 * @param klass an object or array class.
		 * @return the internal name of the given class.
		 */
		inline fun getInternalName(klass: Class<*>): String =
			AsmType.getInternalName(klass)
		
		inline fun getInternalName(klass: KClass<*>): String =
			getInternalName(klass.java)
		
		/**
		 * Returns the descriptor corresponding to the given class.
		 *
		 * @param klass an object class, a primitive class or an array class.
		 * @return the descriptor corresponding to the given class.
		 */
		inline fun getDescriptor(klass: Class<*>): String =
			AsmType.getDescriptor(klass)
		
		inline fun getDescriptor(klass: KClass<*>): String =
			getDescriptor(klass.java)
		
		/**
		 * Returns the descriptor corresponding to the given constructor.
		 *
		 * @param constructor a [Constructor] object.
		 * @return the descriptor of the given constructor.
		 */
		inline fun getConstructorDescriptor(constructor: Constructor<*>): String =
			AsmType.getConstructorDescriptor(constructor)
		
		/**
		 * Returns the descriptor corresponding to the given method.
		 *
		 * @param method a [Method] object.
		 * @return the descriptor of the given method.
		 */
		inline fun getMethodDescriptor(method: Method): String =
			AsmType.getMethodDescriptor(method)
		
		inline fun getMethodDescriptor(function: KFunction<*>): String =
			getMethodDescriptor(function.javaMethod!!)
		
		/**
		 * Computes the size of the arguments and of the return value of a method.
		 *
		 * @param methodDescriptor a method descriptor.
		 * @return the size of the arguments of the method (plus one for the implicit this argument),
		 * argumentsSize, and the size of its return value, returnSize, packed into a single int i =
		 * `(argumentsSize &lt;&lt; 2) | returnSize` (argumentsSize is therefore equal to `i &gt;&gt; 2`, and returnSize to `i &amp; 0x03`).
		 */
		inline fun getArgumentsAndReturnSizes(methodDescriptor: String): Int =
			AsmType.getArgumentsAndReturnSizes(methodDescriptor)
	}
}


@PublishedApi
internal inline fun <T, reified R> Array<out T>.mapArray(block: (T) -> R): Array<out R> =
	Array(size) { block(get(it)) }
