package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type


interface Value {
	val type: Type
	
	context(CodeContext)
	fun push(): StackValue
}

interface MutableValue : Value {
	context(CodeContext)
	var value: Value
}

context(CodeContext)
fun Value.toStack(): StackValue = when(this) {
	is StackValue -> this
	else -> push()
}

context(CodeContext)
fun Value.toStackTop(): StackValue = when(this) {
	is StackValue -> ensureTop()
	else -> push()
}
