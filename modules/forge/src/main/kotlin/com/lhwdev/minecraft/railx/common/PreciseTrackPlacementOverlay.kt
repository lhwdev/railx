package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.flexiTrack.*
import com.lhwdev.minecraft.railx.flexiTrack.rotate.toRotation
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.utils.pow3
import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.content.trains.track.TrackPlacement
import net.createmod.catnip.outliner.Outliner
import net.minecraft.ChatFormatting
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RenderGuiEvent
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.*
import java.lang.invoke.MethodHandles
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.roundToInt


private val lookup = MethodHandles.lookup()

private val TrackPlacement_cached =
	lookup.unreflectGetter(TrackPlacement::class.java.getDeclaredField("cached").also { it.isAccessible = true })

private val TrackPlacement_hoveringPos =
	lookup.unreflectGetter(TrackPlacement::class.java.getDeclaredField("hoveringPos").also { it.isAccessible = true })


@OnlyIn(Dist.CLIENT)
object PreciseTrackPlacementOverlay : LayeredDraw.Layer {
	var info: PreciseInfo? = null
	
	val infoLineCount: Int
		get() = info?.lines?.size ?: 0
	
	fun onPreRender(event: RenderGuiEvent.Pre) {
		info = preciseInfo()
	}
	
	private fun preciseInfo(): PreciseInfo? {
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return null
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return null
		
		fun flexiTrackPlacementInfo(handItem: TrackBlockItem): PrecisePlacementInfo? =
			FlexiTrackPlacementClient.lastOverlay?.let {
				PrecisePlacementInfo(from = it.from.toPoint(), to = it.to.toPoint(), curve = it.curve)
			}
		
		fun createTrackPlacementInfo(handItem: TrackBlockItem): PrecisePlacementInfo? {
			if(handItem is FlexiTrackBlockItem) return null
			val info = TrackPlacement_cached.invokeExact() as TrackPlacement.PlacementInfo? as? PlacementInfoAccessor
			if(info?.curve == null) return null
			
			// do not change to, e.g, if(invokeExact() !is BlockPos)
			val hoveringPos: BlockPos? = TrackPlacement_hoveringPos.invokeExact() as BlockPos?
			@Suppress("FoldInitializerAndIfToElvis", "RedundantSuppression")
			if(hoveringPos == null) return null
			
			return PrecisePlacementInfo(
				from = FlexiPlacementInfo.TrackPoint(
					pos = info.pos1 ?: BlockPos.ZERO,
					tangent = info.axis1 ?: Vec3.ZERO,
					normal = info.normal1 ?: Vec3.ZERO,
				),
				to = FlexiPlacementInfo.TrackPoint(
					pos = info.pos2 ?: BlockPos.ZERO,
					tangent = info.axis2 ?: Vec3.ZERO,
					normal = info.normal2 ?: Vec3.ZERO,
				),
				curve = info.curve,
			)
		}
		
		fun trackCurveInfo(): PreciseTrackInfo? {
			val point = TracksOutline.result ?: return null
			val direction = FlexiDirection.Two(
				point.tangent,
				point.curve.getNormal(point.curve.getSegmentT(point.segmentIndex).toDouble()),
			)
			return PreciseTrackInfo(point.position, direction, curvePoint = point)
		}
		
		fun trackBlockInfo(hand: InteractionHand, handItem: TrackBlockItem): PreciseTrackInfo? {
			val hitResult = mc.hitResult as? BlockHitResult ?: return null
			val level = mc.level ?: return null
			var pos = hitResult.blockPos
			var state = level.getBlockState(pos)
			var virtual = false
			
			if(state.block !is ITrackBlock && !state.canBeReplaced()) {
				pos = pos.relative(hitResult.direction)
				state = handItem.getPlacementState(UseOnContext(player, hand, hitResult))
					?: return null
				virtual = true
			}
			
			val track = state.block as? ITrackBlock ?: return null
			return PreciseTrackInfo(
				pos = pos.bottomCenter,
				direction = if(track is FlexiTrackBlock) {
					if(virtual) {
						FlexiDirection.Known.roundFrom(player.lookAngle)
					} else {
						track.getNearestTrackDirection(level, pos, state, player.lookAngle)
							?: return null
					}
				} else {
					val tangent = track.getNearestTrackAxis(level, pos, state, player.lookAngle)
					FlexiDirection.Two(tangent.first.normalize(), track.getUpNormal(level, pos, state).normalize())
						.optimize()
				},
				virtual = virtual,
			)
		}
		val (hand, handItem) = InteractionHand.entries.map { it to player.getItemInHand(it) }
			.firstNotNullOfOrNull { (it.second.item as? TrackBlockItem)?.let { item -> it.first to item } }
			?: return null
		
		flexiTrackPlacementInfo(handItem)?.let { return it }
		createTrackPlacementInfo(handItem)?.let { return it }
		
		trackCurveInfo()?.let { return it }
		trackBlockInfo(hand, handItem)?.let { return it }
		return null
	}
	
	
	override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
		val info = info ?: return
		
