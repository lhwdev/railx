package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.flexiTrack.*
import com.lhwdev.minecraft.railx.flexiTrack.rotate.toRotation
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.utils.similarTo
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
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.*
import java.lang.invoke.MethodHandles
import kotlin.math.*


private val lookup = MethodHandles.lookup()

private val TrackPlacement_cached =
	lookup.unreflectGetter(TrackPlacement::class.java.getDeclaredField("cached").also { it.isAccessible = true })

private val TrackPlacement_hoveringPos =
	lookup.unreflectGetter(TrackPlacement::class.java.getDeclaredField("hoveringPos").also { it.isAccessible = true })


@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object PreciseTrackPlacementOverlay : LayeredDraw.Layer {
	var info: PreciseInfo? = null
	
	@SubscribeEvent
	private fun onPreRender(event: RenderGuiEvent.Pre) {
		info = preciseInfo()
	}
	
	private fun preciseInfo(): PreciseInfo? {
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return null
		if(mc.options.hideGui || mc.gameMode?.playerMode == GameType.SPECTATOR) return null
		
		fun flexiTrackPlacementInfo(handItem: TrackBlockItem) = FlexiTrackPlacementClient.lastOverlay?.let {
			if(handItem !is FlexiTrackBlockItem) return null
			PrecisePlacementInfo(from = it.from.toPoint(), to = it.to.toPoint(), curve = it.curve)
		}
		
		fun createTrackPlacementInfo(handItem: TrackBlockItem): PrecisePlacementInfo? {
			if(handItem is FlexiTrackBlockItem) return null
			val info = TrackPlacement_cached.invokeExact() as TrackPlacement.PlacementInfo? as? PlacementInfoAccessor
			if(info?.curve == null) return null
			
			val hoveringPos = TrackPlacement_hoveringPos.invokeExact() as BlockPos?
			if(hoveringPos == null) return null
			
			return PrecisePlacementInfo(
				from = FlexiTrackPlacement.TrackPoint(
					pos = info.pos1 ?: BlockPos.ZERO,
					tangent = info.axis1 ?: Vec3.ZERO,
					normal = info.normal1 ?: Vec3.ZERO,
				),
				to = FlexiTrackPlacement.TrackPoint(
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
			return PreciseTrackInfo(point.position, direction)
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
				},
				virtual,
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
		when(val info = info ?: return) {
			is PrecisePlacementInfo -> renderPlacement(info, guiGraphics, deltaTracker)
			is PreciseTrackInfo -> renderTrack(info, guiGraphics, deltaTracker)
		}
	}
	
	private fun renderPlacement(info: PrecisePlacementInfo, guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
		val mc = Minecraft.getInstance()
		val delta = info.to.pos - info.from.pos
		
		val text = Component.empty()
		text.append("Axis = ${displayPoint(direction(info.from.tangent, info.from.normal))}")
			.append(" -> ${displayPoint(direction(info.to.tangent, info.to.normal))}")
		text.append(", Delta = ${with(delta) { "[$x, $y, $z]" }}")
		if(info.curve != null) text.append(", R = ${floor(info.curve.radius).toInt()}")
		if(delta.y != 0) text.append(", Grad = ${delta.toVec3().gradient()} ")
		
		val window = mc.window
		val y = window.guiScaledHeight - 61
		guiGraphics.drawCenteredString(mc.font, text, window.guiScaledWidth / 2, y, 0xffffffffu.toInt())
	}
	
	private fun renderTrack(info: PreciseTrackInfo, guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
		val mc = Minecraft.getInstance()
		val text = Component.empty()
		text.append("Axis = ${displayPoint(info.direction)}")
		
		val window = mc.window
		val y = window.guiScaledHeight - 61
		guiGraphics.drawCenteredString(mc.font, text, window.guiScaledWidth / 2, y, 0xffffffffu.toInt())
		
		if(info.virtual) {
			val center = info.pos.add(0.0, 1.0 / 16.0, 0.0)
			val tangent = info.direction.tangent
			Outliner.getInstance()
				.showLine("precise_track", center - tangent * 0.5, center + tangent * 0.5)
				.colored(0xe0edff)
				.disableLineNormals()
				.lineWidth(1 / 8f)
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
			tangent.asKnownVec3()
				?.let { return "K${it.known.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
			(-tangent).asKnownVec3()
				?.let { return "K${it.known.ordinal.toString().padStart(knownLength, padChar = '0')}($angle)" }
			return angle
		}
	}
	
	private fun Vec3.gradient(): String {
		val mille = 1000 * y / horizontalDistance()
		return "${round(mille, 100)}‰"
	}
	
	sealed class PreciseInfo {
		abstract val targetTrack: TrackPoint
		
		class TrackPoint(val pos: Vec3, val tangent: Vec3)
	}
	
	class PrecisePlacementInfo(
		val from: FlexiTrackPlacement.TrackPoint,
		val to: FlexiTrackPlacement.TrackPoint,
		val curve: BezierConnection?,
	) : PreciseInfo() {
		override val targetTrack = TrackPoint(to.pos.toVec3(), to.tangent)
	}
	
	class PreciseTrackInfo(val pos: Vec3, val direction: FlexiDirection, val virtual: Boolean = false) : PreciseInfo() {
		override val targetTrack = TrackPoint(pos, direction.tangent)
	}
}

private fun round(value: Double, points: Int): Double =
	round(value * points) / points
