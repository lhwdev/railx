package com.lhwdev.minecraft.railx.peripherals.redstoneLink

import com.lhwdev.minecraft.railx.utils.asyncLuaTask
import com.simibubi.create.content.redstone.link.LinkBehaviour
import com.simibubi.create.content.redstone.link.RedstoneLinkFrequencySlot
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import net.minecraft.world.item.Item


class RedstoneLinkHandle(val behavior: ComputerizedRedstoneLinkBehavior, val item1: Item, val item2: Item) {
	private var isValid = true
	
	private var output = 0
	private var input = 0
	
	private val slots = ValueBoxTransform.Dual.makeSlots { RedstoneLinkFrequencySlot(it) }
	private val transmitter = lazy(LazyThreadSafetyMode.NONE) {
		LinkBehaviour.transmitter(behavior.be, slots) { output }
			.also { initFrequency(it) }
	}
	private val receiver = lazy(LazyThreadSafetyMode.NONE) {
		LinkBehaviour.receiver(behavior.be, slots) { input = it }
			.also { initFrequency(it) }
	}
	
	private fun ensureValid() {
		if(!isValid) throw LuaException("already deleted link")
	}
	
	private fun initFrequency(side: LinkBehaviour) {
		side.setFrequency(true, item1.defaultInstance)
		side.setFrequency(false, item2.defaultInstance)
	}
	
	@LuaFunction
	fun getInput(computer: IComputerAccess): MethodResult {
		ensureValid()
		if(receiver.isInitialized()) {
			return MethodResult.of(input)
		}
		
		val (onComplete, result) = asyncLuaTask(computer)
		behavior.runOnNextTick {
			receiver.value.initialize()
			onComplete(arrayOf())
		}
		return result
	}
	
	@LuaFunction
	fun getOutput(): Int {
		ensureValid()
		return output
	}
	
	@LuaFunction
	fun setOutput(strength: Any) {
		ensureValid()
		when(strength) {
			is Number -> setOutput(strength.toInt())
			is Boolean -> setOutput(if(strength) 15 else 0)
		}
	}
	
	fun setOutput(strength: Int) {
		output = strength
		notifyChange()
	}
	
	private fun notifyChange() {
		if(behavior.be.level?.isClientSide != false) return
		behavior.runOnNextTick {
			if(!transmitter.isInitialized()) {
				transmitter.value.initialize()
			}
			transmitter.value.notifySignalChange()
		}
	}
	
	@LuaFunction
	fun delete() {
		ensureValid()
		
		unload()
		behavior.deleteLink(this)
	}
	
	fun unload() {
		isValid = false
		behavior.runOnNextTick {
			if(transmitter.isInitialized()) {
				transmitter.value.unload()
			}
			if(receiver.isInitialized()) {
				receiver.value.unload()
			}
		}
	}
}
