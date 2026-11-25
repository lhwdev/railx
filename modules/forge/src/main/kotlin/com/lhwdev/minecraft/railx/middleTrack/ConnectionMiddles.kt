package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.common.asPrimary
import com.lhwdev.minecraft.railx.utils.minecraft.getBlockEntityIfLoaded
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockEntity
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.WorldAttached
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor


val GlobalConnections: WorldAttached<ConnectionMiddles> = WorldAttached { level -> ConnectionMiddles(level) }


class ConnectionMiddles(private val level: LevelAccessor) : Iterable<ConnectionMiddleState> {
	private val primaryToMiddles =
		Long2ObjectOpenHashMap<Long2ObjectArrayMap<ConnectionMiddleState>>()
	
	operator fun get(from: BlockPos, to: BlockPos): ConnectionMiddleState? =
		primaryToMiddles.get(from.asLong())?.get(to.asLong())
	
	operator fun get(pos: Couple<BlockPos>): ConnectionMiddleState? =
		get(pos.first, pos.second)
	
	fun getAll(from: BlockPos): Collection<ConnectionMiddleState> =
		primaryToMiddles.get(from.asLong())?.values ?: emptyList()
	
	override fun iterator(): Iterator<ConnectionMiddleState> =
		primaryToMiddles.values.asSequence().flatMap { it.values }.iterator()
	
	
	private fun removeCurves(pos: BlockPos, curves: List<BezierConnection>) {
		for(removed in curves) {
			val curve = removed.asPrimary()
			val from = curve.bePositions.first.asLong()
			val to = curve.bePositions.second.asLong()
			val destinations = primaryToMiddles[from] ?: continue
			val state = destinations[to] ?: continue
			if(!state.matches(curve)) continue
			
			state.removeMiddle(pos)
			if(state.isEmpty()) {
				destinations.remove(to)
				if(destinations.isEmpty()) primaryToMiddles.remove(from)
			}
		}
	}
	
	private fun stateFor(
		curve: BezierConnection,
		updatingMiddle: BlockPos? = null,
	): ConnectionMiddleState {
		val from = curve.bePositions.first.asLong()
		val to = curve.bePositions.second.asLong()
		val destinations = primaryToMiddles.getOrPut(from) { Long2ObjectArrayMap() }
		val previous = destinations[to]
		if(previous != null && previous.matches(curve)) return previous
		
		val replacement = ConnectionMiddleState(level, curve)
		destinations[to] = replacement
		if(previous != null) for(pos in previous.allMiddles) {
			if(pos == updatingMiddle) continue
			val middle = level.getBlockEntityIfLoaded(pos) as? MiddleTrackBlockEntity ?: continue
			if(!level.isClientSide) {
				val remaining = middle.connections.filter {
					// note that MiddleTrackBlock does not necessarily target only one curve.
					!previous.matches(it) || replacement.matches(it)
				}
				
				if(remaining.size != middle.connections.size)
					middle.updateConnections(remaining)
			}
			if(middle.connections.any { replacement.matches(it) })
				replacement.addMiddle(pos, valid = true)
		}
		return replacement
	}
	
	fun updateMiddle(be: MiddleTrackBlockEntity, value: List<BezierConnection>): List<BezierConnection> {
		check(value.all { it.primary }) { "given value is not primary" }
		val previous = be.connections
		removeCurves(be.blockPos, previous.withoutIdentity(value))
		
		val rejected = mutableListOf<BezierConnection>()
		for(curve in value.withoutIdentity(previous)) {
			if(curve.length < RailXConfig.Server.middleTrack.placeGap.asInt) continue
			
			val from = curve.bePositions.first
			val to = curve.bePositions.second
			val endpoint = endpointCurve(from, to) ?: endpointCurve(to, from)
			if(endpoint != null && !endpoint.matchesMiddleCurve(curve)) {
				rejected += curve
				continue
			}
			
			val valid = endpoint != null || curve.isInTrackGraph(level)
			if(!valid && !level.isClientSide) {
				rejected += curve
				continue
			}
			val existing = get(curve.bePositions)
			if(!valid && existing != null && !existing.matches(curve)) continue
			
			val state = stateFor(endpoint ?: curve, updatingMiddle = be.blockPos)
			state.addMiddle(be.blockPos, valid)
		}
		
		if(level.isClientSide || rejected.isEmpty()) return value
		return value.withoutIdentity(rejected)
	}
	
	fun removeMiddle(be: MiddleTrackBlockEntity, previous: List<BezierConnection> = be.connections) {
		removeCurves(be.blockPos, previous)
	}
	
	
	fun updateTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		val current = be.connections
		val removed = previous.filter { old ->
			val replacement = current[old.key]
			replacement == null || !old.matchesMiddleCurve(replacement)
		}
		removeCurves(be.blockPos, removed)
		
		for(connection in current.values) {
			val curve = connection.asPrimary()
			val primary = if(connection.primary) curve else {
				endpointCurve(curve.bePositions.first, curve.bePositions.second)
			}
			
			if(primary != null && !primary.matchesMiddleCurve(curve)) continue
			if(curve.length < RailXConfig.Server.middleTrack.placeGap.asInt) continue
			
			if(get(curve.bePositions) == null) continue
			
			val matching = primary ?: curve
			stateFor(matching).addEndpoint(be.blockPos, matching)
		}
	}
	
	fun removeTrack(be: TrackBlockEntity, previous: List<BezierConnection>) {
		removeCurves(be.blockPos, previous)
	}
	
	private fun endpointCurve(endpoint: BlockPos, other: BlockPos): BezierConnection? {
		val track = level.getBlockEntityIfLoaded(endpoint) as? TrackBlockEntity ?: return null
		return track.connections[other]?.asPrimary()
	}
}

private fun <T> Collection<T>.withoutIdentity(other: Collection<T>): List<T> =
	filterNot { a -> other.any { b -> a === b } }
