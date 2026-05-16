package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.common.minRadius
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.lhwdev.minecraft.railx.registry.AllKeys
import com.lhwdev.minecraft.railx.registry.AllSpecialTextures
import com.lhwdev.minecraft.railx.utils.ColorsArgb
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.plus
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockItem
import com.simibubi.create.content.trains.track.TrackPlacement
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.data.Pair
import net.createmod.catnip.math.VecHelper
import net.createmod.catnip.outliner.Outliner
import net.createmod.catnip.theme.Color
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.spongepowered.asm.mixin.injection.callback.Cancellable
import java.lang.invoke.MethodHandles
import java.util.*
import kotlin.math.max
import kotlin.math.min
import com.simibubi.create.AllTags as CreateTags


@OnlyIn(Dist.CLIENT)
object FlexiTrackPlacementClient {
	enum class FlexibleSelection { Hold, Toggle }
	
	private object Reflection {
		private val lookup = MethodHandles.lookup()
		val extraTipWarmup = TrackPlacement::class.java.getDeclaredField("extraTipWarmup")
			.also { it.isAccessible = true }
		val getExtraTipWarmup = lookup.unreflectGetter(extraTipWarmup)
		val setExtraTipWarmup = lookup.unreflectSetter(extraTipWarmup)
	}
	
	
	private var flexibleSelectionToggle = false
	
	val isFlexibleClient: Boolean
		get() {
			if(!RailXConfig.Server.flexiTrak.enabled.get()) return false
			return when(RailXConfig.Client.flexiTrak.flexibleSelection.get()) {
				FlexibleSelection.Hold -> AllKeys.FlexiblePlacement.isPressed
				FlexibleSelection.Toggle -> flexibleSelectionToggle
			}
		}
	
	
	var animation: LerpedFloat = LerpedFloat.linear()
		.startWithValue(0.0)
	var lastLineCount: Int = 0
	
	var hintPos: BlockPos? = null
	var hintAngle: Int = 0
	var hints: MutableList<Hint>? = null
	
	class Hint(val pos: BlockPos, val valid: Boolean, val curvature: Double)
	
	var lastOverlay: FlexiPlacementInfo? = null
	
	private val placementCache = PlacementCache()
	
