package com.lhwdev.minecraft.railx.ccAsm


object ComputerApiRuntime {
	@JvmStatic
	fun apiItems(items: Array<ComputerApiItem>): List<ComputerApiItem> =
		listOf(*items)
}