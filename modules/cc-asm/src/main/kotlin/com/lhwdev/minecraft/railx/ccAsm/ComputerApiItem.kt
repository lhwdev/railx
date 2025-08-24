package com.lhwdev.minecraft.railx.ccAsm

import org.squiddev.cobalt.Varargs
import java.lang.invoke.MethodHandle


sealed class ComputerApiItem {
	sealed class Named : ComputerApiItem() {
		abstract val name: String
	}
	
	class Function(
		override val name: String,
		private val handle: MethodHandle,
	) : Named() {
		fun invoke(self: Any, context: InvokeContext): Varargs =
			handle.invoke(self, context) as Varargs
		
		override fun toString(): String =
			"ComputerApi.Function(name=$name, handle=$handle)"
	}
	
	abstract class Object<T : Any> : ComputerApiItem() {
		abstract val type: Class<T>
		
		abstract val items: List<Named>
		
		
		override fun toString(): String =
			"ComputerApiItem.Object(items=${items.joinToString()})"
	}
}
