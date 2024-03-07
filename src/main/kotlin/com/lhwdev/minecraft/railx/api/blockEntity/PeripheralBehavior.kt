@file:Suppress("LeakingThis")

package com.lhwdev.minecraft.railx.api.blockEntity

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.Direction
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional


abstract class PeripheralBehavior<Peripheral : IPeripheral> {
	abstract fun createPeripheral(): Peripheral
	
	abstract fun willProvidePeripheralToSide(side: Direction): Boolean
	
	
	var lazyPeripheral = LazyOptional.empty<Peripheral>()
	
	fun resolveLazyPeripheral(): LazyOptional<Peripheral> {
		if(!lazyPeripheral.isPresent) {
			lazyPeripheral = LazyOptional.of { createPeripheral() }
		}
		return lazyPeripheral
	}
	
	val peripheral: Peripheral
		get() = lazyPeripheral.resolve().get()
	
	fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T>? {
		if(cap == dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL && side != null) {
			return if(willProvidePeripheralToSide(side)) {
				resolveLazyPeripheral().cast()
			} else {
				LazyOptional.empty()
			}
		}
		
		return null
	}
}
