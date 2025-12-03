package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.common.minRadius
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackPlacement.tryConnect
import com.lhwdev.minecraft.railx.registry.AllSpecialTextures
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.plus
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
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.spongepowered.asm.mixin.injection.callback.Cancellable
import kotlin.math.max
import kotlin.math.min
import com.simibubi.create.AllSpecialTextures as CreateSpecialTextures
import com.simibubi.create.AllTags as CreateTags


@OnlyIn(Dist.CLIENT)
object FlexiTrackPlacementClient {
	var animation: LerpedFloat = LerpedFloat.linear()
		.startWithValue(0.0)
	var lastLineCount: Int = 0
	
	var hintPos: BlockPos? = null
	var hintAngle: Int = 0
	var hints: MutableList<Hint>? = null
	
	class Hint(val pos: BlockPos, val valid: Boolean, val curvature: Double)
	
	var lastOverlay: FlexiPlacementInfo? = null
	
	fun clientTick(defaultHandle: Cancellable) {
		lastOverlay = null
		
		val minecraft = Minecraft.getInstance()
		minecraft.level ?: return
		val player = minecraft.player ?: return
		var stack = player.mainHandItem
		val hitResult = minecraft.hitResult as? BlockHitResult ?: return
		
		if(!stack.hasFoil()) return
		
		var hand = InteractionHand.MAIN_HAND
		if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) {
			stack = player.offhandItem
			hand = InteractionHand.OFF_HAND
			if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) return
		}
		
		val blockItem = stack.item as? TrackBlockItem ?: return
		val level = player.level()
		var pos = hitResult.blockPos
		var hitState = level.getBlockState(pos)
		
		if(hitState.block !is ITrackBlock && !hitState.canBeReplaced()) {
			pos = pos.relative(hitResult.direction)
			hitState = blockItem.getPlacementState(UseOnContext(player, hand, hitResult))
			if(hitState == null) return
		}
		
		if(hitState.block !is ITrackBlock) return
		
		// Handled by create TrackPlacement.clientTick
		if(blockItem !is FlexiTrackBlockItem) {
			val tag = stack.tag?.get("ConnectingFrom") as? CompoundTag ?: return
			if(level.getBlockState(NbtUtils.readBlockPos(tag.getCompound("Pos"))).block !is FlexiTrackBlock) return
		}
		defaultHandle.cancel()
		
		val maxTurns = minecraft.options.keySprint.isDown()
		val info = tryConnect(level, player, pos, hitState, stack, false)
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
		if(hitResult.direction == Direction.UP) {
			val lookVec = player.lookAngle
			val lookAngle = FlexiDirection.Known.roundFrom(lookVec).ordinal
			
			if(pos != hintPos || lookAngle != hintAngle) {
				hints = mutableListOf()
				hintAngle = lookAngle
				hintPos = pos
				
				for(xOffset in -2..2) {
					for(zOffset in -2..2) {
						val offset = pos.offset(xOffset, 0, zOffset)
						val adjInfo = tryConnect(level, player, offset, hitState, stack, false)
						val curvature = if(adjInfo !is FlexiPlacementInfo || !adjInfo.valid) {
							0.0
						} else {
							-1 / (0.004 * adjInfo.curve.minRadius() + 1) + 1 // some random function...
						}
						hints += Hint(pos = offset.below(), valid = adjInfo.valid, curvature = curvature)
					}
				}
				this.hints = hints
			}
			
			if(hints != null) {
				var minCurvature = Double.POSITIVE_INFINITY
				var maxCurvature = Double.NEGATIVE_INFINITY
				for(hint in hints) {
					if(!hint.valid) continue
					minCurvature = min(minCurvature, hint.curvature)
					maxCurvature = max(maxCurvature, hint.curvature)
				}
				val curvatures = maxCurvature - minCurvature
				for((index, hint) in hints.withIndex()) {
					if(hint.valid) {
						val w = (hint.curvature - minCurvature) / curvatures
						val threshold = 0.9
						if(w >= threshold) {
							Outliner.getInstance().showCluster("track_$index", listOf(hint.pos))
								.withFaceTexture(AllSpecialTextures.BOLD_THIN_CHECKERED)
								.colored(
									Color(
										Color.mixColors(
											0x5095CD41u.toInt(),
											0x789AC953u.toInt(),
											((w - threshold) / (1 - threshold)).toFloat().coerceIn(0f, 1f)
										)
									)
								)
								.lineWidth(0f)
						} else {
							Outliner.getInstance().showCluster("track_$index", listOf(hint.pos))
								.withFaceTexture(CreateSpecialTextures.THIN_CHECKERED)
								.colored(
									Color(
										Color.mixColors(
											0xc095CD41u.toInt(),
											0xff95CD41u.toInt(),
											w.toFloat().coerceIn(0f, 1f)
										)
									)
								)
								.lineWidth(0f)
						}
					} else {
						Outliner.getInstance().showCluster("track_$index", listOf(hint.pos))
							.withFaceTexture(CreateSpecialTextures.THIN_CHECKERED)
							.colored(0xEA5C2B)
							.lineWidth(0f)
					}
				}
			}
		}
		
		animation.chase((if(info.valid) 1 else 0).toDouble(), 0.25, LerpedFloat.Chaser.EXP)
		animation.tickChaser()
		
		// if(!info.valid) {
		// 	info.fromExtent = 0.0
		// 	info.toExtent = 0.0
		// }
		
		val railColor = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue())
		val up = Vec3(0.0, (4 / 16f).toDouble(), 0.0)
		
		run {
			val from = info.from
			val a1 = from.tangent
			val n1 = from.normal.cross(a1).scale((15 / 16f).toDouble())
			val o1 = a1.scale(0.125)
			val ex1 = a1.scale(0.0)
			line(1, from.tangent - n1 + up, o1, ex1)
			line(2, from.tangent - n1 + up, o1, ex1)
			
			val to = info.to
			val a2 = to.tangent
			val n2 = to.tangent.cross(a2).scale((15 / 16f).toDouble())
			val o2 = a2.scale(0.125)
			val ex2 = a2.scale(0.0/*  * a2.length() */)
			line(3, to.tangent + n2 + up, o2, ex2)
			line(4, to.tangent - n2 + up, o2, ex2)
		}
		
		if(info.error?.noOverlay == true) return
		val bc = info.curve
		
		var previous1: Vec3? = null
		var previous2: Vec3? = null
		val segCount = bc.segmentCount
		
		val s = animation.value * 7 / 8f + 1 / 8f
		val lw = animation.value * 1 / 16f + 1 / 16f
		val end1 = bc.starts.first
		val end2 = bc.starts.second
		val finish1 = end1.add(bc.axes.getFirst().scale(bc.handleLength))
		val finish2 = end2.add(bc.axes.getSecond().scale(bc.handleLength))
		val key = "curve"
		
		for(i in 0..segCount) {
			val t = i / segCount.toFloat()
			val result = VecHelper.bezier(end1, end2, finish1, finish2, t)
			val derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t)
				.normalize()
			val normal = bc.getNormal(t.toDouble())
				.cross(derivative)
				.scale(15.0 / 16.0)
			val rail1 = result.add(normal).add(up)
			val rail2 = result.subtract(normal).add(up)
			
			if(previous1 != null) {
				val middle1 = rail1.add(previous1).scale(0.5)
				val middle2 = rail2.add(previous2!!).scale(0.5)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2), VecHelper.lerp(s, middle1, previous1),
						VecHelper.lerp(s, middle1, rail1)
					)
					.colored(railColor)
					.disableLineNormals()
					.lineWidth(lw)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2 + 1), VecHelper.lerp(s, middle2, previous2),
						VecHelper.lerp(s, middle2, rail2)
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
	
	private fun line(id: Int, v1: Vec3, o1: Vec3, ex: Vec3) {
		val color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue())
		Outliner.getInstance().showLine(Pair.of("start", id), v1.subtract(o1), v1.add(ex))
			.lineWidth(1 / 8f)
			.disableLineNormals()
			.colored(color)
	}
}
