package com.lhwdev.minecraft.railx.api.peripheral

import com.lhwdev.minecraft.railx.RailX
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.resources.ResourceLocation
import java.util.*


abstract class PeripheralBase : Peripheral {
	abstract val peripheralName: String
	
	
	protected val computers = LinkedList<IComputerAccess>()
	
	override fun attach(computer: IComputerAccess) {
		computers += computer
	}
	
	override fun detach(computer: IComputerAccess) {
		computers -= computers
	}
	
	override fun getType(): String =
		ResourceLocation(RailX.modId, peripheralName).toString()
	
	
	override fun equals(other: IPeripheral?): Boolean = this === other
	
	override fun equals(other: Any?): Boolean = this === other
	
	override fun hashCode(): Int = super.hashCode()
}
