package com.lhwdev.minecraft.railx.splitGraph.block

import com.lhwdev.minecraft.railx.common.addIfConnected
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


class SplitGraphTrackBlock(properties: Properties, material: TrackMaterial) : TrackBlock(properties, material),
	SplitGraphTrack {
	override fun getPoint(world: BlockGetter, pos: BlockPos, state: BlockState): SplitGraphPoint =
		SplitGraphTrackBlockPoint(world, pos, state)
	
	override fun overlay(world: BlockGetter, pos: BlockPos, existing: BlockState, placed: BlockState): BlockState =
		existing
	
	override fun getConnected(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		linear: Boolean,
		connectedTo: TrackNodeLocation?,
	): Collection<DiscoveredLocation> {
		if(linear) return emptyList()
		if(!state.getValue(HAS_BE)) return emptyList()
		val world = if(connectedTo != null && worldIn is ServerLevel) {
			worldIn.server.getLevel(connectedTo.dimension)!!
		} else {
			worldIn
		}
		val blockEntity = getBlockEntity(world, pos) ?: return emptyList()
		
		val list = mutableListOf<DiscoveredLocation>()
		val connections = blockEntity.connections
		for((_, bc) in connections) list.addIfConnected(
			fromEnd = connectedTo,
			getOffset = { t, first ->
				if(t == 1.0) Vec3.atLowerCornerOf(bc.bePositions.get(first))
				else bc.starts.get(first)
			}
		) {
			DiscoveredLocation(
				level = world,
				normal = bc.normals.get(isFirst),
				tangent = null,
				yOffset = bc.yOffsetAt(offsetCenter),
				viaTurn = bc,
			)
		}
		
		return list
	}
}


private class SplitGraphTrackBlockPoint(world: BlockGetter, pos: BlockPos, state: BlockState) : SplitGraphPointBase() {
	override val track: SplitGraphTrackBlock = state.block as SplitGraphTrackBlock
	
	override val centerVec: Vec3 = pos.bottomCenter.add(0.0, track.getElevationAtCenter(world, pos, state), 0.0)
	override val dimension: ResourceKey<Level> = if(world is Level) world.dimension() else Level.OVERWORLD
	private val shape = state.getValue(TrackBlock.SHAPE)
	
	override val tangent: Vec3
		get() = shape.axes.first()
	
	override val normal: Vec3
		get() = shape.normal
}
