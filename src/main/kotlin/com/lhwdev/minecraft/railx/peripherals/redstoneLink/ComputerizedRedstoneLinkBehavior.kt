package com.lhwdev.minecraft.railx.peripherals.redstoneLink

import com.lhwdev.minecraft.railx.config.RailXConfigServer
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.world.item.Item


class ComputerizedRedstoneLinkBehavior(val be: ComputerizedRedstoneLinkBlockEntity) : BlockEntityBehaviour(be) {
	
	companion object {
		val Type = BehaviourType<ComputerizedRedstoneLinkBehavior>()
	}
	
	val links = mutableListOf<RedstoneLinkHandle>()
	
	fun createLink(item1: Item, item2: Item): RedstoneLinkHandle? {
		for(existingLink in links) {
			if(existingLink.item1 == item1 && existingLink.item2 == item2) {
				return existingLink
			}
		}
		
		if(links.size > RailXConfigServer.MAXIMUM_CONCURRENT_LINKS.get()) {
			return null
		}
		val link = RedstoneLinkHandle(this, item1, item2)
		links += link
		return link
	}
	
	fun deleteLink(link: RedstoneLinkHandle) {
		link.unload()
		links -= link
	}
	
	fun deleteAllLinks() {
		links.forEach { it.unload() }
		links.clear()
	}
	
	private val nextTickTasks = mutableListOf<() -> Unit>()
	
	fun runOnNextTick(task: () -> Unit) {
		val notDirty = nextTickTasks.isEmpty()
		nextTickTasks += task
		if(notDirty) {
			be.setChanged()
		}
	}
	
	override fun tick() {
		if(nextTickTasks.isNotEmpty()) {
			for(task in nextTickTasks) {
				task()
			}
			nextTickTasks.clear()
		}
	}
	
	override fun getType() = Type
}
