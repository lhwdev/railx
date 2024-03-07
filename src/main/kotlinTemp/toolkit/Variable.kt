package com.lhwdev.asm.toolkit

import com.lhwdev.asm.toolkit.value.*


interface Variable : MutableStoredValue {
	context(CodeContext)
	override var value: Value
		get() = push()
		set(value) {
			storeContext.storeVariable(from = value.toStack(), to = this)
		}
	
	context(CodeContext)
	override fun push(): StackValue =
		loadContext.pushFromVariable(this)
}


context(CodeContext)
fun variable(klass: Class<*>, initialValue: Value): Variable =
	variable(Type.getType(klass), initialValue.toStack())

context(CodeContext)
fun variable(initialValue: Value): Variable =
	variable(initialValue.type, initialValue.toStack())

context(CodeContext)
fun variable(type: Type, initialValue: Value): Variable =
	variable(type, initialValue.toStack())


context(CodeContext)
fun variable(initialValue: Int): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Boolean): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Byte): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Short): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Long): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Float): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Double): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: Char): Variable = variable(constant(initialValue))

context(CodeContext)
fun variable(initialValue: String): Variable = variable(constant(initialValue))

context(CodeContext)
inline fun <reified T> variable(initialValue: Nothing?): Variable =
	variable(Type.getType(T::class.java), constant(initialValue))

context(CodeContext)
fun variable(type: Type, initialValue: Nothing?): Variable =
	variable(type, constant(initialValue))
