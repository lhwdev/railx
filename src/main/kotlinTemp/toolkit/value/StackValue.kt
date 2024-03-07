package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext


interface StackValue : Value {
	fun isAfter(previous: StackValue): Boolean
	
	context(CodeContext)
	override fun push(): StackValue = this
}


context(CodeContext)
fun <T : StackValue> T.ensureTop(name: String? = null): T = this.also {
	if(it != stackTop) error("expected ${name ?: it} to be top of stack")
}

context(CodeContext)
fun StackValue.dup(): StackValue = dup(this)

context(CodeContext)
fun StackValue.discard() {
	ensureTop()
	popDiscard()
}
