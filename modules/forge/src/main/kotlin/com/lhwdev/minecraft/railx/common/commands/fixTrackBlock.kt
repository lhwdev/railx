package com.lhwdev.minecraft.railx.common.commands

import com.lhwdev.minecraft.railx.compat.onCurveUpdated
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.utils.closeTo
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.simibubi.create.Create
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.content.trains.track.TrackPropagator
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


private fun findNearestStartPoint(level: Level, pos: BlockPos, state: BlockState, previous: Vec3): Vec3 {
	val track = state.block as ITrackBlock
	val points = mutableListOf<Vec3>()
	
	for(axis in track.getTrackAxes(level, pos, state)) {
		val start = track.getCurveStart(level, pos, state, axis)
		val end = track.getCurveStart(level, pos, state, -axis)
		
		points += start
		points += end
	}
	
	val point = points.minBy { it.distanceToSqr(previous) }
	val distance = point.distanceTo(previous)
	if(!(distance similarTo 0.0)) {
		if(distance > 0.1) {
			val axis = point - Vec3.atBottomCenterOf(pos)
				.add(0.0, track.getElevationAtCenter(level, pos, state), 0.0)
			System.err.println(
				"distance > 0.1 for block at $pos, axis=${
					FlexiDirection.Known.roundFrom(axis).format()
				}"
			)
			println("breakpoint here")
		}
	}
	
	return point
}

fun fixTrackBlock(level: Level, pos: BlockPos) {
	val state = level.getBlockState(pos)
	val track = state.block as? ITrackBlock ?: return
	
	val points = mutableListOf<Vec3>()
	
	
	// 1. axis end to end is connected
	
	fun ensureConnected(from: TrackNodeLocation, to: TrackNodeLocation) {
		var added = false
		val graphs = Create.RAILWAYS.getGraphs(level, from)
		val graph = if(graphs.size != 1) {
			added = true
			TrackPropagator.onRailAdded(level, pos, state) ?: return
		} else {
			graphs[0]
		}
		
		val otherGraphs = Create.RAILWAYS.getGraphs(level, to)
		val otherGraph = if(otherGraphs.size != 1) {
			if(!added) {
				TrackPropagator.onRailAdded(level, pos, state)
			} else {
				System.err.println("why???")
				null
			}
		} else {
			otherGraphs[0]
		}
		
		if(graph != otherGraph && !added)
			TrackPropagator.onRailAdded(level, pos, state)
	}
	
	
	// 2. each end of bezier is connected
	
	for(axis in track.getTrackAxes(level, pos, state)) {
		val start = track.getCurveStart(level, pos, state, axis)
		val end = track.getCurveStart(level, pos, state, -axis)
		val center = Vec3.atBottomCenterOf(pos)
			.add(0.0, track.getElevationAtCenter(level, pos, state), 0.0)
		
		points += start
		points += end
		
		if(track is FlexiTrackBlock) ensureConnected(
			TrackNodeLocation(start).`in`(level),
			TrackNodeLocation(center).`in`(level)
		)
		if(track is FlexiTrackBlock) ensureConnected(
			TrackNodeLocation(center).`in`(level),
			TrackNodeLocation(end).`in`(level)
		)
	}
	
	val blockEntity = level.getBlockEntity(pos) as? TrackBlockEntity ?: return
	for(bc in blockEntity.connections.values) {
		val point = points.minBy { it.distanceToSqr(bc.starts.first) }
		val distance = point.distanceTo(bc.starts.first)
		if(!(distance similarTo 0.0)) {
			if(distance > 0.1) {
				val axis = point - Vec3.atBottomCenterOf(pos)
					.add(0.0, track.getElevationAtCenter(level, pos, state), 0.0)
				System.err.println(
					"distance > 0.1 for block at $pos, axis=${
						FlexiDirection.Known.roundFrom(axis).format()
					}"
				)
				println("breakpoint here")
			}
			
			val previous = TrackNodeLocation(bc.starts.first).`in`(level)
			
			val otherBe = level.getBlockEntity(bc.key) as? TrackBlockEntity
			if(otherBe == null) {
				println("cannot load other side")
				continue
			}
			
			val otherBc = otherBe.connections[pos]
			if(otherBc == null) {
				println("cannot load other connection")
				continue
			}
			
			bc.starts.first = point
			bc.onCurveUpdated()
			
			otherBc.starts.second = point
			otherBc.onCurveUpdated()
			
			val current = TrackNodeLocation(point).`in`(level)
			if(previous != current) {
				val previousGraphs = Create.RAILWAYS.getGraphs(level, previous)
				for(graph in previousGraphs) {
					val node = graph.locateNode(previous) ?: continue
					graph.removeNode(level, previous)
					Create.RAILWAYS.sync.nodeRemoved(graph, node)
				}
			}
		}
		
		if(level.isLoaded(bc.key)) {
			val otherBe = level.getBlockEntity(bc.key) as? TrackBlockEntity ?: continue
			val otherBc = otherBe.connections[pos] ?: continue
			if(!(otherBc.starts.second closeTo point) || !(otherBc.starts.first closeTo bc.starts.second)) {
				val a = findNearestStartPoint(level, pos, state, bc.starts.first)
				val b = findNearestStartPoint(level, bc.key, level.getBlockState(bc.key), otherBc.starts.first)
				
				bc.starts.first = a
				bc.starts.second = b
				bc.onCurveUpdated()
				
				otherBc.starts.first = b
				otherBc.starts.second = a
				otherBc.onCurveUpdated()
				
				TrackPropagator.onRailAdded(level, pos, state)
				println("!!!!FIXED ASYMMETRIC")
			}
		}
		
		blockEntity.validateConnections()
	}
	
	
	val visited = HashSet<TrackNodeLocation>(track.getConnected(level, pos, state, true, null))
	val linearNodes = ArrayDeque(visited)
	
	while(linearNodes.isNotEmpty()) {
		val location = linearNodes.removeFirst()
		val graph = Create.RAILWAYS.getGraph(level, location) ?: continue
		
		val node = graph.locateNode(location)
		val connectedNodes = track.getConnected(level, pos, state, false, location)
		
		for(connected in connectedNodes) {
			if(!visited.add(connected))
				continue
			
			// connected is not linear node (as they are already included inside visited)
			ensureConnected(location, connected)
			
			val connectedNode = graph.locateNode(connected)
			if(connectedNode == null) {
				println("???")
				continue
			}
			
			if(graph.getConnection(Couple.create(node, connectedNode)) == null)
				println("L->C no???")
			
			if(graph.getConnection(Couple.create(connectedNode, node)) == null)
				println("C->L no???")
		}
	}
}
