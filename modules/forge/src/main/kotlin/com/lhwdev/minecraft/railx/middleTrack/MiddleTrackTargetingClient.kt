package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.flexiTrack.defaultBlockState
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.track.*
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.PoseTransformStack
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.roundToInt
import com.simibubi.create.AllDataComponents as CreateDataComponents


@OnlyIn(Dist.CLIENT)
object MiddleTrackTargetingClient {
	@Suppress("EqualsOrHashCode")
	class Target(
		val fromPos: BlockPos,
		val bezier: BezierTrackPointLocation,
		val direction: Boolean,
		val edgeType: EdgePointType<*>,
	) {
		var location: MiddleTrackInteraction.GraphLocation? = null
		
		val directionAxis: Direction.AxisDirection
			get() = if(direction) Direction.AxisDirection.POSITIVE else Direction.AxisDirection.NEGATIVE
		
		fun getCurve(level: LevelAccessor): BezierConnection? =
			GlobalConnections[level][fromPos, bezier.curveTarget]?.curve
		
		val renderedOverlayType: TrackTargetingBehaviour.RenderedTrackOverlayType
			get() = when(edgeType) {
				EdgePointType.SIGNAL -> TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL
				EdgePointType.OBSERVER -> TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER
				EdgePointType.STATION -> TrackTargetingBehaviour.RenderedTrackOverlayType.STATION
				else -> TrackTargetingBehaviour.RenderedTrackOverlayType.entries
					.find { it.name.equals(edgeType.id.path, ignoreCase = true) }
					?: TrackTargetingBehaviour.RenderedTrackOverlayType.STATION
			}
		
		override fun equals(other: Any?): Boolean = when {
			this === other -> true
			other !is Target -> false
			else -> fromPos == other.fromPos &&
				bezier == other.bezier &&
				direction == other.direction &&
				edgeType == other.edgeType
		}
	}
	
	
	var currentSelected: Target? = null
	
	fun clientTick() {
		val previousResult = currentSelected
		currentSelected = null
		if(!MiddleTrackInteraction.enabled) return
		
		val mc = Minecraft.getInstance()
		val level = mc.level ?: return
		val player = mc.player ?: return
		val lookAngle = player.lookAngle
		
		val stack = player.mainHandItem
		val item = stack.item as? TrackTargetingBlockItem ?: return
		val type = item.getType(stack)
		
		val fromPos = stack.get(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)
		val selection = MiddleTrackOutline.result
		val target = when {
			// already selected
			fromPos != null && !level.isLoaded(fromPos) -> Target(
				fromPos = fromPos,
				bezier = stack.get(CreateDataComponents.TRACK_TARGETING_ITEM_BEZIER) ?: return,
				direction = stack.getOrDefault(CreateDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, false),
				edgeType = type,
			)
			
			selection != null -> Target(
				fromPos = selection.fromPos,
				bezier = selection.toCreateTrackPointLocation(),
				direction = lookAngle.dot(selection.tangent) < 0,
				edgeType = type,
			)
			
			else -> null
		}
		
		
		if(target == previousResult) {
			currentSelected = previousResult
			return
		}
		currentSelected = target
		if(target == null) return
		
		val connection = GlobalConnections[level][target.fromPos, target.bezier.curveTarget] ?: return
		target.location = MiddleTrackInteraction.withGraphLocation(
			level = level,
			curve = connection.curve,
			targetBezier = target.bezier,
			type = target.edgeType,
			front = target.direction,
		)
	}
	
	fun render(ms: PoseStack, buffer: MultiBufferSource, camera: Vec3) {
		val target = currentSelected ?: return
		target.location ?: return
		
		ms.pushPose()
		TransformStack.of(ms)
			.translate(-camera)
		
		renderFor(target, ms, buffer, scale = 1 + 1 / 16f)
		
		ms.popPose()
	}
	
	
	private fun renderFor(
		target: Target,
		ms: PoseStack,
		buffer: MultiBufferSource,
		scale: Float,
	) {
		val mc = Minecraft.getInstance()
		val level = mc.level!!
		val curve = target.getCurve(level) ?: return
		
		ms.pushPose()
		val msr = TransformStack.of(ms)
		val partial = prepareTrackOverlayOnCurve(
			ms = msr,
			curve = curve,
			segmentIndex = target.bezier.segment,
			direction = target.directionAxis,
			type = target.renderedOverlayType,
		)
		
		if(partial != null) {
			val nearestPos = curve.getPosition(curve.getSegmentT(target.bezier.segment + 1).toDouble())
				.let { BlockPos(it.x.roundToInt(), Mth.ceil(it.y), it.z.roundToInt()) }
			
			CachedBuffers.partial(partial, curve.material.defaultBlockState())
				.translate(0.5, 0.0, 0.5)
				.scale(scale)
				.translate(-0.5, 0.0, -0.5)
				.light<SuperByteBuffer>(LevelRenderer.getLightColor(level, nearestPos))
				.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()))
		}
		
		ms.popPose()
	}
	
	
	private fun prepareTrackOverlayOnCurve(
		ms: PoseTransformStack,
		curve: BezierConnection,
		segmentIndex: Int,
		direction: Direction.AxisDirection,
		type: TrackTargetingBehaviour.RenderedTrackOverlayType,
	): PartialModel? {
		val length = Mth.floor(curve.length * 2).toDouble()
		val seg = segmentIndex + 1
		val t = seg / length
		val tPre = (seg - 1) / length
		val tPost = (seg + 1) / length
		
		val offset = curve.getPosition(t)
		val normal = curve.getNormal(t)
		val diff = curve.getPosition(tPost)
			.subtract(curve.getPosition(tPre))
			.normalize()
		
		
		ms.translate(offset)
		ms.translate(-0.5f, -4 / 16f, -0.5f)
		
		val angles = TrackRenderer.getModelAngles(normal, diff)
		
		ms.center()
			.rotateY(angles.y.toFloat())
			.rotateX(angles.x.toFloat())
			.uncenter()
		
		ms.translate(0f, 4 / 16f, 0f)
		if(direction == Direction.AxisDirection.NEGATIVE) {
			ms.rotateCentered(Mth.PI, Direction.UP)
		}
		
		return when(type) {
			TrackTargetingBehaviour.RenderedTrackOverlayType.DUAL_SIGNAL -> AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER -> AllPartialModels.TRACK_OBSERVER_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL -> AllPartialModels.TRACK_SIGNAL_OVERLAY
			TrackTargetingBehaviour.RenderedTrackOverlayType.STATION -> AllPartialModels.TRACK_STATION_OVERLAY
		}
	}
	
}
