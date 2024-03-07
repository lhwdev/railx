package com.lhwdev.minecraft.railx.peripherals.display.source

import com.google.gson.Gson
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.*

class DisplayLinkHandle(private val parent: ComputerizedDisplaySourcePeripheral, var direction: Direction) {
	var id = UUID.randomUUID()
	var open = true
	
	@Throws(LuaException::class)
	fun checkOpen() {
		if(!open) throw LuaException("Trying to use closed handle.")
	}
	
	var data: ComputerizedDisplaySourceBlockEntity.DisplayData
		@Throws(LuaException::class)
		get() {
			if(direction !in parent.source.displayLinks) {
				close()
				checkOpen()
			}
			return parent.source.displayLinks[direction]!!
		}
		@Throws(LuaException::class)
		set(data) {
			if(direction !in parent.source.displayLinks) {
				close()
				checkOpen()
			}
			parent.source.displayLinks[direction] = data
		}
	
	@LuaFunction
	@Throws(LuaException::class)
	fun close() {
		open = false
		if(!parent.closeHandle(this)) {
			throw RuntimeException("Failed to close handle.")
		}
	}
	
	@get:Throws(LuaException::class)
	val target: DisplayTarget?
		get() {
			val data = data
			return data.blockEntity.activeTarget
		}
	
	@LuaFunction
	@Throws(LuaException::class)
	fun getTargetType(arg: IArguments?): Array<Any?> {
		checkOpen()
		val target = target
		
		return if(target != null) arrayOf(target.id.toString()) else arrayOf(null)
	}
	
	@LuaFunction
	@Throws(LuaException::class)
	fun setText(arg: IArguments) {
		checkOpen()
		val gson = Gson()
		val table = arg.getTable(0)
		val components = mutableListOf<MutableComponent>()
		for((key, value) in table) {
			if(key !is Double) {
				throw LuaException("Invalid table index.")
			}
			if(value !is Map<*, *>) throw LuaException("Table value should be a component (table).")
			try {
				var i = components.size
				while(i < key - 1) {
					components.add(TextDisplaySource.NIL_TEXT)
					i++
				}
				components.add(
					key.toInt() - 1,
					Component.Serializer.fromJson(gson.toJson(value))!!
				)
			} catch(ex: Exception) {
				throw LuaException(ex.message)
			}
		}
		val data = data
		data.toDisplay = components
		this.data = data
	}
	
	@get:Throws(LuaException::class)
	@get:LuaFunction
	val text: Array<Any>
		get() {
			checkOpen()
			val data = data
			val gson = Gson()
			val result: MutableMap<Double, Any> = HashMap()
			for((i, cmp) in data.toDisplay.withIndex()) {
				if(cmp != TextDisplaySource.NIL_TEXT) {
					result[i.toDouble()] =
						gson.fromJson<Map<*, *>>(Component.Serializer.toJson(cmp), MutableMap::class.java)
				}
			}
			return arrayOf(result)
		}
	
	@get:Throws(LuaException::class)
	@get:LuaFunction(mainThread = true)
	val width: Array<Any>
		get() {
			val data = data
			if(target != null) {
				when(target!!.id.toString()) {
					"create:display_board_target" -> {
						var d = data.blockEntity.level!!
							.getBlockEntity(data.blockEntity.targetPosition) as FlapDisplayBlockEntity
						d = d.getController()
						return arrayOf(d.maxCharCount)
					}
				}
			}
			return arrayOf(-1)
		}
	
	@get:Throws(LuaException::class)
	@get:LuaFunction(mainThread = true)
	val height: Array<Any>
		get() {
			val data = data
			if(target != null) {
				when(target!!.id.toString()) {
					"create:display_board_target" -> {
						var d = data.blockEntity.level!!
							.getBlockEntity(data.blockEntity.targetPosition) as FlapDisplayBlockEntity
						d = d.getController()
						return arrayOf(d.getLines().size)
					}
				}
			}
			return arrayOf(-1)
		}
}
