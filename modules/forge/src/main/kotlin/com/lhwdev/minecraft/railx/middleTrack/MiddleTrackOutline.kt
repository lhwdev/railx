package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVoxelShapes
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.foundation.utility.RaycastHelper
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.data.WorldAttached
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min


data class MiddleBezierConnection(
	val bc: BezierConnection,
	val middlePos: BlockPos,
)

class MiddleBezierPointSelection(
	val from: MiddleBezierConnection,
	val loc: BezierTrackPointLocation,
	val vec: Vec3,
	val angles: Vec3,
	val direction: Vec3,
)

object MiddleTrackOutline {
	var result: MiddleBezierPointSelection? = null
	
	
	// NOTE: Most client behaviors are not implemented for out-of-chunk curves
	// - not supported: TrackBlockOutline, TrackTargetingClient, CurvedTrackInteraction
	fun pickCurves() {
		if(
			!RailXConfig.Server.middleTrack.enabled.get() ||
			!RailXConfig.Server.middleTrack.enableInteraction.get()
		) return
		
		val mc = Minecraft.getInstance()
		val player = mc.cameraEntity as? LocalPlayer ?: return
		val level = mc.level ?: return
		
		val origin = player.getEyePosition(AnimationTickHolder.getPartialTicks(level))
		
		val hitResult = mc.hitResult
		var maxRange = hitResult?.location?.distanceToSqr(origin) ?: Double.MAX_VALUE
		
		val range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
		val target = RaycastHelper.getTraceTarget(player, min(maxRange, range) + 1, origin)
		val turns = MiddleTracks.TracksFromMiddle[level]
		
		for((_, connections) in turns) {
			for((_, sameConnections) in connections) {
				val connection = sameConnections.first()
				val bc = connection.bc
				if(!bc.isPrimary) continue
				
				val bounds = bc.getBounds()
				if(!bounds.contains(origin) && bounds.clip(origin, target).isEmpty) continue
				
				val stepLUT = bc.getStepLUT()
				val segments = (bc.getLength() * 2).toInt()
				var segmentBounds = FlexiTrackVoxelShapes.base.bounds()
				segmentBounds = segmentBounds.move(-.5, segmentBounds.ysize / -2, -.5)
				
				var bestSegment = -1
				var bestDistance = Double.MAX_VALUE
				var newMaxRange = maxRange
				
				for(i in 0..<stepLUT.size - 2) {
					val t = stepLUT[i] * i / segments
					val t1 = stepLUT[i + 1] * (i + 1) / segments
					val t2 = stepLUT[i + 2] * (i + 2) / segments
					
					val v1: Vec3 = bc.getPosition(t.toDouble())
					val v2: Vec3 = bc.getPosition(t2.toDouble())
					val diff = v2.subtract(v1)
					val angles = TrackRenderer.getModelAngles(bc.getNormal(t1.toDouble()), diff)
					
					val anchor = v1.add(diff.scale(.5))
					var localOrigin = origin.subtract(anchor)
					var localDirection = target.subtract(origin)
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.x).toDouble(), Direction.Axis.X)
					localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.y).toDouble(), Direction.Axis.Y)
					localDirection =
						VecHelper.rotate(localDirection, AngleHelper.deg(-angles.x).toDouble(), Direction.Axis.X)
					localDirection =
						VecHelper.rotate(localDirection, AngleHelper.deg(-angles.y).toDouble(), Direction.Axis.Y)
					
					val clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection))
					if(clip.isEmpty) continue
					
					if(bestSegment != -1 && bestDistance < clip.get().distanceToSqr(0.0, 0.25, 0.0)
					) continue
					
					val distanceToSqr = clip.get().distanceToSqr(localOrigin)
					if(distanceToSqr > maxRange) continue
					
					bestSegment = i
					newMaxRange = distanceToSqr
					bestDistance = clip.get().distanceToSqr(0.0, 0.25, 0.0)
					
					val location = BezierTrackPointLocation(bc.key, i)
					result = MiddleBezierPointSelection(connection, location, anchor, angles, diff.normalize())
				}
				
				if(bestSegment != -1) maxRange = newMaxRange
			}
		}
		
		if(result == null) return
		if(hitResult != null && hitResult.type != HitResult.Type.MISS) {
			val priorLoc = hitResult.location
			mc.hitResult = BlockHitResult.miss(priorLoc, Direction.UP, BlockPos.containing(priorLoc))
		}
	}
}
