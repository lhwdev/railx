package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import com.simibubi.create.foundation.utility.BlockHelper
import com.simibubi.create.foundation.utility.CreateLang
import io.netty.buffer.ByteBuf
import net.createmod.catnip.animation.LerpedFloat
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Pair
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.createmod.catnip.outliner.Outliner
import net.createmod.catnip.theme.Color
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.codec.StreamCodec
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.spongepowered.asm.mixin.injection.callback.Cancellable
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sign
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllSpecialTextures as CreateSpecialTextures
import com.simibubi.create.AllTags as CreateTags
import com.simibubi.create.AllTags.AllItemTags as CreateItemTags
import com.simibubi.create.infrastructure.config.AllConfigs as CreateConfigs


object FlexiTrackPlacement {
	data class TrackPoint(val pos: BlockPos, val tangent: Vec3, val normal: Vec3) {
		companion object {
			val CODEC: Codec<TrackPoint> = RecordCodecBuilder.create {
				it.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(TrackPoint::pos),
					Vec3.CODEC.fieldOf("tangent").forGetter(TrackPoint::tangent),
					Vec3.CODEC.fieldOf("normal").forGetter(TrackPoint::normal),
				).apply(it, ::TrackPoint)
			}
			val STREAM_CODEC: StreamCodec<ByteBuf, TrackPoint> = StreamCodec.composite(
				BlockPos.STREAM_CODEC, TrackPoint::pos,
				CatnipStreamCodecs.VEC3, TrackPoint::tangent,
				CatnipStreamCodecs.VEC3, TrackPoint::normal,
				::TrackPoint
			)
		}
	}
	
	data class TrackEnd(val block: ITrackBlock, val pos: BlockPos, val end: Vec3, val tangent: Vec3, val normal: Vec3) {
		var state: BlockState = (block as Block).defaultBlockState()
		
		fun toKnownDirection(): FlexiDirection =
			FlexiDirection.Known.roundFrom(tangent).applyNormal(normal)
	}
	
	
	sealed interface PlaceResult
	
	class FlexiPlacementInfo(
		val material: TrackMaterial,
		val trackItem: ItemStack,
		val from: TrackEnd,
		val to: TrackEnd,
		val fromExtent: Double = 0.0,
		val toExtent: Double = 0.0,
	) : PlaceResult {
		val flexiMaterial: FlexiTrackMaterial?
			get() = material as? FlexiTrackMaterial
		
		var girder: Boolean = false
		
		val fromOffset = from.end + from.tangent.scale(fromExtent)
		val toOffset = to.end + to.tangent.scale(toExtent)
		
		lateinit var curve: BezierConnection
		var requiredTracks: Int = 0
		var requiredPavement: Int = 0
		var hasRequiredTracks: Boolean = true
		var hasRequiredPavement: Boolean = true
		
		var error: PlaceError? = null
		val valid: Boolean get() = error == null
		
		fun createCurve(): BezierConnection = BezierConnection(
			Couple.create(from.pos, to.pos),
			Couple.create(from.end, to.end),
			Couple.create(from.tangent, to.tangent),
			Couple.create(from.normal, to.normal),
			true,
			girder,
			material,
		)
		
		fun placeError(text: String) = placeError(Component.literal(text))
		fun placeErrorCreate(key: String) = placeError(CreateLang.translateDirect("track.$key"))
		fun placeError(text: MutableComponent) = this.also { error = PlaceError(text) }
		
		fun noOverlay(): FlexiPlacementInfo {
			error = error?.noOverlay()
			return this
		}
	}
	
	class PlaceError(val message: MutableComponent, val noOverlay: Boolean = false) : PlaceResult {
		fun noOverlay(): PlaceError = PlaceError(message, noOverlay = true)
	}
	
	fun PlaceError(message: String): PlaceError = PlaceError(Component.literal(message))
	fun PlaceErrorCreate(key: String): PlaceError = PlaceError(CreateLang.translateDirect("track.$key"))
	
	private class Cached(
		val cached: FlexiPlacementInfo,
		val pos: BlockPos,
		val angle: FlexiDirection.Known,
		val lastItem: ItemStack,
	)
	
	private class CachedOld(
		val cached: PlacementInfoAccessor,
		val pos: BlockPos,
		val maxed: Boolean,
		val angle: FlexiDirection.Known,
		val lastItem: ItemStack,
	)
	
	private var cached: Cached? = null
	private var cachedOld: CachedOld? = null
	
	fun tryConnect(
		level: Level,
		player: Player,
		toPos: BlockPos,
		toState: BlockState,
		item: ItemStack,
		girder: Boolean,
	): PlaceResult {
		tryMatchCache(player = player, item = item, toPos = toPos)?.let { return it }
		
		val fromPoint = item.get(AllDataComponents.TrackConnectingFrom)
			?: item.get(CreateDataComponents.TRACK_CONNECTING_FROM)?.convertToFlexi()
			?: return PlaceError("internal error: no track_connecting_from found")
		if(fromPoint.pos == toPos)
			return PlaceError("second_point")
		
		val previousToState = level.getBlockState(toPos)
		val track = toState.block as? ITrackBlock
			?: return PlaceError("internal error: block at 'to' is not ITrackBlock")
		
		val toPoint = if(previousToState.block is ITrackBlock || toState.block is TrackBlock) {
			TrackPoint(
				pos = toPos,
				tangent = track.getNearestTrackAxis(level, toPos, toState, player.lookAngle).first,
				normal = track.getUpNormal(level, toPos, toState).normalize()
			)
		} else {
			val direction = FlexiDirection.Known.roundFrom(vector = player.lookAngle)
			TrackPoint(
				pos = toPos,
				tangent = direction.tangent,
				normal = direction.normal,
			)
		}
		
		val info = resolveTrackEnd(level, fromPoint, toPoint, toState, item)
		if(info !is FlexiPlacementInfo) return info
		
		info.girder = girder
		info.curve = info.createCurve()
		return info.tryConnect(level, player)
	}
	
	private fun FlexiPlacementInfo.validateConnect(level: Level): FlexiPlacementInfo {
		if(from.pos.distSqr(to.pos) > CreateConfigs.server().trains.maxTrackPlacementLength.get().pow2())
			return placeErrorCreate("too_far").noOverlay()
		if((level.getBlockEntity(to.pos) as? TrackBlockEntity)?.isTilted == true)
			return placeErrorCreate("turn_start")
		
		val intersect = VecHelper.intersect(from.end, to.end, from.tangent, to.tangent, Direction.Axis.Y)
		if(intersect != null) {
			if(from.tangent.dot(to.tangent) > 0) // illegal curve
				return placeErrorCreate("too_sharp")
			
			if(curve.radius < RailXConfig.Server.flexiTrak.minRadius.get())
				return placeErrorCreate("too_sharp")
		} else {
			val fromCross = from.tangent.cross(Vec3(0.0, 1.0, 0.0))
			val sCurve = VecHelper.intersect(from.end, to.end, fromCross, to.tangent, Direction.Axis.Y)
				?: return this
			val (u, v) = sCurve
			val t = round(abs(u) * 100) * 0.01
			if(t similarTo 0.0) {
				// straight line
			} else {
				// s curve
				val maxT = max(v, 1.0) / (RailXConfig.Server.flexiTrak.minRadius.get() / 2) // IDK about this
				if(t > maxT) return placeErrorCreate("too_sharp")
			}
		}
		
		return this
	}
	
	private fun FlexiPlacementInfo.tryConnect(level: Level, player: Player): PlaceResult {
		validateConnect(level)
		error?.let { return it }
		
		placeTracks(level, simulate = true)
		
		val offhandItem = player.offhandItem.copy()
		val shouldPave = offhandItem.item is BlockItem && !CreateItemTags.INVALID_FOR_TRACK_PAVING.matches(offhandItem)
		if(shouldPave) {
			val paveItem = offhandItem.item as BlockItem
			paveTracks(level, paveItem, simulate = true)
		}
		
		if(!player.isCreative) {
			fun useTrackItem(simulate: Boolean): Boolean {
				if(level.isClientSide && !simulate) return true
				var tracks = 0
				var pavements = 0
				val inv = player.inventory
				val size = inv.items.size
				for(j in 0..size + 1) {
					var i = j
					val offhand = j == size + 1
					if(j == size)
						i = inv.selected
					else if(offhand)
						i = 0
					else if(j == inv.selected)
						continue
					
					val stackInSlot = (if(offhand) inv.offhand else inv.items)[i]
					val isTrack =
						CreateTags.AllBlockTags.TRACKS.matches(stackInSlot) && stackInSlot.`is`(trackItem.item)
					if(!isTrack && (!shouldPave || offhandItem.item != stackInSlot.item))
						continue
					if(if(isTrack) tracks >= requiredTracks else pavements >= requiredPavement) continue
					
					val count = stackInSlot.count
					
					if(!simulate) {
						val remainingItems = count -
							(if(isTrack) requiredTracks - tracks else requiredPavement - pavements)
								.coerceAtMost(count)
						if(i == inv.selected) {
							stackInSlot.remove(AllDataComponents.TrackConnectingFrom)
							stackInSlot.remove(CreateDataComponents.TRACK_CONNECTING_FROM)
						}
						val newItem = stackInSlot.copyWithCount(remainingItems)
						if(offhand)
							player.setItemInHand(InteractionHand.OFF_HAND, newItem)
						else
							inv.setItem(i, newItem)
					}
					
					if(isTrack)
						tracks += count
					else
						pavements += count
				}
				hasRequiredTracks = tracks >= requiredTracks
				hasRequiredPavement = pavements >= requiredPavement
				return hasRequiredTracks && hasRequiredPavement
			}
			if(!useTrackItem(simulate = true)) {
				return placeErrorCreate("not_enough_tracks").noOverlay()
			}
			useTrackItem(simulate = false)
		}
		if(!level.isClientSide) {
			if(shouldPave) paveTracks(level, offhandItem.item as BlockItem, simulate = false)
			placeTracks(level, simulate = false)
		}
		return this
	}
	
	private fun tryMatchCache(player: Player, item: ItemStack, toPos: BlockPos): FlexiPlacementInfo? {
		val lookVec = player.lookAngle.multiply(1.0, 0.0, 1.0)
		val lookAngle = if(Mth.equal(lookVec.lengthSqr(), 0.0)) {
			player.yRot.toDouble()
		} else {
			Mth.atan2(-lookVec.z, lookVec.x)
		}.let { FlexiDirection.Known.roundFrom(radian = it) }
		
		if(player.level().isClientSide) cached?.let { cache ->
			if(cache.angle == lookAngle && cache.lastItem == item && cache.pos == toPos) return cache.cached
		}
		return null
	}
	
	private fun resolveTrackEnd(
		level: Level,
		from: TrackPoint,
		to: TrackPoint,
		toState: BlockState,
		item: ItemStack,
	): PlaceResult {
		val fromState = level.getBlockState(from.pos)
		val fromBlock = fromState.block as? ITrackBlock ?: return PlaceErrorCreate("original_missing")
		
		val toBlock = toState.block as ITrackBlock
		
		// 1. Tangent should look inside curve; A -> (curve) <- B
		// 2. Angle difference should be smallest, curve should be shortest
		//    -> the position where two vector intersects; tangent go towards intersection point (where t, u > 0)
		// 3. If they do not intersect, (parallel / on same line; same point -> will not happen)
		//    3-1. S curve: make u of intersect(A, B.rotY(90)) > 0, then make A dot B < 0
		//    3-2. on same line: yeah just line
		
		val fromTangent: Vec3
		val toTangent: Vec3
		
		// find sign of tangent
		run {
			val fromVec = fromBlock.getCurveStart(level, from.pos, fromState, from.tangent)
			val toVec = toBlock.getCurveStart(level, to.pos, toState, to.tangent)
			val intersect = VecHelper.intersect(fromVec, toVec, from.tangent, to.tangent, Direction.Axis.Y)
			if(intersect != null) {
				fromTangent = from.tangent.scale(sign(intersect[0]))
				toTangent = to.tangent.scale(sign(intersect[1]))
			} else {
				val crossIntersect = VecHelper.intersect(
					fromVec, toVec,
					from.tangent, to.tangent.cross(Vec3(0.0, 1.0, 0.0)),
					Direction.Axis.Y
				)
				if(crossIntersect != null) {
					fromTangent = from.tangent.scale(sign(crossIntersect[0]))
					toTangent = to.tangent.scale(-sign(fromTangent.dot(to.tangent)))
				} else {
					fromTangent = (toVec - fromVec).normalize()
					toTangent = -fromTangent
				}
			}
		}
		
		val fromEnd = TrackEnd(
			block = fromBlock,
			pos = from.pos,
			end = fromBlock.getCurveStart(level, from.pos, fromState, fromTangent),
			tangent = fromTangent,
			normal = from.normal,
		)
		fromEnd.state = fromState
		val toEnd = TrackEnd(
			block = toBlock,
			pos = to.pos,
			end = toBlock.getCurveStart(level, to.pos, toState, toTangent),
			tangent = toTangent,
			normal = to.normal,
		)
		toEnd.state = toState
		
		return FlexiPlacementInfo(
			material = TrackMaterial.fromItem(item.item),
			trackItem = item,
			from = fromEnd,
			to = toEnd,
		)
	}
	
	private fun FlexiPlacementInfo.placeTracks(level: Level, simulate: Boolean) {
		val target = (flexiMaterial?.flexiBlock ?: material.block).defaultBlockState()
		// val targetFrom = BlockPos.containing(fromOffset)
		// val targetTo = BlockPos.containing(toOffset)
		
		requiredTracks = 0
		
		fun placeTrack(pos: BlockPos, state: BlockState, direction: FlexiDirection): Boolean {
			val stateAtPos = level.getBlockState(pos)
			return when {
				stateAtPos.block is FlexiTrackBlock -> {
					val be = level.getBlockEntity(pos) as FlexiTrackBlockEntity
					val newState = be.overlayShape(direction)
					if(be.state == newState) {
						false
					} else {
						be.updateState(newState)
						true
					}
				}
				
				stateAtPos.block is ITrackBlock -> {
					level.setBlock(
						pos, ProperWaterloggedBlock.withWater(
							level,
							stateAtPos.setValue(TrackBlock.HAS_BE, true), pos
						), 3
					)
					true
				}
				
				stateAtPos.canBeReplaced() || stateAtPos.`is`(BlockTags.FLOWERS) -> {
					val newState = BlockHelper.copyProperties(state, target)
					level.setBlock(pos, ProperWaterloggedBlock.withWater(level, newState, pos), 3)
					val be = level.getBlockEntity(pos)
					if(be is FlexiTrackBlockEntity) {
						be.updateState(be.state.copy(shape = FlexiShape.Single(direction)))
					}
					true
				}
				
				else -> false
			}
		}
		
		check(fromExtent == 0.0) { TODO() }
		check(toExtent == 0.0) { TODO() }
		
		// TODO: place extents
		
		if(!simulate) {
			placeTrack(from.pos, from.state, from.toKnownDirection())
			placeTrack(to.pos, to.state, to.toKnownDirection())
		}
		val fromBe = level.getBlockEntity(from.pos)
		val toBe = level.getBlockEntity(to.pos)
		val turnTracks = (curve.segmentCount + 1) / 2
		
		if(fromBe !is TrackBlockEntity || toBe !is TrackBlockEntity) {
			requiredTracks += turnTracks
			return
		}
		if(toBe.blockPos !in fromBe.connections) {
			requiredTracks += turnTracks
		}
		
		if(!simulate) {
			fromBe.addConnection(curve)
			toBe.addConnection(curve.secondary())
			fromBe.tilt.tryApplySmoothing()
			toBe.tilt.tryApplySmoothing()
		}
	}
	
	private fun FlexiPlacementInfo.paveTracks(level: Level, item: BlockItem, simulate: Boolean) {
		requiredPavement = 0
		val block = item.block ?: return
		if(block is EntityBlock || block.defaultBlockState().getCollisionShape(level, from.pos).isEmpty)
			return
		
		val visited = hashSetOf<BlockPos>()
		
		// TODO: pave extended
		requiredPavement += TrackPaver.paveCurve(level, curve, block, simulate, visited)
	}
	
	
	var extraTipWarmup: Int = 0
	
	var animation: LerpedFloat = LerpedFloat.linear()
		.startWithValue(0.0)
	var lastLineCount: Int = 0
	
	var hintPos: BlockPos? = null
	var hintAngle: Int = 0
	var hints: Couple<MutableList<BlockPos>>? = null
	
	@OnlyIn(Dist.CLIENT)
	fun clientTick(defaultHandle: Cancellable) {
		val minecraft = Minecraft.getInstance()
		minecraft.level ?: return
		val player = minecraft.player ?: return
		var stack = player.mainHandItem
		val hitResult = minecraft.hitResult
		val restoreWarmup = extraTipWarmup
		extraTipWarmup = 0
		
		if(hitResult == null) return
		if(hitResult.type != HitResult.Type.BLOCK) return
		
		var hand = InteractionHand.MAIN_HAND
		if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) {
			stack = player.offhandItem
			hand = InteractionHand.OFF_HAND
			if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) return
		}
		
		if(!stack.hasFoil()) return
		
		val blockItem = stack.item as? TrackBlockItem ?: return
		val level = player.level()
		val bhr = hitResult as BlockHitResult
		var pos = bhr.blockPos
		var hitState = level.getBlockState(pos)
		if(hitState.block !is ITrackBlock && !hitState.canBeReplaced()) {
			pos = pos.relative(bhr.direction)
			hitState = blockItem.getPlacementState(UseOnContext(player, hand, bhr))
			if(hitState == null) return
		}
		
		if(hitState.block !is ITrackBlock) return
		
		// Handled by create TrackPlacement.clientTick
		if(blockItem !is FlexiTrackBlockItem) {
			val from = stack.get(CreateDataComponents.TRACK_CONNECTING_FROM) ?: return
			if(level.getBlockState(from.pos).block !is FlexiTrackBlock) return
		}
		defaultHandle.cancel()
		
		extraTipWarmup = restoreWarmup
		val maxTurns = minecraft.options.keySprint.isDown()
		val info = tryConnect(level, player, pos, hitState, stack, false)
		if(info !is FlexiPlacementInfo) {
			if(info is PlaceError) player.displayClientMessage(info.message.withStyle(ChatFormatting.RED), true)
			return
		}
		
		if(extraTipWarmup < 20) extraTipWarmup++
		if(!info.valid || /* !hoveringMaxed && */ info.fromExtent == 0.0 || info.toExtent == 0.0)
			extraTipWarmup = 0
		
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
		if(error != null) {
			player.displayClientMessage(
				error.message
					.withStyle(/* if(error == "track.second_point") ChatFormatting.WHITE else */ ChatFormatting.RED),
				true
			)
		} else {
			player.displayClientMessage(
				CreateLang.translateDirect("track.valid_connection")
					.withStyle(ChatFormatting.GREEN), true
			)
		}
		
		
		var hints: Couple<MutableList<BlockPos>>? = hints
		if(bhr.direction == Direction.UP) {
			val lookVec = player.lookAngle
			val lookAngle = (22.5 + AngleHelper.deg(Mth.atan2(lookVec.z, lookVec.x)) % 360).toInt() / 8
			
			if(pos != hintPos || lookAngle != hintAngle) {
				hints = Couple.create { mutableListOf<BlockPos>() }
				hintAngle = lookAngle
				hintPos = pos
				
				for(xOffset in -2..2) {
					for(zOffset in -2..2) {
						val offset = pos.offset(xOffset, 0, zOffset)
						val adjInfo = tryConnect(level, player, offset, hitState, stack, false)
						hints[adjInfo is FlexiPlacementInfo].add(offset.below())
					}
				}
				this.hints = hints
			}
			
			if(hints != null && !hints.either { it.isEmpty() }) {
				Outliner.getInstance().showCluster("track_valid", hints.first)
					.withFaceTexture(CreateSpecialTextures.THIN_CHECKERED)
					.colored(0x95CD41)
					.lineWidth(0f)
				Outliner.getInstance().showCluster("track_invalid", hints.second)
					.withFaceTexture(CreateSpecialTextures.THIN_CHECKERED)
					.colored(0xEA5C2B)
					.lineWidth(0f)
			}
		}
		
		animation.chase((if(info.valid) 1 else 0).toDouble(), 0.25, LerpedFloat.Chaser.EXP)
		animation.tickChaser()
		
		// if(!info.valid) {
		// 	info.fromExtent = 0.0
		// 	info.toExtent = 0.0
		// }
		
		val color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue())
		val up = Vec3(0.0, (4 / 16f).toDouble(), 0.0)
		
		run {
			val from = info.from
			val a1 = from.tangent
			val n1 = from.normal.cross(a1).scale((15 / 16f).toDouble())
			val o1 = a1.scale(0.125)
			val ex1 = a1.scale(info.fromExtent)
			line(1, from.tangent - n1 + up, o1, ex1)
			line(2, from.tangent - n1 + up, o1, ex1)
			
			val to = info.to
			val a2 = to.tangent
			val n2 = to.tangent.cross(a2).scale((15 / 16f).toDouble())
			val o2 = a2.scale(0.125)
			val ex2 = a2.scale(info.toExtent/*  * a2.length() */)
			line(3, to.tangent + n2 + up, o2, ex2)
			line(4, to.tangent - n2 + up, o2, ex2)
		}
		
		if(info.error?.noOverlay == true) return
		val bc = info.curve
		
		var previous1: Vec3? = null
		var previous2: Vec3? = null
		val railcolor = color
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
				.scale((15 / 16f).toDouble())
			val rail1 = result.add(normal)
				.add(up)
			val rail2 = result.subtract(normal)
				.add(up)
			
			if(previous1 != null) {
				val middle1 = rail1.add(previous1)
					.scale(0.5)
				val middle2 = rail2.add(previous2!!)
					.scale(0.5)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2), VecHelper.lerp(s, middle1, previous1),
						VecHelper.lerp(s, middle1, rail1)
					)
					.colored(railcolor)
					.disableLineNormals()
					.lineWidth(lw)
				Outliner.getInstance()
					.showLine(
						Pair.of(key, i * 2 + 1), VecHelper.lerp(s, middle2, previous2),
						VecHelper.lerp(s, middle2, rail2)
					)
					.colored(railcolor)
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
	
	@OnlyIn(Dist.CLIENT)
	private fun line(id: Int, v1: Vec3, o1: Vec3, ex: Vec3) {
		val color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue())
		Outliner.getInstance().showLine(Pair.of("start", id), v1.subtract(o1), v1.add(ex))
			.lineWidth(1 / 8f)
			.disableLineNormals()
			.colored(color)
	}
}


fun TrackPlacement.ConnectingFrom.convertToFlexi(): FlexiTrackPlacement.TrackPoint =
	FlexiTrackPlacement.TrackPoint(pos = pos, tangent = axis, normal = normal)
