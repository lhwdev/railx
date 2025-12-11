package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.buildTrack.BuildTrak
import com.lhwdev.minecraft.railx.common.minRadius
import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlaceResult.PlaceError
import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import com.simibubi.create.foundation.utility.BlockHelper
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
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
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllTags as CreateTags
import com.simibubi.create.AllTags.AllItemTags as CreateItemTags


object FlexiTrackPlacement {
	fun PlaceError(message: String): PlaceError = PlaceError(Component.literal(message))
	fun PlaceErrorCreate(key: String): PlaceError = PlaceError(CreateLang.translateDirect("track.$key"))
	
	private class Cached(
		val cached: FlexiPlaceResult,
		val pos: BlockPos,
		val angle: FlexiDirection.Known,
		val lastItem: ItemStack,
	)
	
	private var cached: Cached? = null
	
	fun tryConnect(
		level: Level,
		player: Player,
		toPos: BlockPos,
		toState: BlockState,
		item: ItemStack,
		girder: Boolean,
	): FlexiPlaceResult {
		val info = resolveConnection(level, player, toPos, toState, item, girder)
		val lookDirection = FlexiDirection.Known.roundFrom(vector = player.lookAngle)
		cached = Cached(info, pos = toPos, angle = lookDirection, lastItem = item)
		if(info !is FlexiPlacementInfo || !info.valid) return info
		
		if(info.addToPlan) {
			if(info.pavementBlock != null) return info.placeError("pavement not supported for track plan")
			if(!level.isClientSide) BuildTrak.currentPlan.addSegment(info)
			return info
		}
		
		return connect(info, level, player)
	}
	
	fun resolveConnection(
		level: Level,
		player: Player,
		toPos: BlockPos,
		toState: BlockState,
		item: ItemStack,
		girder: Boolean,
	): FlexiPlaceResult {
		tryMatchCache(player = player, item = item, toPos = toPos)?.let { return it }
		
		val fromPoint = item.get(AllDataComponents.TrackConnectingFrom)
			?: item.get(CreateDataComponents.TRACK_CONNECTING_FROM)?.convertToFlexi()
			?: return PlaceError("internal error: no track_connecting_from found")
		if(fromPoint.pos == toPos)
			return PlaceError.SecondPoint()
		
		val previousToState = level.getBlockState(toPos)
		val track = toState.block as? ITrackBlock
			?: return PlaceError("internal error: block at 'to' is not ITrackBlock")
		
		val toPoint = if(previousToState.block is ITrackBlock || toState.block !is FlexiTrackBlock) {
			FlexiPlacementInfo.TrackPoint(
				pos = toPos,
				tangent = track.getNearestTrackAxis(level, toPos, toState, player.lookAngle).first,
				normal = track.getUpNormal(level, toPos, toState).normalize()
			)
		} else {
			val lookDirection = FlexiDirection.Known.roundFrom(vector = player.lookAngle)
			FlexiPlacementInfo.TrackPoint(
				pos = toPos,
				tangent = lookDirection.tangent,
				normal = lookDirection.normal,
			)
		}
		
		var info = resolveTrackEnd(level, fromPoint, toPoint, toState, item)
		if(info !is FlexiPlacementInfo || !info.valid) return info
		
		info.girder = girder
		
		val offhandItem = player.offhandItem.item
		val shouldPave = offhandItem is BlockItem && !CreateItemTags.INVALID_FOR_TRACK_PAVING.matches(offhandItem)
		if(shouldPave) info.pavementBlock = offhandItem.block
		
		info.curve = info.createCurve()
		
		info = prepareConnect(info, level, player)
		return info
	}
	
