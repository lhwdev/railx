package com.lhwdev.minecraft.railx.splitGraph.block

import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.Create
import com.simibubi.create.content.trains.graph.TrackGraphLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.ITrackBlock
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

interface SplitGraphTrack : ITrackBlock {
	fun getPoint(world: BlockGetter, pos: BlockPos, state: BlockState): SplitGraphPoint
	
	fun getEndConnected(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		linear: Boolean,
		connectedTo: TrackNodeLocation?,
	): Collection<TrackNodeLocation.DiscoveredLocation> =
		getPoint(worldIn, pos, state).let { listOf(it.fromDiscovered, it.centerDiscovered, it.toDiscovered) }
	
	fun getConnectedForCleanup(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
	): Collection<TrackNodeLocation.DiscoveredLocation> =
		getPoint(worldIn, pos, state).let { listOf(it.fromDiscovered, it.centerDiscovered, it.toDiscovered) }
	
	fun getGraphLocation(
		level: Level,
		pos: BlockPos,
		targetDirection: Direction.AxisDirection,
		targetAxis: Vec3,
	): TrackGraphLocation? {
		val point = getPoint(level, pos, level.getBlockState(pos))
		val dot = targetAxis.dot(point.tangent) * targetDirection.step
		if(!(dot * dot similarTo targetAxis.lengthSqr() * point.tangent.lengthSqr())) return null
		
		val center = point.center
		return TrackGraphLocation().apply {
			if(dot > 0) {
				val to = point.to
				val toGraph = Create.RAILWAYS.getGraph(level, to)
				graph = toGraph
				edge = Couple.create(center, to)
				position = 0.0
			} else {
				val from = point.from
				val fromGraph = Create.RAILWAYS.getGraph(level, from)
				graph = fromGraph
				edge = Couple.create(from, center)
				position = from.location.distanceTo(center.location)
			}
		}
	}
}
