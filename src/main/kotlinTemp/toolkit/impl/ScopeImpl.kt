package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.CodeContext
import com.lhwdev.asm.toolkit.Type


class ScopeImpl(val parent: ScopeImpl?) : CodeContext.Scope {
	var valid = true
	
	var varIndex: Int = parent?.varIndex ?: 0
	
	val scopeVariables = mutableListOf<VariableImpl>()
	
	
	val variables: Iterable<VariableImpl> = object : Iterable<VariableImpl> {
		override fun iterator(): Iterator<VariableImpl> = object : Iterator<VariableImpl> {
			private var scope = this@ScopeImpl
			private var index = 0
			private var next: VariableImpl? = nextAhead()
			
			private tailrec fun nextAhead(): VariableImpl? {
				val variables = scope.scopeVariables
				return if(index < variables.size) {
					variables[index++]
				} else {
					val parent = scope.parent
					if(parent == null) {
						null
					} else {
						index = 0
						scope = parent
						nextAhead()
					}
				}
			}
			
			override fun hasNext(): Boolean = next != null
			
			override fun next(): VariableImpl = next!!.also { next = nextAhead() }
		}
	}
	
	
	fun variable(type: Type): VariableImpl {
		val variable = VariableImpl(scope = this, index = varIndex, type = type)
		varIndex += type.size
		scopeVariables += variable
		return variable
	}
	
	fun end() {
		valid = false
	}
}