	private fun FlexiPlacementInfo.validateConnect(level: Level): FlexiPlacementInfo {
		val distSqr = from.pos.distSqr(to.pos)
		if(distSqr > RailXConfig.Server.flexiTrak.placementLength.asInt.pow2()) {
			if(BuildTrak.enabled && distSqr <= RailXConfig.Server.buildTrak.maxPlacementLength.asInt.pow2()) {
				addToPlan = true
			} else {
				return placeError(PlaceError.TooFar())
			}
		} else {
			if(BuildTrak.enabled && BuildTrak.currentPlan.addPlacedTracks) addToPlan = true
		}
		
		if((level.getBlockEntity(to.pos) as? TrackBlockEntity)?.isTilted == true)
			return placeErrorCreate("turn_start")
		
		val gradient = 1000 * abs(from.end.y - to.end.y) / (from.end - to.end).horizontalDistance() // in per mille
		if(gradient > RailXConfig.Server.flexiTrak.maxGradient.asDouble) return placeErrorCreate("too_steep")
		
		val intersect = VecHelper.intersect(from.end, to.end, from.tangent, to.tangent, Direction.Axis.Y)
		if(intersect != null) {
			if(from.tangent.dot(to.tangent) > 0) // illegal curve
				return placeError(PlaceError.TooSharp().noOverlay())
			
			if(curve.minRadius() < RailXConfig.Server.flexiTrak.minRadius.asInt)
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
				// val maxT = max(v, 1.0) / (RailXConfig.Server.flexiTrak.minRadius.asInt / 4) // IDK about this
				// if(t > maxT) return placeError(PlaceError.TooSharp())
				
				if(curve.minRadius() < RailXConfig.Server.flexiTrak.minRadius.asInt)
					return placeError(PlaceError.TooSharp())
			}
		}
		return this
	}
	
	private fun prepareConnect(info: FlexiPlacementInfo, level: Level, player: Player): FlexiPlaceResult {
		info.validateConnect(level)
		if(info.error != null) return info
		
		info.placeTracks(level, simulate = true)
		
		info.pavementBlock?.let { pavement ->
			info.paveTracks(level, pavement, simulate = true)
		}
		
		if(!info.useTrackItem(level, player, simulate = true) && !player.isCreative)
			return info.placeErrorCreate("not_enough_tracks").noOverlay()
		
		return info
	}
	
	fun connect(info: FlexiPlacementInfo, level: Level, player: Player): FlexiPlaceResult {
		if(!player.isCreative) {
			info.useTrackItem(level, player, simulate = false)
		}
		if(!level.isClientSide) {
			info.pavementBlock?.let { block -> info.paveTracks(level, block, simulate = false) }
			info.placeTracks(level, simulate = false)
		}
		return info
	}
	
	private fun FlexiPlacementInfo.useTrackItem(level: Level, player: Player, simulate: Boolean): Boolean {
		if(level.isClientSide && !simulate) return true
		var tracks = 0
		var pavements = 0
		val inv = player.inventory
		val size = inv.items.size
		for(j in 0..size + 1) {
			var i = j
			val offhand = j == size + 1
			if(j == size) i = inv.selected
			else if(offhand) i = 0
			else if(j == inv.selected) continue
			
			val stackInSlot = (if(offhand) inv.offhand else inv.items)[i]
			val isTrack = CreateTags.AllBlockTags.TRACKS.matches(stackInSlot) && stackInSlot.`is`(trackItem.item)
			if(!isTrack) {
				val item = stackInSlot.item as? BlockItem
				if(item != null && pavementBlock != item.block) continue
			}
			if(if(isTrack) tracks >= requiredTracks else pavements >= requiredPavement) continue
			
			val count = stackInSlot.count
			
			if(!simulate) {
				val remainingItems = count -
					(if(isTrack) requiredTracks - tracks else requiredPavement - pavements).coerceAtMost(count)
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
	
	
	private fun tryMatchCache(player: Player, item: ItemStack, toPos: BlockPos): FlexiPlaceResult? {
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
		from: FlexiPlacementInfo.TrackPoint,
		to: FlexiPlacementInfo.TrackPoint,
		toState: BlockState,
		item: ItemStack,
	): FlexiPlaceResult {
		val fromState = level.getBlockState(from.pos)
		val fromBlock = fromState.block as? ITrackBlock ?: return PlaceErrorCreate("original_missing")
		
		val toBlock = toState.block as ITrackBlock
		
		// 1. Tangent should look inside curve; A -> (curve) <- B
		// 2. Angle difference should be smallest, curve should be shortest
		//    -> the position where two vector intersects; tangent go towards intersection point (where t, u > 0)
		// 3. If they do not intersect, (parallel / on same line; same point -> will not happen)
		//    3-1. S curve: make u of intersect(A, B.rotY(90)) > 0, then make A dot B < 0
		//    3-2. on same line: yeah just line
		
		fun tryResolve(fromVec: Vec3, toVec: Vec3, fromTangent: Vec3, toTangent: Vec3): FlexiPlacementInfo? {
			val fromSign: Double
			val toSign: Double
			
			val intersect = VecHelper.intersect(fromVec, toVec, fromTangent, toTangent, Direction.Axis.Y)
			if(intersect != null) {
				if(fromTangent.dot(toTangent) > 0) // illegal curve
					return null
				
				fromSign = sign(intersect[0])
				toSign = sign(intersect[1])
			} else {
				val crossIntersect = VecHelper.intersect(
					fromVec, toVec,
					fromTangent, toTangent.cross(Vec3(0.0, 1.0, 0.0)),
					Direction.Axis.Y
				)
				if(crossIntersect != null) {
					fromSign = sign(crossIntersect[0])
					if(fromSign <= 0.0) return null
					toSign = -sign(fromTangent.dot(toTangent))
				} else { // generally mostly impossible for Known; why use flexi for straight line
					fromSign = sign(fromTangent.dot(toVec - fromVec))
					toSign = -fromSign
				}
			}
			
			check(fromSign != 0.0 && toSign != 0.0) { "fromSign or toSign == 0, ($fromSign, $toSign)" }
			val fromTangent = fromTangent.scale(fromSign)
			val toTangent = toTangent.scale(toSign)
			val fromEnd = FlexiPlacementInfo.TrackEnd(
				block = fromBlock,
				pos = from.pos,
				end = fromBlock.getCurveStart(level, from.pos, fromState, fromTangent),
				tangent = fromTangent,
				normal = from.normal,
			)
			fromEnd.state = fromState
			val toEnd = FlexiPlacementInfo.TrackEnd(
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
		
		
		val fromVec1 = fromBlock.getCurveStart(level, from.pos, fromState, from.tangent)
		val fromVec2 = fromBlock.getCurveStart(level, from.pos, fromState, -from.tangent)
		val toVec1 = toBlock.getCurveStart(level, to.pos, toState, to.tangent)
		val toVec2 = toBlock.getCurveStart(level, to.pos, toState, -to.tangent)
		
		tryResolve(fromVec1, toVec1, from.tangent, to.tangent)?.let { return it }
		tryResolve(fromVec1, toVec2, from.tangent, -to.tangent)?.let { return it }
		tryResolve(fromVec2, toVec1, -from.tangent, to.tangent)?.let { return it }
		tryResolve(fromVec2, toVec2, -from.tangent, -to.tangent)?.let { return it }
		return PlaceError.TooSharp().noOverlay()
	}
	
	private fun FlexiPlacementInfo.placeTracks(level: Level, simulate: Boolean) {
		val target = material.defaultBlockState()
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
							stateAtPos.trySetValue(TrackBlock.HAS_BE, true), pos
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
	
	private fun FlexiPlacementInfo.paveTracks(level: Level, block: Block, simulate: Boolean) {
		requiredPavement = 0
		if(block is EntityBlock || block.defaultBlockState().getCollisionShape(level, from.pos).isEmpty)
			return
		
		val visited = hashSetOf<BlockPos>()
		
		// TODO: pave extended
		requiredPavement += TrackPaver.paveCurve(level, curve, block, simulate, visited)
	}
}


fun TrackPlacement.ConnectingFrom.convertToFlexi(): FlexiPlacementInfo.TrackPoint =
	FlexiPlacementInfo.TrackPoint(pos = pos, tangent = axis, normal = normal)
