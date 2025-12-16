package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.flexiTrack.*
import com.lhwdev.minecraft.railx.flexiTrack.rotate.toRotation
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.utils.ColorsArgb
import com.lhwdev.minecraft.railx.utils.orFalse
import com.lhwdev.minecraft.railx.utils.round
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.*
import com.simibubi.create.AllDataComponents
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.content.trains.track.TrackPlacement
import net.createmod.catnip.outliner.Outliner
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RenderGuiEvent
import java.lang.invoke.MethodHandles
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
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
		if(!RailXConfig.Client.common.preciseOverlay.orFalse) return null
		
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return null
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return null
		
		class HandItem<T>(val hand: InteractionHand, val stack: ItemStack, val item: T)
		
		fun flexiTrackPlacementInfo(): PrecisePlacementInfo? =
			FlexiTrackPlacementClient.lastOverlay?.let {
				PrecisePlacementInfo(from = it.from.toPoint(), to = it.to.toPoint(), curve = it.curve)
			}
		
		fun createTrackPlacementInfo(handItem: HandItem<TrackBlockItem>): PrecisePlacementInfo? {
			if(handItem.item is FlexiTrackBlockItem) return null
			if(!handItem.stack.has(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)) return null
			
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
			var tilted = false
			return PreciseTrackInfo(
				pos = pos.bottomCenter,
				direction = if(track is FlexiTrackBlock) {
					if(virtual) {
						FlexiDirection.Known.roundFrom(player.lookAngle)
					} else {
						(level.getBlockEntity(pos) as? FlexiTrackBlockEntity)?.let { be ->
							tilted = be.state.tilt != null
						}
						track.getNearestTrackDirection(level, pos, state, player.lookAngle)?.axis
							?: return null
					}
				} else {
					val tangent = track.getNearestTrackAxis(level, pos, state, player.lookAngle)
					FlexiDirection.Two(tangent.first.normalize(), track.getUpNormal(level, pos, state).normalize())
						.optimize()
				},
				tilted = tilted,
				virtual = virtual,
			)
		}
		
		val handItem = InteractionHand.entries.map { it to player.getItemInHand(it) }
			.firstNotNullOfOrNull {
				(it.second.item as? TrackBlockItem)?.let { item -> HandItem(it.first, it.second, item) }
			}
			?: return null
		
		flexiTrackPlacementInfo()?.let { return it }
		createTrackPlacementInfo(handItem)?.let { return it }
		
		trackCurveInfo()?.let { return it }
		trackBlockInfo(handItem.hand, handItem.item)?.let { return it }
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
		val curve: BezierConnection,
	) : PreciseInfo() {
		override val targetTrack = TrackPoint(to.pos.toVec3(), to.tangent)
		
		override val lines = curveInfo(curve, title = "Placement")
	}
	
	class PreciseTrackInfo(
		val pos: Vec3,
		val direction: FlexiDirection,
		val curvePoint: TrackBezierPointSelection? = null,
		tilted: Boolean = false,
		virtual: Boolean = false,
	) : PreciseInfo() {
		override val targetTrack = TrackPoint(pos, direction.tangent)
		
		override val lines = mutableListOf<Component>()
		
		init {
			val first = Component.literal("Axis = ${displayPoint(direction)}")
			if(tilted) first.append(" (Tilted)")
			if(curvePoint != null) first.append(", R=")
				.append(
					valueStyle(
						curvePoint.curve.radiusTextAt(curvePoint.curve.getSegmentT(curvePoint.segmentIndex).toDouble())
					)
				)
			lines += first
			
			if(curvePoint != null) lines += curveInfo(
				curvePoint.curve,
				title = "Hovered Curve",
				baseColor = 0xcce8ff,
				bezierPoint = curvePoint,
			)
			
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
	
	
	private var contextColor: Int = 0
	
	private fun curveInfo(
		curve: BezierConnection,
		title: String,
		baseColor: Int = 0xffffff,
		bezierPoint: TrackBezierPointSelection? = null,
	): List<MutableComponent> {
		contextColor = baseColor
		
		val lines = mutableListOf<MutableComponent>()
		val delta = curve.bePositions.second - curve.bePositions.first
		
		val line = Component.empty()
		line.append(Component.literal(title))
		line.append(" | Axis: ")
			.append(valueStyle(displayPoint(direction(curve.axes.first, curve.normals.first))))
			.append(" -> ")
			.append(valueStyle(displayPoint(direction(curve.axes.second, curve.normals.second))))
		line.append(", Δ=")
			.append(valueStyle(with(delta) { "[$x, $y, $z]" }))
		line.append(", L=")
			.append(valueStyle(round(curve.length, 100).toString()))
		line.append(", R=")
			.append(valueStyle(minRadius(curve)))
		if(delta.y != 0) line.append(", Grad=")
			.append(valueStyle(delta.toVec3().gradient()))
		lines += line.withColor(baseColor)
		
		val onStraightLine = curve.axes.second.cross(delta.toVec3()).length() < 0.001
		if(!onStraightLine) {
			val line2 = Component.empty()
			val dimFrom = if(bezierPoint != null) curve.getSegmentT(bezierPoint.segmentIndex) > 0.5 else null
			
			fun dimTitle(name: String, from: Boolean) = if(dimFrom == null || from == dimFrom) {
				Component.literal(name)
			} else {
				Component.literal(name).withColor(0xffffff)
			}
			
			fun dimContent(from: Boolean, block: () -> MutableComponent) =
				if(dimFrom == from) withColor(ColorsArgb.multiply(baseColor, 0xcccccc), block) else block()
			
			line2.append(dimContent(from = true) {
				Component.empty()
					.append(dimTitle("From", from = true))
					.append(": R=")
					.append(valueStyle(curve.radiusTextAt(t = 0.0)))
			})
			line2.append(" -> ")
			line2.append(dimContent(from = false) {
				Component.empty()
					.append(dimTitle("To", from = false))
					.append(": R=")
					.append(valueStyle(curve.radiusTextAt(t = 1.0)))
			})
			
			if(bezierPoint != null) line2.append(", at ")
				.append(valueStyle(round(curve.lengthTo(segmentIndex = bezierPoint.segmentIndex + 1), 100).toString()))
			
			lines += line2.withColor(baseColor)
		}
		return lines
	}
	
	private inline fun withColor(color: Int, block: () -> MutableComponent): MutableComponent {
		val previous = contextColor
		contextColor = color
		return try {
			block().withColor(color)
		} finally {
			contextColor = previous
		}
	}
	
	private fun valueStyle(value: String): Component =
		Component.literal(value).withColor(ColorsArgb.multiply(contextColor, 0xffd1a6))
	
	private fun direction(tangent: Vec3, normal: Vec3) = when {
		normal.x similarTo 0.0 && normal.z similarTo 0.0 -> FlexiDirection.FlatImpl(tangent.normalize())
		else -> FlexiDirection.Two(tangent.normalize(), normal.normalize())
	}
	
	private fun displayPoint(direction: FlexiDirection): String {
		val tangent = direction.tangent
		val normal = direction.normal
		val rot = direction.toRotation()
		val radToDeg = 180 / PI
		val angle = "${((rot.direction * radToDeg + 360) % 180).roundToInt()}°"
		
		val knownLength = log10(FlexiDirection.Known.DivisionCount.toDouble()).toInt() + 1
		val known = tangent.asKnown()
			?.let { "K${it.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
			?: (-tangent).asKnown()
				?.let { "K${it.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
		
		if(abs(normal.x) > 1e-5 || abs(normal.z) > 1e-5) { // lesser than similarTo
			val info = "Grad=${tangent.gradient()}, Tilt=${round(rot.tilt * radToDeg, 100)}"
			return if(known != null) "$known ($angle, $info)" else "$angle ($info)"
		} else {
			return known ?: angle
		}
	}
	
	private fun Vec3.gradient(): String {
		val mille = 1000 * y / horizontalDistance()
		return "${round(mille, 100)}‰"
	}
	
	private fun BezierConnection.lengthTo(segmentIndex: Int): Double {
		val lut = stepLUT[segmentIndex] // lut = (t=i/segments) / (combinedDistance=lengthToSegment/length)
		val combinedDistance = (segmentIndex.toDouble() / segmentCount) / lut
		return combinedDistance * length
	}
	
	private fun BezierConnection.radiusTextAt(t: Double): String {
		val value = radiusAt(t)
		return when {
			value.isFinite() -> value.roundToInt().toString()
			value == Double.POSITIVE_INFINITY -> "∞"
			else -> "???"
		}
	}
	
	private fun minRadius(curve: BezierConnection): String {
		val minRadius = curve.minRadius()
		return if(minRadius > 100000.0 || !minRadius.isFinite()) "∞" else "${minRadius.roundToInt()}"
	}
}
