package com.lhwdev.minecraft.railx.peripherals.display.source

import com.lhwdev.minecraft.railx.api.peripheral.PeripheralBase
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import net.minecraft.core.Direction
import java.util.*


class ComputerizedDisplaySourcePeripheral(var source: ComputerizedDisplaySourceBlockEntity) : PeripheralBase() {
	override val peripheralName get() = "computerized_display_source"
	
	
	@LuaFunction
	fun getLink(dir: Optional<String>): DisplayLinkHandle {
		val direction = if(dir.isPresent) {
			Direction.byName(dir.get())
				?: throw LuaException("Specified direction is not up, down, noth, south, west, or east.")
		} else {
			if(source.displayLinks.size != 1) {
				throw LuaException("getLink() without arguments can only be called if there is only one display link attached.")
			}
			source.displayLinks.keys.first()
		}
		if(direction !in source.displayLinks) {
			throw LuaException("Specified direction does not have an attached link.")
		}
		
		return createHandle(direction)
	}
	
	@LuaFunction
	fun getLinkNames(): List<String> {
		return source.displayLinks.keys.map { it.toString() }
	}
	
	var handles: MutableList<DisplayLinkHandle> = ArrayList()
	fun createHandle(dir: Direction): DisplayLinkHandle {
		val handle = DisplayLinkHandle(this, dir)
		handles.add(handle)
		return handle
	}
	
	fun closeHandle(direction: Direction) {
		for(handle in handles) {
			if(handle.direction == direction) {
				handle.open = false
				handles.remove(handle)
			}
		}
	}
	
	fun closeHandle(toDelete: DisplayLinkHandle): Boolean {
		for(handle in handles) {
			if(handle.id === toDelete.id) {
				handle.open = false
				handles.remove(handle)
				return true
			}
		}
		return false
	}
	
	fun addLink(dir: Direction) {
		computers.forEach { it.queueEvent("display_link_added", dir.toString()) }
	}
	
	fun deleteLink(dir: Direction) {
		closeHandle(dir)
		computers.forEach { it.queueEvent("display_link_removed", dir.toString()) }
	}
}
