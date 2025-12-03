package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.common.TrackBezierPointSelection
import com.lhwdev.minecraft.railx.common.primaryPositions
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVoxelShapes
import com.lhwdev.minecraft.utils.vectors.minus
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackBlockOutline
import com.simibubi.create.content.trains.track.TrackRenderer
import com.simibubi.create.foundation.utility.RaycastHelper
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.ForgeMod
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
	fun write(buffer: FriendlyByteBuf) {
		buffer.writeBlockPos(middlePos)
		buffer.writeVarInt(index)
	}
	
	companion object {
		fun read(buffer: FriendlyByteBuf): MiddleBezierSource = MiddleBezierSource(
			middlePos = buffer.readBlockPos(),
			index = buffer.readVarInt(),
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
		
		val range = player.getAttributeValue(ForgeMod.BLOCK_REACH.get())
		val target = RaycastHelper.getTraceTarget(player, min(maxRange, range) + 1, origin)
		val connections = GlobalConnections[level]
		
		val segmentBounds = CreateShapes.TRACK_ORTHO[Direction.SOUTH].bounds()
			.let { it.move(-.5, it.ysize / -2, -.5) }
		
		for(connection in connections) {
			if(!connection.isActive) continue
			val bc = connection.curve
			if(!bc.isPrimary) continue
			
			val bounds = bc.bounds
			if(!bounds.contains(origin) && bounds.clip(origin, target).isEmpty) continue
			
			val stepLUT = bc.stepLUT
			val segments = (bc.getLength() * 2).toInt()
			
			var bestSegment = -1
			var bestDistance = Double.MAX_VALUE
			var newMaxRange = maxRange
			
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
				
				if(bestSegment != -1 && bestDistance < clip.get().distanceToSqr(0.0, 0.25, 0.0)
				) continue
				
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
		TrackBlockOutline.renderShape(FlexiTrackVoxelShapes.base, ms, vb, if(holdingTrack) false else null)
		
		ms.popPose()
	}
}
