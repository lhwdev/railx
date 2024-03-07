package com.lhwdev.minecraft.railx.peripherals.redstoneLink

import com.lhwdev.minecraft.railx.api.lua.annotations.LuaFunction
import com.lhwdev.minecraft.railx.api.lua.annotations.LuaObject
import com.lhwdev.minecraft.railx.api.peripheral.PeripheralBase
import dan200.computercraft.api.lua.LuaException
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraftforge.registries.ForgeRegistries


private fun getItemByName(name: String): Item {
	val location = ResourceLocation(name)
	val holder = ForgeRegistries.ITEMS.getHolder(location)
	if(holder.isEmpty) throw LuaException("item $name does not exist")
	return holder.get().value()
}


@LuaObject
class ComputerizedRedstoneLinkPeripheral(val behavior: ComputerizedRedstoneLinkBehavior) : PeripheralBase() {
	override val peripheralName get() = "computerized_redstone_link"
	
	
	@LuaFunction
	fun createLink(item1: String, item2: String): RedstoneLinkHandle {
		val forgeItem1 = getItemByName(item1)
		val forgeItem2 = getItemByName(item2)
		
		return behavior.createLink(forgeItem1, forgeItem2)
			?: throw LuaException("maximum concurrent links reached")
	}
	
	@LuaFunction
	fun deleteLink(link: RedstoneLinkHandle) {
		behavior.deleteLink(link)
	}
	
	@LuaFunction
	fun deleteAllLinks() {
		behavior.deleteAllLinks()
	}
}
