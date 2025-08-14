package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.registry.AllTags
import com.lhwdev.minecraft.railx.registry.matches
import com.lhwdev.minecraft.railx.utils.pow2
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
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
import kotlin.math.sign
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
		val material: FlexiTrackMaterial,
		val trackItem: ItemStack,
		val from: TrackEnd,
		val to: TrackEnd,
		val fromExtent: Double = 0.0,
		val toExtent: Double = 0.0,
	) : PlaceResult {
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
	
	private fun FlexiPlacementInfo.tryConnect(level: Level, player: Player): PlaceResult {
		if(from.pos.distSqr(to.pos) > CreateConfigs.server().trains.maxTrackPlacementLength.get().pow2())
			return placeErrorCreate("too_far").noOverlay()
		if((level.getBlockEntity(to.pos) as? TrackBlockEntity)?.isTilted == true)
			return placeErrorCreate("turn_start")
		
		// TODO: constraint about curvature / gradient
		
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
						if(i == inv.selected)
							stackInSlot.remove(AllDataComponents.TrackConnectingFrom)
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
		// 1. tangent should look inside curve; A -> (curve) <- B
		// 2. angle difference should be smallest, curve should be shortest
		//    -> the position where two vector intersects; tangent go towards intersection point (where t, u > 0)
		// 3. if does not intersect, (parallel / on same line; same point -> will not happen)
		//    3-1. S curve: make u of intersect(A, B.rotY(90)) > 0, then make A dot B < 0
		//    3-2. on same line: yeah just line
		val fromVec = Vec3.atBottomCenterOf(from.pos)
		val toVec = Vec3.atBottomCenterOf(to.pos)
		val intersect = VecHelper.intersect(fromVec, toVec, from.tangent, to.tangent, Direction.Axis.Y)
		val fromTangent: Vec3
		val toTangent: Vec3
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
		
		val fromState = level.getBlockState(from.pos)
		val fromBlock = fromState.block as? ITrackBlock ?: return PlaceErrorCreate("original_missing")
		val toState = toState
		val toBlock = toState.block as ITrackBlock
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
			material = TrackMaterial.fromItem(item.item) as? FlexiTrackMaterial ?: FlexiTrackMaterial.Andesite,
			trackItem = item,
			from = fromEnd,
			to = toEnd,
		)
	}
	
	private fun FlexiPlacementInfo.placeTracks(level: Level, simulate: Boolean) {
		val target = material.flexiBlock.defaultBlockState()
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
				
				stateAtPos.block is TrackBlock -> {
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
					val be = level.getBlockEntity(pos) as FlexiTrackBlockEntity
					be.updateState(be.state.copy(shape = FlexiShape.Single(direction)))
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
	fun clientTick() {
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
		if(!AllTags.Features.FlexiTrack.block.matches(stack)) {
			stack = player.offhandItem
			hand = InteractionHand.OFF_HAND
			if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) return
		}
		
		if(!stack.hasFoil()) return
		
		val blockItem = stack.item as? FlexiTrackBlockItem ?: return
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
		
		extraTipWarmup = restoreWarmup
		val maxTurns = minecraft.options.keySprint.isDown()
		val info = tryConnect(level, player, pos, hitState, stack, false)
		if(info !is FlexiPlacementInfo) return
		
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
		
		if(info.valid) player.displayClientMessage(
			CreateLang.translateDirect("track.valid_connection")
				.withStyle(ChatFormatting.GREEN), true
		)
		else info.error?.let { error ->
			player.displayClientMessage(
				error.message
					.withStyle(/* if(error == "track.second_point") ChatFormatting.WHITE else */ ChatFormatting.RED),
				true
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
				val middle2 = rail2.add(previous2)
					.scale(0.5)
				Outliner.getInstance()
					.showLine(
						Pair.of<String?, Int?>(key, i * 2), VecHelper.lerp(s, middle1, previous1),
						VecHelper.lerp(s, middle1, rail1)
					)
					.colored(railcolor)
					.disableLineNormals()
					.lineWidth(lw)
				Outliner.getInstance()
					.showLine(
						Pair.of<String?, Int?>(key, i * 2 + 1), VecHelper.lerp(s, middle2, previous2),
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
			Outliner.getInstance().remove(Pair.of<String?, Int?>(key, i * 2))
			Outliner.getInstance().remove(Pair.of<String?, Int?>(key, i * 2 + 1))
		}
		
		lastLineCount = segCount
	}
	
	@OnlyIn(Dist.CLIENT)
	private fun line(id: Int, v1: Vec3, o1: Vec3, ex: Vec3) {
		val color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue())
		Outliner.getInstance().showLine(Pair.of<String?, Int?>("start", id), v1.subtract(o1), v1.add(ex))
			.lineWidth(1 / 8f)
			.disableLineNormals()
			.colored(color)
	}
	
	/* fun tryConnect(
		level: Level,
		player: Player,
		pos2: BlockPos,
		state2: BlockState,
		stack: ItemStack,
		girder: Boolean,
		maximiseTurn: Boolean,
	): PlacementInfo {
		val lookVec = player.lookAngle.multiply(1.0, 0.0, 1.0)
		val lookAngleDouble = if(Mth.equal(lookVec.length(), 0.0)) {
			player.yRot.toDouble()
		} else {
			Mth.atan2(-lookVec.z, lookVec.x)
		}
		val lookAngle = FlexiDirection.Known.roundFrom(radian = lookAngleDouble)
		val maxLength = CreateConfigs.server().trains.maxTrackPlacementLength.get()
		
		val currentCache = cachedOld
		if(
			level.isClientSide &&
			currentCache != null &&
			currentCache.lastItem == stack &&
			currentCache.pos == pos2 &&
			currentCache.maxed == maximiseTurn &&
			currentCache.angle == lookAngle
		) {
			return currentCache.cached as PlacementInfo
		}
		
		@Suppress("KotlinConstantConditions")
		val info = PlacementInfo(TrackMaterial.fromItem(stack.item)) as PlacementInfoAccessor
		cachedOld = CachedOld(info, pos = pos2, maxed = maximiseTurn, angle = lookAngle, lastItem = stack)
		
		val track = state2.block as ITrackBlock
		val nearestTrackAxis = track.getNearestTrackAxis(level, pos2, state2, lookVec)
		var axis2: Vec3 = nearestTrackAxis.getFirst()
			.scale((if(nearestTrackAxis.getSecond() == Direction.AxisDirection.POSITIVE) -1 else 1).toDouble())
		val normal2 = track.getUpNormal(level, pos2, state2).normalize()
		var normedAxis2 = axis2.normalize()
		var end2 = track.getCurveStart(level, pos2, state2, axis2)
		val tbe = level.getBlockEntity(pos2)
		
		val connectingFrom = stack.get(CreateDataComponents.TRACK_CONNECTING_FROM)!!
		
		val pos1 = connectingFrom.pos
		var axis1 = connectingFrom.axis
		var normedAxis1 = axis1.normalize()
		var end1 = connectingFrom.end
		val normal1 = connectingFrom.normal
		val state1 = level.getBlockState(pos1)
		
		if(level.isClientSide) {
			info.end1 = end1
			info.end2 = end2
			info.normal1 = normal1
			info.normal2 = normal2
			info.axis1 = axis1
			info.axis2 = axis2
		}
		
		if(pos1 == pos2) return info.callWithMessage("second_point")
		if(pos1.distSqr(pos2) > maxLength * maxLength) return info.callWithMessage("too_far").tooJumbly()
		if(level.getBlockEntity(pos1) !is TrackBlockEntity) return info.callWithMessage("original_missing")
		
		if(tbe is TrackBlockEntity && tbe.isTilted) return info.callWithMessage("turn_start")
		
		if(axis1.dot(end2.subtract(end1)) < 0) {
			axis1 = axis1.scale(-1.0)
			normedAxis1 = normedAxis1.scale(-1.0)
			end1 = track.getCurveStart(level, pos1, state1, axis1)
			if(level.isClientSide) {
				info.end1 = end1
				info.axis1 = axis1
			}
		}
		
		var intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y)
		val parallel = intersect == null
		var skipCurve = false
		
		if((parallel && normedAxis1.dot(normedAxis2) > 0) || (!parallel && (intersect[0] < 0 || intersect[1] < 0))) {
			axis2 = axis2.scale(-1.0)
			normedAxis2 = normedAxis2.scale(-1.0)
			end2 = track.getCurveStart(level, pos2, state2, axis2)
			if(level.isClientSide) {
				info.end2 = end2
				info.axis2 = axis2
			}
		}
		
		val cross2 = normedAxis2.cross(Vec3(0.0, 1.0, 0.0))
		
		val a1 = Mth.atan2(normedAxis2.z, normedAxis2.x)
		val a2 = Mth.atan2(normedAxis1.z, normedAxis1.x)
		val angle = a1 - a2
		val ascend = end2.subtract(end1).y
		val absAscend = abs(ascend)
		val slope = normal1 != normal2
		
		if(level.isClientSide) {
			val offset1 = axis1.scale(info.end1Extent.toDouble())
			val offset2 = axis2.scale(info.end2Extent.toDouble())
			val targetPos1 = pos1.offset(BlockPos.containing(offset1))
			val targetPos2 = pos2.offset(BlockPos.containing(offset2))
			info.curve = BezierConnection(
				Couple.create(targetPos1, targetPos2),
				Couple.create(end1.add(offset1), end2.add(offset2)),
				Couple.create(normedAxis1, normedAxis2),
				Couple.create(normal1, normal2),
				true,
				girder,
				TrackMaterial.fromItem(stack.item)
			)
		}
		
		
		// S curve or Straight
		var dist = 0.0
		
		if(parallel) {
			val sTest = VecHelper.intersect(end1, end2, normedAxis1, cross2, Direction.Axis.Y)
			if(sTest != null) {
				val t = abs(sTest[0])
				val u = abs(sTest[1])
				
				skipCurve = Mth.equal(u, 0.0)
				
				if(!skipCurve && sTest[0] < 0) return info.callWithMessage("perpendicular").tooJumbly()
				
				if(skipCurve) {
					dist = VecHelper.getCenterOf(pos1)
						.distanceTo(VecHelper.getCenterOf(pos2))
					info.end1Extent = ((dist + 1) / axis1.length()).roundToInt()
				} else {
					if(!Mth.equal(ascend, 0.0) || normedAxis1.y != 0.0) return info.callWithMessage("ascending_s_curve")
					
					val targetT = if(u <= 1) 3.0 else u * 2
					
					if(t < targetT) return info.callWithMessage("too_sharp")
					
					// This is for standardising s curve sizes
					if(t > targetT) {
						val correction = ((t - targetT) / axis1.length()).toInt()
						info.end1Extent = if(maximiseTurn) 0 else correction / 2 + (correction % 2)
						info.end2Extent = if(maximiseTurn) 0 else correction / 2
					}
				}
			}
		}
		
		
		// Slope
		if(slope) {
			if(!skipCurve) return info.callWithMessage("slope_turn")
			if(Mth.equal(normal1.dot(normal2), 0.0)) return info.callWithMessage("opposing_slopes")
			if((axis1.y < 0 || axis2.y > 0) && ascend > 0) return info.callWithMessage("leave_slope_ascending")
			if((axis1.y > 0 || axis2.y < 0) && ascend < 0) return info.callWithMessage("leave_slope_descending")
			
			skipCurve = false
			info.end1Extent = 0
			info.end2Extent = 0
			
			val plane = if(Mth.equal(axis1.x, 0.0)) Direction.Axis.X else Direction.Axis.Z
			intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, plane)!!
			val dist1 = abs(intersect[0] / axis1.length())
			val dist2 = abs(intersect[1] / axis2.length())
			
			if(dist1 > dist2) info.end1Extent = (dist1 - dist2).roundToInt()
			if(dist2 > dist1) info.end2Extent = (dist2 - dist1).roundToInt()
			
			var turnSize = min(dist1, dist2)
			if(intersect[0] < 0 || intersect[1] < 0) return info.callWithMessage("too_sharp").tooJumbly()
			if(turnSize < 2) return info.callWithMessage("too_sharp")
			
			// This is for standardising curve sizes
			if(turnSize > 2 && !maximiseTurn) {
				info.end1Extent += (turnSize - 2).toInt()
				info.end2Extent += (turnSize - 2).toInt()
				turnSize = 2.0
			}
		}
		
		
		// Straight ascend
		if(skipCurve && !Mth.equal(ascend, 0.0)) {
			val hDistance = info.end1Extent
			if(axis1.y == 0.0 || !Mth.equal(absAscend + 1, dist / axis1.length())) {
				if(axis1.y != 0.0 && axis1.y == -axis2.y) return info.callWithMessage("ascending_s_curve")
				
				info.end1Extent = 0
				val minHDistance = max(if(absAscend < 4) absAscend * 4 else absAscend * 3, 6.0) / axis1.length()
				if(hDistance < minHDistance) return info.callWithMessage("too_steep")
				if(hDistance > minHDistance) {
					val correction = (hDistance - minHDistance).toInt()
					info.end1Extent = if(maximiseTurn) 0 else correction / 2 + (correction % 2)
					info.end2Extent = if(maximiseTurn) 0 else correction / 2
				}
				
				skipCurve = false
			}
		}
		
		
		// Turn
		if(!parallel) {
			val absAngle = abs(AngleHelper.deg(angle))
			if(absAngle < 60 || absAngle > 300) return info.callWithMessage("turn_90").tooJumbly()
			
			intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y)!!
			val dist1 = abs(intersect[0])
			val dist2 = abs(intersect[1])
			var ex1 = 0f
			var ex2 = 0f
			
			if(dist1 > dist2) ex1 = ((dist1 - dist2) / axis1.length()).toFloat()
			if(dist2 > dist1) ex2 = ((dist2 - dist1) / axis2.length()).toFloat()
			
			var turnSize = min(dist1, dist2) - .1
			val ninety = (absAngle + .25f) % 90 < 1
			
			if(intersect[0] < 0 || intersect[1] < 0) return info.callWithMessage("too_sharp").tooJumbly()
			
			val minTurnSize = if(ninety) 7.0 else 3.25
			val turnSizeToFitAscend =
				minTurnSize + (if(ninety) max(0.0, absAscend - 3) * 2f else max(0.0, absAscend - 1.5f) * 1.5f)
			
			if(turnSize < minTurnSize) return info.callWithMessage("too_sharp")
			if(turnSize < turnSizeToFitAscend) return info.callWithMessage("too_steep")
			
			// This is for standardising curve sizes
			if(!maximiseTurn) {
				ex1 += ((turnSize - turnSizeToFitAscend) / axis1.length()).toFloat()
				ex2 += ((turnSize - turnSizeToFitAscend) / axis2.length()).toFloat()
			}
			info.end1Extent = Mth.floor(ex1)
			info.end2Extent = Mth.floor(ex2)
			turnSize = turnSizeToFitAscend
		}
		
		val offset1 = axis1.scale(info.end1Extent.toDouble())
		val offset2 = axis2.scale(info.end2Extent.toDouble())
		val targetPos1 = pos1.offset(BlockPos.containing(offset1))
		val targetPos2 = pos2.offset(BlockPos.containing(offset2))
		
		info.curve = if(skipCurve) null else {
			BezierConnection(
				Couple.create(targetPos1, targetPos2),
				Couple.create(end1.add(offset1), end2.add(offset2)),
				Couple.create(normedAxis1, normedAxis2),
				Couple.create(normal1, normal2),
				true,
				girder,
				TrackMaterial.fromItem(stack.item),
			)
		}
		
		info.valid = true
		
		info.pos1 = pos1
		info.pos2 = pos2
		info.axis1 = axis1
		info.axis2 = axis2
		
		placeTracks(level, info, state1, state2, targetPos1, targetPos2, true)
		
		val offhandItem = player.offhandItem.copy()
		val shouldPave = offhandItem.item is BlockItem && !CreateItemTags.INVALID_FOR_TRACK_PAVING.matches(offhandItem)
		if(shouldPave) {
			val paveItem = offhandItem.item as BlockItem
			paveTracks(level, info, paveItem, true)
			info.hasRequiredPavement = true
		}
		
		info.hasRequiredTracks = true
		
		if(!player.isCreative) {
			for(simulate in Iterate.trueAndFalse) {
				if(level.isClientSide && !simulate) break
				
				val tracks = info.requiredTracks
				val pavement = info.requiredPavement
				var foundTracks = 0
				var foundPavement = 0
				
				val inv = player.inventory
				val size: Int = inv.items.size
				for(j in 0..size + 1) {
					var i = j
					val offhand = j == size + 1
					if(j == size) i = inv.selected
					else if(offhand) i = 0
					else if(j == inv.selected) continue
					
					val stackInSlot: ItemStack = (if(offhand) inv.offhand else inv.items)[i]
					val isTrack = CreateTags.AllBlockTags.TRACKS.matches(stackInSlot) && stackInSlot.`is`(stack.item)
					if(!isTrack && (!shouldPave || offhandItem.item !== stackInSlot.item)) continue
					if(if(isTrack) foundTracks >= tracks else foundPavement >= pavement) continue
					
					val count = stackInSlot.count
					
					if(!simulate) {
						val remainingItems =
							count - min(if(isTrack) tracks - foundTracks else pavement - foundPavement, count)
						if(i == inv.selected) stackInSlot.remove(CreateDataComponents.TRACK_CONNECTING_FROM)
						val newItem = stackInSlot.copyWithCount(remainingItems)
						if(offhand) player.setItemInHand(InteractionHand.OFF_HAND, newItem)
						else inv.setItem(i, newItem)
					}
					
					if(isTrack) foundTracks += count
					else foundPavement += count
				}
				
				if(simulate && foundTracks < tracks) {
					info.valid = false
					info.callTooJumbly()
					info.hasRequiredTracks = false
					return info.callWithMessage("not_enough_tracks")
				}
				
				if(simulate && foundPavement < pavement) {
					info.valid = false
					info.callTooJumbly()
					info.hasRequiredPavement = false
					return info.callWithMessage("not_enough_pavement")
				}
			}
		}
		
		if(level.isClientSide) return info as PlacementInfo
		if(shouldPave) {
			val paveItem = offhandItem.item as BlockItem
			paveTracks(level, info, paveItem, false)
		}
		return placeTracks(level, info, state1, state2, targetPos1, targetPos2, false)
	}
	
	private fun paveTracks(level: Level, info: PlacementInfoAccessor, blockItem: BlockItem, simulate: Boolean) {
		val block = blockItem.block
		info.requiredPavement = 0
		if(block == null || block is EntityBlock || block.defaultBlockState()
				.getCollisionShape(level, info.pos1)
				.isEmpty
		) return
		
		val visited = HashSet<BlockPos>()
		
		for(first in Iterate.trueAndFalse) {
			val extent = (if(first) info.end1Extent else info.end2Extent) + (if(info.curve != null) 1 else 0)
			val axis: Vec3 = if(first) info.axis1 else info.axis2
			val pavePos: BlockPos = if(first) info.pos1 else info.pos2
			info.requiredPavement +=
				TrackPaver.paveStraight(level, pavePos.below(), axis, extent, block, simulate, visited)
		}
		
		if(info.curve != null) {
			info.requiredPavement += TrackPaver.paveCurve(level, info.curve, block, simulate, visited)
		}
	}
	
	private fun placeTracks(
		level: Level, info: PlacementInfoAccessor, state1: BlockState, state2: BlockState,
		targetPos1: BlockPos, targetPos2: BlockPos, simulate: Boolean,
	): PlacementInfo {
		info.requiredTracks = 0
		val trackMaterial = info.trackMaterial
		
		for(first in Iterate.trueAndFalse) {
			val extent = if(first) info.end1Extent else info.end2Extent
			val axis: Vec3 = if(first) info.axis1 else info.axis2
			val pos: BlockPos = if(first) info.pos1 else info.pos2
			var state = if(first) state1 else state2
			if(state is FlexiBlockState) {
				state = BlockHelper.copyProperties(state, trackMaterial.block.defaultBlockState())
			}
			
			// in case state.block is TrackBlock
			if(state.hasProperty(TrackBlock.HAS_BE) && !simulate)
				state = state.setValue(TrackBlock.HAS_BE, false)
			
			when(state.getOptionalValue(TrackBlock.SHAPE).getOrNull()) {
				TrackShape.TE, TrackShape.TW -> state =
					state.setValue(TrackBlock.SHAPE, TrackShape.XO)
				
				TrackShape.TN, TrackShape.TS -> state =
					state.setValue(TrackBlock.SHAPE, TrackShape.ZO)
				
				else -> {}
			}
			
			for(i in 0..<(if(info.curve != null) extent + 1 else extent)) {
				val offset = axis.scale(i.toDouble())
				val offsetPos = pos.offset(BlockPos.containing(offset))
				val stateAtPos = level.getBlockState(offsetPos)
				
				var canPlace = stateAtPos.canBeReplaced() || stateAtPos.`is`(BlockTags.FLOWERS)
				if(canPlace) info.requiredTracks++
				if(simulate) continue
				
				val trackAtPos = stateAtPos.block
				if(trackAtPos is ITrackBlock) {
					// discarded
					state = trackAtPos.overlay(level, offsetPos, stateAtPos, state)
					canPlace = true
				}
				
				if(canPlace) level.setBlock(offsetPos, ProperWaterloggedBlock.withWater(level, state, offsetPos), 3)
			}
		}
		
		if(info.curve == null) return info as PlacementInfo
		
		if(!simulate) {
			val onto = if(trackMaterial is FlexiTrackMaterial) {
				trackMaterial.flexiBlock.defaultBlockState()
			} else {
				trackMaterial.block.defaultBlockState()
			}
			
			fun placeTrackEnd(pos: BlockPos, state: BlockState, direction: FlexiDirection) {
				level.setBlock(
					pos,
					ProperWaterloggedBlock.withWater(level, BlockHelper.copyProperties(state, onto), pos),
					3
				)
				val be = level.getBlockEntity(pos)
				if(be is FlexiTrackBlockEntity) {
					be.updateState(be.overlayShape(direction))
				}
			}
			
			placeTrackEnd(targetPos1, state1, FlexiDirection.Known.roundFrom(vector = info.axis1))
			placeTrackEnd(targetPos2, state2, FlexiDirection.Known.roundFrom(vector = info.axis2))
		}
		
		val te1 = level.getBlockEntity(targetPos1)
		val te2 = level.getBlockEntity(targetPos2)
		val requiredTracksForTurn = (info.curve!!.getSegmentCount() + 1) / 2
		
		if(te1 !is TrackBlockEntity || te2 !is TrackBlockEntity) {
			info.requiredTracks += requiredTracksForTurn
			return info as PlacementInfo
		}
		
		if(!te1.getConnections().containsKey(te2.blockPos))
			info.requiredTracks += requiredTracksForTurn
		
		if(simulate) return info as PlacementInfo
		
		te1.addConnection(info.curve)
		te2.addConnection(info.curve.secondary())
		te1.tilt.tryApplySmoothing()
		te2.tilt.tryApplySmoothing()
		return info as PlacementInfo
	} */
}
