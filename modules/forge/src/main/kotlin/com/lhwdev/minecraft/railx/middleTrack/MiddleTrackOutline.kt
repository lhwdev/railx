package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.common.TrackBezierPointSelection
import com.lhwdev.minecraft.railx.common.primaryPositions
import com.lhwdev.minecraft.utils.vectors.minus
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.utility.RaycastHelper
import dev.engine_room.flywheel.lib.transform.TransformStack
import io.netty.buffer.ByteBuf
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.PI
import kotlin.math.min
import com.simibubi.create.AllShapes as CreateShapes
import com.simibubi.create.AllTags as CreateTags


class MiddleBezierPointSelection(
	override val curve: BezierConnection,
	override val segmentIndex: Int,
	override val position: Vec3,
	override val angles: Vec3,
	override val tangent: Vec3,
	
	val bezierSource: MiddleBezierSource,
) : TrackBezierPointSelection {
	val fromPos: BlockPos
		get() = curve.bePositions.first
	val toPos: BlockPos
		get() = curve.bePositions.second
	
	fun toCreateTrackPointLocation(): BezierTrackPointLocation =
		BezierTrackPointLocation(toPos, segmentIndex)
}

class MiddleBezierSource(val middlePos: BlockPos, val index: Int) {
	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, MiddleBezierSource> = StreamCodec.composite(
			BlockPos.STREAM_CODEC, MiddleBezierSource::middlePos,
			ByteBufCodecs.VAR_INT, MiddleBezierSource::index,
			::MiddleBezierSource,
		)
	}
	
	fun resolveCurve(level: BlockGetter): BezierConnection? =
		(level.getBlockEntity(middlePos) as? MiddleTrackLikeBlockEntity)?.let { it.connectionValues[index] }
			?.let { if(it.primary) it else it.secondary() }
}


@OnlyIn(Dist.CLIENT)
object MiddleTrackOutline {
	var result: MiddleBezierPointSelection? = null
	
	
	fun pickCurves() {
		result = null
		if(!MiddleTrackInteraction.enabled) return
		
		if(TrackBlockOutline.result != null) return
		
		val mc = Minecraft.getInstance()
		val player = mc.cameraEntity as? LocalPlayer ?: return
		val level = mc.level ?: return
		
		val origin = player.getEyePosition(AnimationTickHolder.getPartialTicks(level))
		
		val hitResult = mc.hitResult
		var maxRange = hitResult?.location?.distanceToSqr(origin) ?: Double.MAX_VALUE
		
		val range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
		val target = RaycastHelper.getTraceTarget(player, min(maxRange, range) + 1, origin)
		val connections = GlobalConnections[level]
		
		val standardSegmentBounds = CreateShapes.TRACK_ORTHO[Direction.SOUTH].bounds()
			.let { it.move(-.5, it.ysize / -2, -.5) }
		
		for(connection in connections) {
			if(!connection.isActive) continue
			val bc = connection.curve
			if(!bc.isPrimary) continue
			
			val bounds = bc.bounds
			if(!bounds.contains(origin) && bounds.clip(origin, target).isEmpty) continue
			
			val stepLUT = bc.stepLUT
			val segments = (bc.length * 2).toInt()
			
			var bestSegment = -1
			var bestDistance = Double.MAX_VALUE
			var newMaxRange = maxRange
			
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
				
				if(bestSegment != -1 && bestDistance < clip.get().distanceToSqr(0.0, 0.25, 0.0)) continue
				
				val distanceToSqr = clip.get().distanceToSqr(localOrigin)
				if(distanceToSqr > maxRange) continue
				
				bestSegment = i
				newMaxRange = distanceToSqr
				bestDistance = clip.get().distanceToSqr(0.0, 0.25, 0.0)
				
				val middlePos = connection.allMiddles.minBy { it.distToCenterSqr(player.position()) }
				val middle = level.getBlockEntity(middlePos) as? MiddleTrackLikeBlockEntity ?: continue
				result = MiddleBezierPointSelection(
					curve = bc,
					segmentIndex = i,
					position = anchor,
					angles = angles,
					tangent = diff.normalize(),
					bezierSource = MiddleBezierSource(
						middlePos = middlePos,
						index = middle.connectionValues.indexOfFirst { it.primaryPositions == bc.bePositions },
					),
				)
			}
			
			if(bestSegment != -1) maxRange = newMaxRange
		}
		
		if(result == null) return
		if(hitResult != null && hitResult.type != HitResult.Type.MISS) {
			val priorLoc = hitResult.location
			mc.hitResult = BlockHitResult.miss(priorLoc, Direction.UP, BlockPos.containing(priorLoc))
		}
	}
	
	
	fun drawCurveSelection(ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) {
		val mc = Minecraft.getInstance()
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return
		
		val result = result ?: return
		
		val vb = buffer.getBuffer(RenderType.lines())
		val vec = result.position - camera
		val angles = result.angles
		TransformStack.of(ms)
			.pushPose()
			.translate(vec.x, vec.y + .125f, vec.z)
			.rotateY(angles.y.toFloat() + (PI * 0.5).toFloat())
			.rotateX(angles.x.toFloat())
			.translate(-.5, -.125, -.5)
		
		val holdingTrack = CreateTags.AllBlockTags.TRACKS.matches(Minecraft.getInstance().player!!.mainHandItem)
		val shape = getShape(result.curve.material, direction = Direction.EAST)
		TrackBlockOutline.renderShape(shape, ms, vb, if(holdingTrack) false else null)
		
		ms.popPose()
	}
	
	private fun getShape(material: TrackMaterial, direction: Direction): VoxelShape {
		var shape = CreateShapes.TRACK_ORTHO[direction]
		// if(CompatMods.railways) shape = when(material.trackType) {
		// 	CRTrackMaterials.CRTrackType.MONORAIL -> CRShapes.MONORAIL_TRACK_ORTHO[direction]
		// 	CRTrackMaterials.CRTrackType.NARROW_GAUGE -> CRShapes.NARROW_TRACK_ORTHO[direction]
		// 	else -> shape
		// }
		return shape
	}
}
