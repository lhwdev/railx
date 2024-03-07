package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.Variable
import com.lhwdev.asm.toolkit.descriptor.FieldHandle
import com.lhwdev.asm.toolkit.descriptor.StaticFieldHandle

interface LoadContext {
	fun pushFromVariable(variable: Variable): StackValue
	
	fun pushFromStaticField(handle: StaticFieldHandle): StackValue
	
	fun pushFromField(handle: FieldHandle, instance: StackValue): StackValue
}
