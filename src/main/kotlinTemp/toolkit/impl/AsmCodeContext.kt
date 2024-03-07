package com.lhwdev.asm.toolkit.impl

import com.lhwdev.asm.toolkit.*
import com.lhwdev.asm.toolkit.descriptor.ClassDescriptor
import com.lhwdev.asm.toolkit.descriptor.MethodDescriptor
import com.lhwdev.asm.toolkit.impl.tree.ExpressionNode
import com.lhwdev.asm.toolkit.impl.tree.Nodes
import com.lhwdev.asm.toolkit.value.LoadContext
import com.lhwdev.asm.toolkit.value.StackValue
import com.lhwdev.asm.toolkit.value.StoreContext
import com.lhwdev.asm.toolkit.value.ensureTop
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


private object DummyStackValue : StackValue {
	override val type: Type
		get() = Type.VOID_TYPE
	
	override fun isAfter(previous: StackValue): Boolean = false
}


abstract class AsmCodeContext : CodeContext {
	abstract val output: MethodVisitor
	
	private var scope = ScopeImpl(parent = null)
	
	
	/// Scopes
	
	override fun pushScope(): CodeContext.Scope = scope.also { parent ->
		scope = ScopeImpl(parent)
	}
	
	override fun popScope(previous: CodeContext.Scope) {
		val child = scope
		require(child.parent === previous) { "popScope not balanced" }
		child.end()
		scope = previous as ScopeImpl
	}
	
	override fun variable(type: Type, initialValue: StackValue): Variable {
		val variable = scope.variable(type)
		this.statement {
			parameter(initialValue)
			output.visitVarInsn(type.getOpcode(ISTORE), variable.index)
		}
		return variable
	}
	
	
	/// Stacks
	
	@Suppress("LeakingThis")
	val stack = StackImpl(this)
	
	@Suppress("LeakingThis")
	val ops = AsmCodeOps(this)
	
	override val stackTop: StackValue
		get() = stack.top
	
	@PublishedApi
	internal val expressionScope = ExpressionScope()
	
	inline fun expression(block: ExpressionScope.() -> ExpressionNode): StackValue {
		val scope = expressionScope
		val node = scope.block()
		return stack.push(node, parameters = scope.build())
	}
	
	
	override fun pushStackFrame(parameters: Int, results: Int): StackFrame =
		stack.stackFrame.also { stack.stackFrame = StackImpl.Frame(start = parameters, results = results) }
	
	override fun popStackFrame(previous: StackFrame) {
		stack.stackFrame = previous as StackImpl.Frame
	}
	
	
	override val loadContext: LoadContext
		get() = ops
	
	override fun commitPush(value: StackValue) {
		value.ensureTop()
		stack.commitPush()
	}
	
	override fun push(constant: Nothing?): StackValue =
		expression { Nodes.Constant.NullConstant }
	
	override fun push(constant: Int): StackValue =
		expression { Nodes.Constant.IntConstant(constant) }
	
	override fun push(constant: Long): StackValue =
		expression { Nodes.Constant.LongConstant(constant) }
	
	override fun push(constant: Float): StackValue =
		expression { Nodes.Constant.FloatConstant(constant) }
	
	override fun push(constant: Double): StackValue  =
		expression { Nodes.Constant.DoubleConstant(constant) }
	
	override fun push(constant: String): StackValue  =
		expression { Nodes.Constant.StringConstant(constant) }
	
	override fun push(constant: Type): StackValue =
		expression { Nodes.Constant.ClassConstant(constant.asm) }
	
	override fun dup(target: StackValue): StackValue {
		target.ensureTop()
		return stack.dup()
	}
	
	override val storeContext: StoreContext
		get() = ops
	
	override fun pop(): StackValue {
		val top = stackTop
		stack.pop()
		return top
	}
	
	override fun popDiscard() {
		stack.commitPush()
		stack.pop()
	}
	
	
	/// Ops
	
	override val instructions: CodeInstructions
		get() = ops
	
	override fun newInstance(klass: ClassDescriptor): StackValue =
		expression(klass.type) { expression.visitTypeInsn(NEW, klass.type.descriptor) }
	
	override fun invoke(descriptor: MethodDescriptor): StackValue = expression(descriptor.returnType) {
		implicitParameter()
		expression.visitMethodInsn(
			descriptor.opcode,
			descriptor.owner.name,
			descriptor.name,
			descriptor.methodType.descriptor,
			descriptor.owner.isInterface,
		)
	}
	
	/// Ops: Flow
	
	override val currentFlowScope: CodeContext.FlowScope
		get() = TODO("Not yet implemented")
	
	override fun pushFlowScope(allowContinue: Boolean, allowBreak: Boolean): CodeContext.FlowScope {
		TODO("Not yet implemented")
	}
	
	override fun popFlowScope(previous: CodeContext.FlowScope) {
		TODO("Not yet implemented")
	}
	
	override fun jumpTo(label: Label) {
		TODO("Not yet implemented")
	}
	
	
	override fun returnVoid() {
		TODO("Not yet implemented")
	}
	
	override fun returnValue(value: StackValue) {
		TODO("Not yet implemented")
	}
	
	override fun throwValue(value: StackValue) {
		TODO("Not yet implemented")
	}
}