	fun clientTick(defaultHandle: Cancellable) {
		lastOverlay = null
		if(!RailXConfig.Server.flexiTrak.enabled.get()) {
			hints = null
			return
		}
		
		if(
			RailXConfig.Client.flexiTrak.flexibleSelection.get() == FlexibleSelection.Toggle &&
			AllKeys.FlexiblePlacement.isKeyDown
		) {
			val player = Minecraft.getInstance().player
			if(player != null && CreateTags.AllBlockTags.TRACKS.matches(player.mainHandItem)) {
				flexibleSelectionToggle = !flexibleSelectionToggle
			}
		}
		
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return
		val level = player.level()
		
		var stack = player.mainHandItem
		val hitResult = mc.hitResult as? BlockHitResult ?: return
		
		if(!stack.hasFoil()) return
		
		var hand = InteractionHand.MAIN_HAND
		if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) {
			stack = player.offhandItem
			hand = InteractionHand.OFF_HAND
			if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) return
		}
		
		val blockItem = stack.item as? TrackBlockItem ?: return
		var pos = hitResult.blockPos
		var hitState = level.getBlockState(pos)
		
		if(hitState.block !is ITrackBlock && !hitState.canBeReplaced()) {
			pos = pos.relative(hitResult.direction)
			hitState = blockItem.getPlacementState(UseOnContext(player, hand, hitResult))
			if(hitState == null) return
		}
		
		val flexible = isFlexibleClient
		if(
			!flexible &&
			!FlexiTrackPlacement.isFlexiPlacementRequired(level, toState = hitState, stack)
		) return
		
		val track = hitState.block
		if(track !is ITrackBlock) return
		if(FlexiTrackMaterial.toFlexible(track) == null) return
		
		defaultHandle.cancel()
		
		
		placementCache.refresh()
		
		val parameter = run {
			val maxTurn = mc.options.keySprint.isDown
			FlexiTrackPlacement.Parameter(isFlexible = flexible, extend = flexible || maxTurn)
		}
		val info = FlexiTrackPlacement.resolveConnection(
			level = level,
			player = player,
			toPos = pos,
			toState = hitState,
			item = stack,
			cacheStorage = placementCache,
			parameter = parameter,
		)
		if(info !is FlexiPlacementInfo) {
			if(info is FlexiPlaceResult.PlaceError) {
				if(info is FlexiPlaceResult.PlaceError.SecondPoint) {
					player.displayClientMessage(info.message.withStyle(ChatFormatting.WHITE), true)
				} else {
					player.displayClientMessage(info.message.withStyle(ChatFormatting.RED), true)
				}
			}
			return
		}
		lastOverlay = info
		
		
		if(!player.isCreative && (info.valid || !info.hasRequiredTracks || !info.hasRequiredPavement)) BlueprintOverlayRenderer.displayTrackRequirements(
			TrackPlacement.PlacementInfo(info.material).apply {
				requiredTracks = info.requiredTracks
				requiredPavement = info.requiredPavement
				hasRequiredTracks = info.hasRequiredTracks
				hasRequiredPavement = info.hasRequiredPavement
			},
			player.offhandItem
		)
		
		val error = info.error
		when(error) {
			null -> player.displayClientMessage(
				CreateLang.translateDirect("track.valid_connection").withStyle(ChatFormatting.GREEN),
				true,
			)
			
			else -> player.displayClientMessage(error.message.withStyle(ChatFormatting.RED), true)
		}
		
		var hints = hints
		val overlayRange = RailXConfig.Client.flexiTrak.overlayWidth.get() - 1
		if(hitResult.direction == Direction.UP) {
			val lookVec = player.lookAngle
			val lookAngle = FlexiDirection.Known.roundFrom(lookVec).ordinal
			
			if(pos != hintPos || lookAngle != hintAngle) {
				hints = mutableListOf()
				hintAngle = lookAngle
				hintPos = pos
				
				for(xOffset in -overlayRange..overlayRange) {
					for(zOffset in -overlayRange..overlayRange) {
						val offset = pos.offset(xOffset, 0, zOffset)
						val adjInfo = FlexiTrackPlacement.resolveConnection(
							level = level,
							player = player,
							toPos = offset,
							toState = blockItem.getPlacementState(BlockPlaceContext(player, hand, stack, hitResult))
								?: continue,
							item = stack,
							cacheStorage = placementCache,
							parameter = parameter,
						)
						val curvature = if(adjInfo !is FlexiPlacementInfo) {
							0.0
						} else {
							val error = adjInfo.error
							val curve = adjInfo.curve
							val radius = curve?.minRadius() ?: Double.POSITIVE_INFINITY
							val maxRadius = 20000.0
							if(error != null && error.noOverlay) 0.0 else {
								-1 / (0.004 * min(radius, maxRadius) + 1) + 1 // some random function...
							}
						}
						hints += Hint(pos = offset.below(), valid = adjInfo.valid, curvature = curvature)
					}
				}
				this.hints = hints
			}
			
			if(hints != null) {
				var minCurvature = Double.POSITIVE_INFINITY
				var maxCurvature = Double.NEGATIVE_INFINITY
				var minErrorCurvature = Double.POSITIVE_INFINITY
				var maxErrorCurvature = Double.NEGATIVE_INFINITY
				for(hint in hints) {
					val curvature = hint.curvature
					if(hint.valid) {
						minCurvature = min(minCurvature, curvature)
						maxCurvature = max(maxCurvature, curvature)
					} else {
						if(curvature == 0.0) continue
						minErrorCurvature = min(minErrorCurvature, curvature)
						maxErrorCurvature = max(maxErrorCurvature, curvature)
					}
				}
				if(!minErrorCurvature.isFinite()) minErrorCurvature = 1.0
				if(!maxErrorCurvature.isFinite()) maxErrorCurvature = 1.0
				val curvatures = max(maxCurvature - minCurvature, 0.0001)
				val errorCurvatures = max(maxErrorCurvature - minErrorCurvature, 0.0001)
				for((index, hint) in hints.withIndex()) {
					val curvature = hint.curvature
					val w = (curvature - minCurvature) / curvatures
					val color = if(hint.valid) {
						if(w >= 0.9) ColorsArgb.lerp(
							0xA095CD41u.toInt(),
							0xC09AC960u.toInt(),
							w.toFloat().coerceIn(0f, 1f)
						) else ColorsArgb.lerp(
							0x4070BD3Au.toInt(),
							0xA095CD41u.toInt(),
							w.toFloat().coerceIn(0f, 1f)
						)
					} else {
						val e = (curvature - minErrorCurvature) / errorCurvatures
						if(curvature == 0.0) 0xA0FF4100u.toInt() else ColorsArgb.lerp(
							0x30FF4100u.toInt(),
							0x80FF4100u.toInt(),
							(1 - e).toFloat().coerceIn(0f, 1f),
						)
					}
					Outliner.getInstance().showCluster("track_$index", listOf(hint.pos))
						.withFaceTexture(AllSpecialTextures.BOLD_THIN_CHECKERED)
						.colored(Color(color))
						.lineWidth(0f)
				}
			}
		}
		
		animation.chase((if(info.valid) 1 else 0).toDouble(), 0.25, LerpedFloat.Chaser.EXP)
		animation.tickChaser()
		
		val railColor = ColorsArgb.lerp(0xEA5C2B, 0x95CD41, animation.getValue())
		val up = Vec3(0.0, (4 / 16f).toDouble(), 0.0)
		
		val railWidth = if(CompatMods.railways) when(info.material.trackType) {
			CRTrackMaterials.CRTrackType.WIDE_GAUGE -> 23 / 16.0
			CRTrackMaterials.CRTrackType.NARROW_GAUGE -> 8 / 16.0
			CRTrackMaterials.CRTrackType.MONORAIL -> 8 / 16.0
			else -> 15 / 16.0
		} else 15 / 16.0
		
		if(!info.valid) {
			info.fromExtent = 0
			info.toExtent = 0
		}
		
		run {
			val from = info.from
			val fromTangent = from.normalizedTangent
			val fromCross = from.normalizedNormal.cross(fromTangent).scale(railWidth)
			val o1 = fromTangent.scale(0.125)
			val ex1 = fromTangent.scale(info.fromExtent * info.from.tangent.length())
			line(1, info.from.end + fromCross + up, o1, ex1)
			line(2, info.from.end - fromCross + up, o1, ex1)
			
			val to = info.to
			val toTangent = to.normalizedTangent
			val toCross = to.normalizedNormal.cross(toTangent).scale(railWidth)
			val o2 = toTangent.scale(0.125)
			val ex2 = toTangent.scale(info.toExtent * info.to.tangent.length())
			line(3, info.to.end + toCross + up, o2, ex2)
			line(4, info.to.end - toCross + up, o2, ex2)
		}
		
		if(info.error?.noOverlay == true) return
		val bc = info.curve ?: return
		
		var previous1: Vec3? = null
		var previous2: Vec3? = null
		val segCount = bc.segmentCount
		
		val s = animation.value * 7 / 8f + 1 / 8f
		val lw = animation.value * 1 / 16f + 1 / 16f
		val end1 = bc.starts.first
		val end2 = bc.starts.second
		val finish1 = end1.add(bc.axes.first.scale(bc.handleLength))
		val finish2 = end2.add(bc.axes.second.scale(bc.handleLength))
		val key = "curve"
		
		for(i in 0..segCount) {
			val t = i / segCount.toFloat()
			val result = VecHelper.bezier(end1, end2, finish1, finish2, t)
			val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize()
			val normal = bc.getNormal(t.toDouble())
				.cross(derivative)
				.scale(railWidth)
			val rail1 = result.add(normal).add(up)
			val rail2 = result.subtract(normal).add(up)
			
			if(previous1 != null) {
				val middle1 = rail1.add(previous1).scale(0.5)
				val middle2 = rail2.add(previous2!!).scale(0.5)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2),
						VecHelper.lerp(s, middle1, previous1),
						VecHelper.lerp(s, middle1, rail1),
					)
					.colored(railColor)
					.disableLineNormals()
					.lineWidth(lw)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2 + 1),
						VecHelper.lerp(s, middle2, previous2),
						VecHelper.lerp(s, middle2, rail2),
					)
					.colored(railColor)
					.disableLineNormals()
					.lineWidth(lw)
			}
			
			previous1 = rail1
			previous2 = rail2
		}
		
		for(i in segCount + 1..lastLineCount) {
			Outliner.getInstance().remove(Pair.of(key, i * 2))
			Outliner.getInstance().remove(Pair.of(key, i * 2 + 1))
		}
		
		lastLineCount = segCount
	}
	
	private fun line(id: Int, center: Vec3, a: Vec3, b: Vec3) {
		val color = ColorsArgb.lerp(0xEA5C2B, 0x95CD41, animation.value)
		Outliner.getInstance().showLine(Pair.of("start", id), center.subtract(a), center.add(b))
			.lineWidth(1 / 8f)
			.disableLineNormals()
			.colored(color)
	}
}


private class PlacementCache : FlexiTrackPlacement.CacheStorage {
	class Cache(cached: FlexiPlaceResult, pos: BlockPos, angle: FlexiDirection.Known, lastItem: ItemStack) :
		FlexiTrackPlacement.Cache(cached, pos, angle, lastItem) {
		var state = true
	}
	
	val caches = LinkedList<Cache>()
	
	fun refresh() {
		val cacheIt = caches.iterator()
		while(cacheIt.hasNext()) {
			val cache = cacheIt.next()
			if(!cache.state) cacheIt.remove()
		}
		for(cache in caches) cache.state = false
	}
	
	override fun pull(pos: BlockPos, angle: FlexiDirection.Known, item: ItemStack): Cache? {
		for(cache in caches) {
			if(cache.pos == pos && cache.angle == angle && cache.lastItem == item) {
				cache.state = true
				return cache
			}
		}
		return null
	}
	
	override fun store(cached: FlexiPlaceResult, pos: BlockPos, angle: FlexiDirection.Known, lastItem: ItemStack) {
		caches += Cache(cached, pos, angle, lastItem)
	}
}
