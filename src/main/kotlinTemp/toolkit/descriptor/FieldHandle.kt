package com.lhwdev.asm.toolkit.descriptor

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.value.MutableStoredValue
import com.lhwdev.asm.toolkit.value.StackValue
import com.lhwdev.asm.toolkit.value.Value
import com.lhwdev.asm.toolkit.value.toStack


abstract class FieldHandle : MutableStoredValue, FieldDescriptor {
	protected abstract val instance: Value
	
	override val type: Type
		get() = Type.getType(fieldType)
	
	context(CodeContext)
	override var value: Value
		get() = push()
		set(value) {
			storeContext.storeField(handle = this, instance = instance.toStack(), from = value.toStack())
		}
	
	context(CodeContext)
	override fun push(): StackValue =
		loadContext.pushFromField(handle = this, instance = instance.toStack())
}


class StaticFieldHandle(descriptor: FieldDescriptor) : MutableStoredValue, FieldDescriptor by descriptor {
	override val type: Type
		get() = Type.getType(fieldType)
	
	context(CodeContext)
	override var value: Value
		get() = push()
		set(value) {
			storeContext.storeStaticField(this, value.toStack())
		}
	
	context(CodeContext) override fun push(): StackValue =
		loadContext.pushFromStaticField(this)
}