		val mc = Minecraft.getInstance()
		val window = mc.window
		for((index, line) in info.lines.withIndex()) guiGraphics.drawCenteredString(
			mc.font,
			line,
			window.guiScaledWidth / 2,
			window.guiScaledHeight - 61 + index * 9,
			0xffffffffu.toInt(),
		)
		info.renderExtra(guiGraphics, deltaTracker)
	}
	
	
	sealed class PreciseInfo {
		abstract val targetTrack: TrackPoint
		
		abstract val lines: List<Component>
		
		open fun renderExtra(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {}
		
		class TrackPoint(val pos: Vec3, val tangent: Vec3)
	}
	
	class PrecisePlacementInfo(
		val from: FlexiPlacementInfo.TrackPoint,
		val to: FlexiPlacementInfo.TrackPoint,
		val curve: BezierConnection?,
	) : PreciseInfo() {
		override val targetTrack = TrackPoint(to.pos.toVec3(), to.tangent)
		
		override val lines = mutableListOf<Component>()
		
		init {
			val delta = to.pos - from.pos
			val curve = curve
			
			val line = Component.empty()
			line.append("Axis = ${displayPoint(direction(from.tangent, from.normal))}")
				.append(" -> ${displayPoint(direction(to.tangent, to.normal))}")
			line.append(", Delta = ${with(delta) { "[$x, $y, $z]" }}")
			if(curve != null) line.append(", R = ${minRadius(curve)}")
			if(delta.y != 0) line.append(", Grad = ${delta.toVec3().gradient()}")
			lines += line
			
			val onStraightLine = to.tangent.cross(delta.toVec3()).length() < 0.001
			if(curve != null && !onStraightLine) {
				val line2 = Component.empty()
				line2.append("End Radius = ${radiusAt(curve, offset = 0.0)}")
					.append(" -> ${radiusAt(curve, offset = 1.0)}")
				lines += line2
			}
		}
	}
	
	class PreciseTrackInfo(
		val pos: Vec3,
		val direction: FlexiDirection,
		val curvePoint: TrackBezierPointSelection? = null,
		virtual: Boolean = false,
	) : PreciseInfo() {
		override val targetTrack = TrackPoint(pos, direction.tangent)
		
		override val lines = mutableListOf<Component>()
		
		init {
			lines += Component.literal("Axis = ${displayPoint(direction)}")
			
			if(curvePoint != null) {
				val line = Component.empty()
				val curve = curvePoint.curve
				val delta = curve.bePositions.second - curve.bePositions.first
				
				line.append(Component.literal("This Curve: ").withStyle(ChatFormatting.WHITE))
				line.append("Axis = ${displayPoint(direction(curve.axes.first, curve.normals.first))}")
					.append(" -> ${displayPoint(direction(curve.axes.second, curve.normals.second))}")
				line.append(", Delta = ${with(delta) { "[$x, $y, $z]" }}")
				line.append(", R = ${minRadius(curve)}")
				if(delta.y != 0) line.append(", Grad = ${delta.toVec3().gradient()}")
				lines += line.withStyle(ChatFormatting.AQUA)
				
				val onStraightLine = curve.axes.second.cross(delta.toVec3()).length() < 0.001
				if(!onStraightLine) {
					val line2 = Component.empty()
					line2.append("End Radius = ${radiusAt(curve, offset = 0.0)}")
						.append(" -> ${radiusAt(curve, offset = 1.0)}")
					lines += line2.withStyle(ChatFormatting.AQUA)
				}
			}
			
			if(virtual) {
				val center = pos.add(0.0, 1.0 / 16.0, 0.0)
				val tangent = direction.tangent
				Outliner.getInstance()
					.showLine("precise_track", center - tangent * 0.5, center + tangent * 0.5)
					.colored(0xe0edff)
					.disableLineNormals()
					.lineWidth(1 / 8f)
			}
		}
	}
	
	
	private fun direction(tangent: Vec3, normal: Vec3) = when {
		normal.x similarTo 0.0 && normal.z similarTo 0.0 -> FlexiDirection.FlatImpl(tangent.normalize())
		else -> FlexiDirection.Two(tangent.normalize(), normal.normalize())
	}
	
	private fun displayPoint(direction: FlexiDirection): String {
		val tangent = direction.tangent
		val normal = direction.normal
		val rot = direction.toRotation()
		val radToDeg = 180 / PI
		val angle = "${((rot.direction * radToDeg + 360) % 360).roundToInt()}°"
		if(normal.x != 0.0 || normal.z != 0.0) {
			return "$angle (Grad=${tangent.gradient()}, Tilt=${round(rot.tilt * radToDeg, 100)})"
		} else {
			val knownLength = log10(FlexiDirection.Known.DivisionCount.toDouble()).toInt() + 1
			tangent.asKnown()
				?.let { return "K${it.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
			(-tangent).asKnown()
				?.let { return "K${it.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
			return angle
		}
	}
	
	private fun Vec3.gradient(): String {
		val mille = 1000 * y / horizontalDistance()
		return "${round(mille, 100)}‰"
	}
	
	private fun radiusAt(curve: BezierConnection, offset: Double): String {
		val derivative = curve.derivative(offset)
		val derivative2 = curve.derivative2(offset)
		val radius = derivative.length().pow3() / derivative.cross(derivative2).length()
		return if(radius.isFinite()) "R=${radius.roundToInt()}" else "R=?"
	}
	
	private fun minRadius(curve: BezierConnection): String {
		val minRadius = curve.minRadius()
		return if(minRadius > 100000.0 || !minRadius.isFinite()) "∞" else "${minRadius.roundToInt()}"
	}
}

private fun round(value: Double, points: Int): Double =
	round(value * points) / points
