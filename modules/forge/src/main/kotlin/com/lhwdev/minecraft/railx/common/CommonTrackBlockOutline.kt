package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.compat.CompatMods
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.plus
import com.railwayteam.railways.registry.CRShapes
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.foundation.block.IHaveBigOutline
import com.simibubi.create.foundation.utility.RaycastHelper
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import com.simibubi.create.AllShapes as CreateShapes
import com.simibubi.create.AllTags as CreateTags


object CommonTrackBlockOutline {
	private val standardSegmentBounds = CreateShapes.TRACK_ORTHO[Direction.SOUTH].bounds()
		.let { it.move(-.5, it.ysize / -2, -.5) }
	
	
	fun pickBlock(level: LevelAccessor, origin: Vec3, target: Vec3, maxRange: Double): BlockHitResult? {
		val p = BlockPos.MutableBlockPos()
		var result: BlockHitResult? = null
		
		RaycastHelper.rayTraceUntil(origin, target) { pos ->
			for(x in -1..1) {
				for(y in -1..1) {
					for(z in -1..1) {
						p.set(pos.x + x, pos.y + y, pos.z + z)
						val state = level.getBlockState(p)
						val block = state.block
						if(block !is IHaveBigOutline || block !is ITrackBlock)
							continue
						
						val hit = state.getInteractionShape(level, p)
							.clip(origin, target, p.immutable()) ?: continue
						
						if(
							result != null &&
							Vec3.atCenterOf(p).distanceToSqr(origin) >=
							Vec3.atCenterOf(result!!.blockPos).distanceToSqr(origin)
						) continue
						
						
						var vec = hit.location
						val interactionDist = vec.distanceToSqr(origin)
						if(interactionDist >= maxRange) continue
						
						val hitPos = hit.blockPos
						
						// pacifies ServerGamePacketListenerImpl.handleUseItemOn
						vec -= Vec3.atCenterOf(hitPos)
						vec = VecHelper.clampComponentWise(vec, 1f)
						vec += Vec3.atCenterOf(hitPos)
						
						result = BlockHitResult(vec, hit.direction, hitPos, hit.isInside)
					}
				}
			}
			
			false
		}
		
		if(result == null) return null
		val state = level.getBlockState(result.blockPos)
		if(state.`is`(CreateTags.AllBlockTags.TRACKS.tag)) return result
		return null
	}
	
	
	class PickCurveResult(
		val curve: BezierConnection,
		val segmentIndex: Int,
		val position: Vec3,
		val tangent: Vec3,
		val normal: Vec3,
		val angles: Vec3,
	)
	
	fun pickCurves(
		connections: Collection<BezierConnection>,
		origin: Vec3,
		target: Vec3,
		maxRange: Double,
		updateResult: (index: Int, pick: PickCurveResult) -> Unit,
	) {
		var maxRangeSqr = maxRange * maxRange
		val rayDirection = target.subtract(origin)
		
		for((index, bc) in connections.withIndex()) {
			if(!bc.isPrimary) continue
			
			val bounds = bc.bounds
			if(!bounds.contains(origin) && bounds.clip(origin, target).isEmpty)
				continue
			
			val stepLUT = bc.stepLUT
			val segments = (bc.length * 2).toInt()
			
			var bestSegment = -1
			var bestDistance = Double.MAX_VALUE
			var newMaxRangeSqr = maxRangeSqr
			
			val shape = getShape(bc.material, direction = Direction.SOUTH)
			val segmentBounds = if(bc.material.trackType == TrackMaterial.TrackType.STANDARD) {
				standardSegmentBounds
			} else {
				shape.bounds().let { it.move(-.5, it.ysize / -2, -.5) }
			}
			
			for(i in 0..<stepLUT.size - 2) {
				val t = stepLUT[i] * i / segments
				val t1 = stepLUT[i + 1] * (i + 1) / segments
				val t2 = stepLUT[i + 2] * (i + 2) / segments
				
				val v1 = bc.getPosition(t.toDouble())
				val v2 = bc.getPosition(t2.toDouble())
				val diff = v2.subtract(v1)
				val normal = bc.getNormal(t1.toDouble())
				val angles = TrackRenderer.getModelAngles(normal, diff)
				
				val anchor = v1.add(diff.scale(.5))
				var localOrigin = origin.subtract(anchor)
				var localDirection = rayDirection
				localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.x).toDouble(), Direction.Axis.X)
				localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.y).toDouble(), Direction.Axis.Y)
				localDirection =
					VecHelper.rotate(localDirection, AngleHelper.deg(-angles.x).toDouble(), Direction.Axis.X)
				localDirection =
					VecHelper.rotate(localDirection, AngleHelper.deg(-angles.y).toDouble(), Direction.Axis.Y)
				
				val clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection))
					.orElse(null) ?: continue
				
				val distance = clip.distanceToSqr(0.0, 0.25, 0.0)
				if(bestSegment != -1 && bestDistance < distance) continue
				
				val distanceToSqr = clip.distanceToSqr(localOrigin)
				if(distanceToSqr > maxRangeSqr) continue
				
				bestSegment = i
				newMaxRangeSqr = distanceToSqr
				bestDistance = distance
				
				val curveResult = PickCurveResult(
					curve = bc,
					segmentIndex = i,
					position = anchor,
					tangent = diff.normalize(),
					normal = normal.normalize(),
					angles = angles,
				)
				updateResult(index, curveResult)
			}
			
			if(bestSegment != -1) maxRangeSqr = newMaxRangeSqr
		}
	}
	
	fun getShape(material: TrackMaterial, direction: Direction): VoxelShape {
		var shape = CreateShapes.TRACK_ORTHO[direction]
		if(CompatMods.railways) shape = when(material.trackType) {
			CRTrackMaterials.CRTrackType.MONORAIL -> CRShapes.MONORAIL_TRACK_ORTHO[direction]
			CRTrackMaterials.CRTrackType.NARROW_GAUGE -> CRShapes.NARROW_TRACK_ORTHO[direction]
			else -> shape
		}
		return shape
	}
}
