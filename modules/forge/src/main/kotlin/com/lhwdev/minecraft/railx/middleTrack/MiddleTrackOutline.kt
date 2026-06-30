package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.common.CommonTrackBlockOutline
import com.lhwdev.minecraft.railx.common.TrackBezierPointSelection
import com.lhwdev.minecraft.railx.common.primaryPositions
import com.lhwdev.minecraft.utils.vectors.minus
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackBlockOutline
import com.simibubi.create.foundation.utility.RaycastHelper
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.animation.AnimationTickHolder
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
import kotlin.math.sqrt
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
		val maxRange = hitResult?.location?.distanceToSqr(origin)?.let(::sqrt) ?: Double.MAX_VALUE
		
		val range = player.getAttributeValue(ForgeMod.BLOCK_REACH.get())
		val target = RaycastHelper.getTraceTarget(player, min(maxRange, range) + 1, origin)
		val connections = GlobalConnections[level].toList()
		
		CommonTrackBlockOutline.pickCurves(
			connections = connections.map { it.curve },
			origin = origin,
			target = target,
			maxRange = maxRange,
		) { index, pick ->
			val connection = connections[index]
			val middlePos = connection.allMiddles.minBy { it.distToCenterSqr(player.position()) }
			val middle = level.getBlockEntity(middlePos) as? MiddleTrackLikeBlockEntity ?: return@pickCurves
			
			result = MiddleBezierPointSelection(
				curve = pick.curve,
				segmentIndex = pick.segmentIndex,
				position = pick.position,
				angles = pick.angles,
				tangent = pick.tangent,
				bezierSource = MiddleBezierSource(
					middlePos = middlePos,
					index = middle.connectionValues.indexOfFirst { it.primaryPositions == pick.curve.bePositions },
				),
			)
			
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
		val shape = CommonTrackBlockOutline.getShape(result.curve.material, direction = Direction.EAST)
		TrackBlockOutline.renderShape(shape, ms, vb, if(holdingTrack) false else null)
		
		ms.popPose()
	}
}
