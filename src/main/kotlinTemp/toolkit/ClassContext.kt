package com.lhwdev.asm.toolkit

import com.lhwdev.asm.toolkit.descriptor.ClassDescriptor
import com.lhwdev.asm.toolkit.descriptor.FieldHandle
import com.lhwdev.asm.toolkit.descriptor.MethodDescriptor


@ContextMarker
interface ClassContext {
	fun define(access: Int, name: String, superType: Type): ClassDescriptor
	
	fun implements(interfaceType: Type)
	
	
	fun addField(access: Int, name: String, type: Type, block: context(FieldContext) () -> Unit = {}): FieldHandle?
	
	
	fun addMethod(descriptor: MethodInfo? = null, block: context(MethodContext) () -> Unit): MethodDescriptor?
	
	fun addMethod(access: Int, name: String, block: context(MethodContext) () -> Unit): MethodDescriptor?
	
	
	fun addConstructor(access: Int, block: context(MethodContext) () -> Unit): MethodDescriptor?
	
	fun endClass(): ClassDescriptor
}

