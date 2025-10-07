package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.mixin.flexiTrack.PlacementInfoAccessor
import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import com.simibubi.create.foundation.utility.BlockHelper
import com.simibubi.create.foundation.utility.CreateLang
import io.netty.buffer.ByteBuf
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs
import net.createmod.catnip.data.Couple
import net.createmod.catnip.math.VecHelper
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
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sign
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllTags as CreateTags
import com.simibubi.create.AllTags.AllItemTags as CreateItemTags


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
		
		fun toPoint(): TrackPoint = TrackPoint(pos, tangent, normal)
		
		fun toKnownDirection(): FlexiDirection {
			if(normal.x similarTo 0.0 && normal.z similarTo 0.0) return FlexiDirection.Known.roundFrom(tangent)
			val normalized = FlexiDirection.Two(tangent, normal).toNormalized()
			return FlexiDirection.NormalizedImpl(FlexiDirection.Known.roundFrom(normalized.tangent), normalized.normal)
		}
	}
	
	
	sealed interface PlaceResult {
		val valid: Boolean
	}
	
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
		override val valid: Boolean get() = error == null
		
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
		fun placeError(text: MutableComponent) = placeError(PlaceError(text))
		fun placeError(error: PlaceError) = this.also { this.error = error }
		
		fun noOverlay(): FlexiPlacementInfo {
			error = error?.noOverlay()
			return this
		}
	}
	
	open class PlaceError(val message: MutableComponent, val noOverlay: Boolean = false) : PlaceResult {
		override val valid: Boolean
			get() = false
		
		fun noOverlay(): PlaceError = PlaceError(message, noOverlay = true)
		
		class SecondPoint : PlaceError(message = CreateLang.translateDirect("track.second_point"), noOverlay = true)
		class TooSharp : PlaceError(message = CreateLang.translateDirect("track.too_sharp"))
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
			return PlaceError.SecondPoint()
		
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
		if(from.pos.distSqr(to.pos) > RailXConfig.Server.flexiTrak.maxPlacementLength.asInt.pow2())
			return placeErrorCreate("too_far").noOverlay()
		if((level.getBlockEntity(to.pos) as? TrackBlockEntity)?.isTilted == true)
			return placeErrorCreate("turn_start")
		
		val gradient = 1000 * abs(from.end.y - to.end.y) / (from.end - to.end).horizontalDistance() // in per mille
		if(gradient > RailXConfig.Server.flexiTrak.maxGradient.asDouble) return placeErrorCreate("too_steep")
		
		val intersect = VecHelper.intersect(from.end, to.end, from.tangent, to.tangent, Direction.Axis.Y)
		if(intersect != null) {
			if(from.tangent.dot(to.tangent) > 0) // illegal curve
				return placeError(PlaceError.TooSharp().noOverlay())
			
			if(curve.radius < RailXConfig.Server.flexiTrak.minRadius.asInt)
				return placeError(PlaceError.TooSharp())
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
				val maxT = max(v, 1.0) / (RailXConfig.Server.flexiTrak.minRadius.asInt / 4) // IDK about this
				if(t > maxT) return placeError(PlaceError.TooSharp())
			}
		}
		
		return this
	}
	
	private fun FlexiPlacementInfo.tryConnect(level: Level, player: Player): PlaceResult {
		validateConnect(level)
		if(error != null) return this
		
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
			// NOTE: fromTangent/toTangent may be FlexiDirection.KnownVec3
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
				} else { // generally mostly impossible for Known; why use flexi for straight line
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
		
		fun placeTrack(pos: BlockPos, state: BlockState, direction: FlexiDirection) {
			val stateAtPos = level.getBlockState(pos)
			when {
				stateAtPos.block is FlexiTrackBlock -> {
					val be = level.getBlockEntity(pos) as FlexiTrackBlockEntity
					val newState = be.overlayShape(direction)
					be.updateState(newState)
				}
				
				stateAtPos.block is ITrackBlock -> {
					level.setBlock(
						pos, ProperWaterloggedBlock.withWater(
							level,
							stateAtPos.setValue(TrackBlock.HAS_BE, true), pos
						), 3
					)
				}
				
				stateAtPos.canBeReplaced() || stateAtPos.`is`(BlockTags.FLOWERS) -> {
					val newState = BlockHelper.copyProperties(state, target)
					level.setBlock(pos, ProperWaterloggedBlock.withWater(level, newState, pos), 3)
					val be = level.getBlockEntity(pos)
					if(be is FlexiTrackBlockEntity) {
						be.updateState(be.state.copy(baseShape = FlexiShape.Single(direction)))
					}
				}
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
	
	
}


fun TrackPlacement.ConnectingFrom.convertToFlexi(): FlexiTrackPlacement.TrackPoint =
	FlexiTrackPlacement.TrackPoint(pos = pos, tangent = axis, normal = normal)
