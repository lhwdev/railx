package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.trains.track.*
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.times
import kotlin.math.sign


class FlexiTrackBlockEntityTilt(private val blockEntity: FlexiTrackBlockEntity) : TrackBlockEntityTilt(blockEntity) {
	private class Handle(val start: Vec3, smoothing: Int)
	
	private val state get() = blockEntity.state
	
	private var previousSmoothingHandles: List<Handle>? = null
	
	override fun tryApplySmoothing() {
		if(state.tilt != null) return
		
		val blockState = blockEntity.blockState
		val blockPos = blockEntity.blockPos
		val level = blockEntity.level!!
		
		var axis = state.shape.axes.singleOrNull() ?: return
		if(axis.tangent.y != 0.0) return
		
		val connections = blockEntity.connections.values.sortedBy { it.starts.second.y }
		if(connections.size != 2) return
		val (lower, higher) = connections
		
		val lowStarts = lower.starts
		val highStarts = higher.starts
		val lowestPoint = lowStarts.second
		val highestPoint = highStarts.second
		
		if(lowestPoint.y > lowStarts.first.y) return
		if(highestPoint.y < highStarts.first.y) return
		if(lowestPoint.y similarTo highestPoint.y) return
		
		blockEntity.removeInboundConnections(false)
		blockEntity.connections.clear()
		TrackPropagator.onRailRemoved(level, blockPos, blockState)
		
		val hDistance = lower.length + higher.length
		val vDistance = highestPoint.y - lowestPoint.y
		
		val tilt = FlexiShapeTilt(
			axis = lower.normals.first.cross(lower.axes.first),
			rotation = Mth.atan2(vDistance, hDistance)
		)
		blockEntity.updateState(state.copy(tilt = tilt))
		axis = state.shape.axis1
		
		fun applySmoothing(connection: BezierConnection) {
			val connection = connection.clone()
			val tangent = axis.tangent * sign(axis.tangent.dot(connection.axes.first))
			connection.starts.first = blockEntity.block.getCurveStart(
				world = level,
				pos = blockPos,
				state = blockState,
				axis = tangent,
			)
			connection.axes.first = tangent
			connection.normals.first = axis.normal
			
			val otherPosition = connection.key
			val otherState = level.getBlockState(otherPosition)
			if(otherState.block !is ITrackBlock) return
			level.setBlockAndUpdate(otherPosition, otherState.setValue(TrackBlock.HAS_BE, true))
			val otherBE = level.getBlockEntity(otherPosition)
			if(otherBE is TrackBlockEntity) {
				blockEntity.addConnection(connection)
				otherBE.addConnection(connection.secondary())
			}
		}
		applySmoothing(connection = lower)
		applySmoothing(connection = higher)
	}
	
	override fun captureSmoothingHandles() {
		previousSmoothingHandles = blockEntity.connections.values.map {
			Handle(start = it.starts.first, smoothing = it.smoothing?.first ?: 0)
		}
	}
	
	override fun undoSmoothing() {
		if(state.tilt == null) return
		if(previousSmoothingHandles == null) return
		if(blockEntity.connections.size >= 2) return
		if(state.shape.axes.size != 1) return
		
		val blockState = blockEntity.blockState
		val blockPos = blockEntity.blockPos
		val level = blockEntity.level!!
		
		blockEntity.updateEachConnections {
			blockEntity.updateState(state.copy(tilt = null))
			val axis = state.shape.axis1
			
			forEachConnections { connection ->
				val tangent = axis.tangent * sign(axis.tangent.dot(connection.axes.first))
				connection.starts.first = blockEntity.block.getCurveStart(
					world = level,
					pos = blockPos,
					state = blockState,
					axis = tangent,
				)
				connection.axes.first = tangent
				connection.normals.first = axis.normal
			}
		}
		previousSmoothingHandles = null
		TrackPropagator.onRailAdded(level, blockPos, blockState)
	}
	
	override fun restoreToOriginalCurve(connection: BezierConnection): BezierConnection {
		if(connection.smoothing != null)
			connection.smoothing = null
		val axis = state.shape.axes.singleOrNull() ?: return connection
		
		connection.starts.first = blockEntity.block.getCurveStart(
			world = blockEntity.level!!,
			pos = blockEntity.blockPos,
			state = blockEntity.blockState,
			axis = axis.tangent * sign(axis.tangent.dot(connection.axes.first)),
		)
		connection.axes.first = axis.tangent
		connection.normals.first = axis.normal
		
		return connection
	}
	
	// fun getYOffsetForAxisEnd(end: Vec3): Int {
	// 	if(smoothingAngle.isEmpty) return 0
	// 	for(bezierConnection in blockEntity.connections.values) if(compareHandles(
	// 			bezierConnection.starts.first,
	// 			end
	// 		)
	// 	) return bezierConnection.yOffsetAt(end)
	// 	if(previousSmoothingHandles == null) return 0
	// 	for(handle in previousSmoothingHandles) if(handle != null && compareHandles(
	// 			handle.first,
	// 			end
	// 		)
	// 	) return handle.second
	// 	return 0
	// }
	
	// fun compareHandles(handle1: Vec3, handle2: Vec3): Boolean {
	// 	return TrackNodeLocation(handle1).location
	// 		.multiply(1.0, 0.0, 1.0)
	// 		.distanceToSqr(
	// 			TrackNodeLocation(handle2).location
	// 				.multiply(1.0, 0.0, 1.0)
	// 		) < 1 / 512f
	// }
}
