package com.lhwdev.minecraft.railx.ccAsm

import java.lang.invoke.MethodHandle


sealed class ComputerApiItem {
	class Function(
		val name: String,
		val handle: MethodHandle,
	) : ComputerApiItem()
	
	abstract class Object : ComputerApiItem() {
		abstract val items: List<ComputerApiItem>
	}
}