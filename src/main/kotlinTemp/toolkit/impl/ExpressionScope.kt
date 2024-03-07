package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.value.StackValue
import com.lhwdev.asm.toolkit.value.ensureTop


class ExpressionScope {
	private val values = ArrayList<StackValue>()
	
	fun parameter(value: StackValue) {
		values += value
	}
	
	fun parameter(vararg values: StackValue) {
		this.values += values
	}
	
	context(CodeContext)
	fun build(): Int {
		val count = values.size
		values.clear()
		if(count == 0) return 0
		
		for(i in 0..<values.size - 1) {
			require(values[i + 1].isAfter(values[i])) { "parameters are not adjacent" }
		}
		values.last().ensureTop()
		return count
	}
}
