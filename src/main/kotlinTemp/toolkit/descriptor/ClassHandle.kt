package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.value.Value


interface ClassHandle : ClassDescriptor {
	operator fun get(descriptor: FieldDescriptor): FieldHandle
	
	operator fun get(descriptor: MethodDescriptor): MethodHandle
}


fun ClassDescriptor.handle(instance: Value): ClassHandle = object : ClassHandle, ClassDescriptor by this {
	override fun get(descriptor: FieldDescriptor): FieldHandle =
		object : FieldHandle(), FieldDescriptor by descriptor {
			init { require(kind == FieldDescriptor.Kind.Instance) }
			override val instance: Value = instance
		}
	
	override fun get(descriptor: MethodDescriptor): MethodHandle =
		object : MethodHandle(), MethodDescriptor by descriptor {
			override val instance: Value = instance
		}
}
