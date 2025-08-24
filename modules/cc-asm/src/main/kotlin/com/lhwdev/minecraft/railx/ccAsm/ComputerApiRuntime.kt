package com.lhwdev.minecraft.railx.ccAsm


@Suppress("unused")
object ComputerApiRuntime {
	@JvmStatic
	fun namedApiItems(items: Array<ComputerApiItem.Named>): List<ComputerApiItem.Named> =
		listOf(*items)
	
	@JvmStatic
	fun <T> list(): MutableList<T> =
		mutableListOf()
}
