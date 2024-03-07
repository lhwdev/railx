package com.lhwdev.asm.toolkit.value

import com.lhwdev.asm.toolkit.CodeContext
import org.objectweb.asm.Label


context(CodeContext)
inline fun <R> flowScope(
	allowContinue: Boolean = false,
	allowBreak: Boolean = false,
	block: context(CodeContext) (CodeContext.FlowScope) -> R,
): R {
	val previous = pushFlowScope(allowContinue, allowBreak)
	return try {
		block(this@CodeContext, currentFlowScope)
	} finally {
		popFlowScope(previous)
	}
}

context(CodeContext)
inline fun <R> blockScope(block: context(CodeContext) (CodeContext.FlowScope) -> R): R =
	flowScope(allowBreak = true) { block(this@CodeContext, it) }

context(CodeContext)
inline fun ifThen(condition: Value, block: context(CodeContext) () -> Unit): Unit = blockScope { scope ->
	instructions.jumpIf(condition.toStackTop(), scope.breakLabel)
	block(this@CodeContext)
}

context(CodeContext)
inline fun whenStatement(block: context(CodeContext, WhenStatementContext) () -> Unit) {
	blockScope { scope ->
		val whenContext = WhenStatementContext(scope.breakLabel)
		block(this@CodeContext, whenContext)
	}
}

context(CodeContext)
inline fun loop(block: context(CodeContext) (CodeContext.FlowScope) -> Unit) {
	flowScope { scope ->
		block(this@CodeContext, scope)
		jumpTo(scope.continueLabel)
	}
}


context(CodeContext)
fun returnValue(value: Value) {
	returnValue(value.toStackTop())
}

context(CodeContext)
fun throwValue(value: Value) {
	throwValue(value.toStackTop())
}


context(CodeContext)
class WhenStatementContext(private val end: Label) {
	fun on(condition: Value, block: context(CodeContext) () -> Unit) {
		ifThen(condition) {
			block(this@CodeContext)
			jumpTo(end)
		}
	}
	
	fun default(block: context(CodeContext) () -> Unit) {
		block(this@CodeContext)
	}
}


private inline fun <T, R> context(value: T, block: context(T) () -> R): R = block(value)
