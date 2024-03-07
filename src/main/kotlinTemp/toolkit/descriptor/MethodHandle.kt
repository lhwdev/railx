package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.value.StackValue
import com.lhwdev.asm.toolkit.value.Value
import com.lhwdev.asm.toolkit.value.invoke


abstract class MethodHandle : MethodDescriptor {
	protected abstract val instance: Value
	
	context(CodeContext)
	operator fun invoke(vararg arguments: Value): StackValue =
		invoke(instance = instance, arguments = arguments)
}
