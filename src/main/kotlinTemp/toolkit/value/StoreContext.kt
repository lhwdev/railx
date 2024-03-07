package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.Variable
import com.lhwdev.asm.toolkit.descriptor.FieldHandle
import com.lhwdev.asm.toolkit.descriptor.StaticFieldHandle


interface StoreContext {
	fun storeVariable(from: StackValue, to: Variable)
	
	fun storeStaticField(handle: StaticFieldHandle, from: StackValue)
	
	fun storeField(handle: FieldHandle, instance: StackValue, from: StackValue)
}
