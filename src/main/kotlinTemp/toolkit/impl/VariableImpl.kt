package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.Type
import com.lhwdev.asm.toolkit.Variable


class VariableImpl(
	val scope: ScopeImpl,
	val index: Int,
	override val type: Type,
) : Variable
