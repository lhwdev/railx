package com.lhwdev.minecraft.railx.middleTrack

import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockEntity
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
	
	override fun iterator(): Iterator<ConnectionMiddleState> =
		primaryToMiddles.values.asSequence().flatMap { it.values }.iterator()
	
	fun toList(): List<ConnectionMiddleState> =
		primaryToMiddles.flatMap { it.value.map { it.value } }
	
	
	fun updateMiddle(
		be: MiddleTrackLikeBlockEntity,
		value: List<BezierConnection>,
		previous: List<BezierConnection> = be.connectionValues,
	) {
		for(toRemove in previous withoutIdentity value) {
			val from = toRemove.bePositions.first.asLong()
			val to = toRemove.bePositions.second.asLong()
			val state = primaryToMiddles.get(from)?.get(to) ?: continue
			state.removeMiddle(be.blockPos)
			if(state.isEmpty()) {
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
	
	fun removeMiddle(be: MiddleTrackLikeBlockEntity, previous: List<BezierConnection> = be.connectionValues) {
		updateMiddle(be, value = emptyList(), previous = previous)
	}
	
	
	fun updateTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		updateMiddle(
			be as MiddleTrackLikeBlockEntity,
			value = be.connections.values.filter { !it.primary },
			previous = previous.filter { !it.primary },
		)
	}
	
	fun removeTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		removeMiddle(be as MiddleTrackLikeBlockEntity, previous.filter { !it.primary })
	}
}


private infix fun <T> Collection<T>.withoutIdentity(other: Collection<T>): Collection<T> =
	filterNot { a -> other.any { b -> a === b } }
