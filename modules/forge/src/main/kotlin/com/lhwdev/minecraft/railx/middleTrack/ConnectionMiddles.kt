package com.lhwdev.minecraft.railx.middleTrack

import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockEntity
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntity


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
	
	override fun iterator(): Iterator<ConnectionMiddleState> =
		primaryToMiddles.values.asSequence().flatMap { it.values }.iterator()
	
	
	private fun updateFor(
		be: BlockEntity,
		previous: List<BezierConnection>,
		value: List<BezierConnection>,
	) {
		for(toRemove in previous withoutIdentity value) {
			var pos = toRemove.bePositions
			if(!toRemove.primary) pos = pos.swap()
			
			val from = pos.first.asLong()
			val to = pos.second.asLong()
			val state = primaryToMiddles.get(from)?.get(to) ?: continue
			state.removeMiddle(be.blockPos)
			if(state.isEmpty()) {
				val toMiddles = primaryToMiddles[from]!!
				toMiddles.remove(to)
				if(toMiddles.isEmpty()) primaryToMiddles.remove(from)
			}
		}
		
		for(toAdd in value withoutIdentity previous) {
			var pos = toAdd.bePositions
			if(!toAdd.primary) pos = pos.swap()
			
			val from = pos.first.asLong()
			val to = pos.second.asLong()
			val state = primaryToMiddles.getOrPut(from) { Long2ObjectOpenHashMap() }
				.getOrPut(to) { ConnectionMiddleState(level, curve = if(toAdd.primary) toAdd else toAdd.secondary()) }
			state.addMiddle(be.blockPos)
		}
	}
	
	private fun removeFor(be: BlockEntity, previous: List<BezierConnection>) {
		updateFor(be, previous, value = emptyList())
	}
	
	fun updateMiddle(be: MiddleTrackBlockEntity, value: List<BezierConnection>) {
		check(value.all { it.primary }) { "given value is not primary" }
		updateFor(be, previous = be.connections, value = value)
	}
	
	fun removeMiddle(be: MiddleTrackBlockEntity, previous: List<BezierConnection> = be.connections) {
		removeFor(be, previous = previous)
	}
	
	
	fun updateTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		updateFor(
			be,
			previous = previous.filter { !it.primary },
			value = be.connections.values.filter { !it.primary }
		)
	}
	
	fun removeTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		removeFor(be, previous.filter { !it.primary })
	}
}


private infix fun <T> Collection<T>.withoutIdentity(other: Collection<T>): Collection<T> =
	filterNot { a -> other.any { b -> a === b } }
