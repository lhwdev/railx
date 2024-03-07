package com.lhwdev.minecraft.railx.peripherals.display.target

import com.google.gson.Gson
import com.lhwdev.minecraft.railx.RailX
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IDynamicPeripheral
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import java.util.*

class ComputerizedDisplayTargetPeripheral(var parent: ComputerizedDisplayTargetBlockEntity) : IDynamicPeripheral {
	var computers: MutableList<IComputerAccess> = LinkedList()
	override fun attach(computer: IComputerAccess) {
		computers.add(computer)
	}
	
	override fun detach(computer: IComputerAccess) {
		computers.remove(computer)
	}
	
	override fun getMethodNames(): Array<String> {
		return arrayOf("setWidth", "getWidth", "setHeight", "getHeight")
	}
	
	@Throws(LuaException::class)
	override fun callMethod(
		iComputerAccess: IComputerAccess,
		iLuaContext: ILuaContext,
		i: Int,
		iArguments: IArguments,
	): MethodResult {
		when(i) {
			0 -> {
				run {
					val width = iArguments.getInt(0)
					parent.maxWidth = width
				}
				run { return MethodResult.of(parent.maxWidth) }
			}
			
			1 -> {
				return MethodResult.of(parent.maxWidth)
			}
			
			2 -> {
				run {
					val height = iArguments.getInt(0)
					parent.maxHeight = height
				}
				run { return MethodResult.of(parent.maxHeight) }
			}
			
			3 -> {
				return MethodResult.of(parent.maxHeight)
			}
		}
		return MethodResult.of()
	}
	
	override fun getType(): String {
		return ResourceLocation(RailX.modId, "computerized_display_target").toString()
	}
	
	override fun equals(iPeripheral: IPeripheral?): Boolean {
		return false
	}
	
	var gson = Gson()
	fun acceptText(line: Int, list: List<MutableComponent>, displayLinkContext: DisplayLinkContext) {
		val map: MutableMap<Double, Any> = HashMap()
		for(i in list.indices) {
			map[i.toDouble()] =
				gson.fromJson<Map<*, *>>(
					Component.Serializer.toJson(list[i]),
					MutableMap::class.java
				) // this code is terrific
		}
		computers.forEach {
			it.queueEvent(
				"display_link_data",
				displayLinkContext.blockEntity().activeSource.id.toString(),
				displayLinkContext.sourcePos.x,
				displayLinkContext.sourcePos.y,
				displayLinkContext.sourcePos.z,
				line,
				map
			)
		}
	}
}
