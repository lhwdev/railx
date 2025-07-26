package com.lhwdev.minecraft.railx.ccAsm


abstract class InvokeContext {
	abstract fun argumentsCount(count: Int)
	abstract fun argumentsCount(minCount: Int, maxCount: Int)
}
