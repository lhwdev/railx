package com.lhwdev.minecraft.railx.splitGraph.flexiBlock

import com.lhwdev.minecraft.railx.common.addIfConnected
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphPoint
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphPointBase
import com.lhwdev.minecraft.railx.splitGraph.block.SplitGraphTrack
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3


class FlexiSplitGraphTrackBlock(properties: Properties, material: FlexiTrackMaterial) :
	FlexiTrackBlock(properties, material), SplitGraphTrack {
	override fun getPoint(world: BlockGetter, pos: BlockPos, state: BlockState): SplitGraphPoint =
		FlexiSplitGraphTrackBlockPoint(world, pos, state)
	
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
		
		val world = if(connectedTo != null && worldIn is ServerLevel) {
			worldIn.server.getLevel(connectedTo.dimension)!!
		} else {
			worldIn
		}
		val blockEntity = world.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return emptyList()
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
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		return super.getStateForPlacement(context)
	}
}

private class FlexiSplitGraphTrackBlockPoint(world: BlockGetter, pos: BlockPos, state: BlockState) :
	SplitGraphPointBase() {
	override val track: FlexiSplitGraphTrackBlock = state.block as FlexiSplitGraphTrackBlock
	
	override val centerVec: Vec3 = track.getTrackBase(world, pos, state)
	override val dimension: ResourceKey<Level> = if(world is Level) world.dimension() else Level.OVERWORLD
	private val shape = track.flexiShape(world, pos)
	
	override val tangent: Vec3
		get() = shape.axis1.tangent
	
	override val normal: Vec3
		get() = shape.normal
}
