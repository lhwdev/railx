package com.lhwdev.minecraft.railx.middleTrack

import com.simibubi.create.content.trains.track.BezierConnection
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor


val GlobalConnections: WorldAttached<ConnectionMiddles> = WorldAttached { level -> ConnectionMiddles(level) }


class ConnectionMiddles(private val level: LevelAccessor) : Iterable<ConnectionMiddleState> {
	private val primaryToMiddles =
		Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<ConnectionMiddleState>>()
	
	operator fun get(from: BlockPos, to: BlockPos): ConnectionMiddleState? =
		primaryToMiddles.get(from.asLong())?.get(to.asLong())
	
	operator fun get(pos: Couple<BlockPos>): ConnectionMiddleState? =
		get(pos.first, pos.second)
	
	fun getAll(from: BlockPos): Collection<ConnectionMiddleState> =
		primaryToMiddles.get(from.asLong())?.values ?: emptyList()
	
	override fun iterator(): Iterator<ConnectionMiddleState> = iterator {
		for(toMiddles in primaryToMiddles.values) {
			for(middle in toMiddles.values) {
				yield(middle)
			}
		}
	}
	
	fun toList(): List<ConnectionMiddleState> =
		primaryToMiddles.flatMap { it.value.map { it.value } }
	
	
	fun updateMiddle(be: MiddleTrackBlockEntity, value: List<BezierConnection>) {
		val previous = be.connections
		for(toRemove in previous withoutIdentity value) {
			val from = toRemove.bePositions.first.asLong()
			val to = toRemove.bePositions.second.asLong()
			val state = primaryToMiddles.get(from)?.get(to) ?: continue
			state.removeMiddle(be.blockPos)
			if(state.middles.isEmpty()) {
				val toMiddles = primaryToMiddles[from]!!
				toMiddles.remove(to)
				if(toMiddles.isEmpty()) primaryToMiddles.remove(from)
			}
		}
		
		for(toAdd in value withoutIdentity previous) {
			val from = toAdd.bePositions.first.asLong()
			val to = toAdd.bePositions.second.asLong()
			val state = primaryToMiddles.getOrPut(from) { Long2ObjectOpenHashMap() }
				.getOrPut(to) { ConnectionMiddleState(level, curve = toAdd) }
			state.addMiddle(be.blockPos)
		}
	}
	
	fun removeMiddle(be: MiddleTrackBlockEntity) {
		updateMiddle(be, emptyList())
	}
}


private infix fun <T> List<T>.withoutIdentity(other: List<T>): List<T> =
	filterNot { a -> other.any { b -> a === b } }
