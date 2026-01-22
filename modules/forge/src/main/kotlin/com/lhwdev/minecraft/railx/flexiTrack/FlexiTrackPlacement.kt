package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.buildTrack.BuildTrak
import com.lhwdev.minecraft.railx.common.minRadius
import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlaceResult.PlaceError
import com.lhwdev.minecraft.railx.registry.AllDataComponents
import com.lhwdev.minecraft.railx.utils.closeTo
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.plus
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
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.*
import com.simibubi.create.AllDataComponents as CreateDataComponents
import com.simibubi.create.AllTags as CreateTags
import com.simibubi.create.AllTags.AllItemTags as CreateItemTags


object FlexiTrackPlacement {
	fun isFlexible(level: LevelReader, stack: ItemStack): Boolean = if(level.isClientSide) {
		FlexiTrackPlacementClient.isFlexibleClient
	} else {
		RailXConfig.Server.flexiTrak.enabled.isTrue && stack.getOrDefault(AllDataComponents.FlexiblePlacement, false)
	}
	
	fun isFlexiPlacementRequired(world: BlockGetter, toState: BlockState, stack: ItemStack): Boolean {
		if(isFromFlexiPlacementRequired(world, stack)) return true
		if(toState.block is FlexiTrackBlock) return true
		return false
	}
	
	private fun isFromFlexiPlacementRequired(world: BlockGetter, stack: ItemStack): Boolean {
		if(!CreateTags.AllBlockTags.TRACKS.matches(stack)) return false
		val connectingFrom = stack.get(CreateDataComponents.TRACK_CONNECTING_FROM) ?: return false
		val from = world.getBlockState(connectingFrom.pos)
		return from.block is FlexiTrackBlock
	}
	
	
	class Parameter(
		val isFlexible: Boolean = true,
		val extend: Boolean = true,
		val hasGirder: Boolean = false,
	) {
		init {
			check(!(isFlexible && !extend))
		}
	}
	
	
	fun PlaceError(message: String): PlaceError = PlaceError(Component.literal(message))
	fun PlaceErrorCreate(key: String): PlaceError = PlaceError(CreateLang.translateDirect("track.$key"))
	
	
	abstract class Cache(
		val cached: FlexiPlaceResult,
		val pos: BlockPos,
		val angle: FlexiDirection.Known,
		val lastItem: ItemStack,
	)
	
	interface CacheStorage {
		fun pull(pos: BlockPos, angle: FlexiDirection.Known, item: ItemStack): Cache?
		
		fun store(cached: FlexiPlaceResult, pos: BlockPos, angle: FlexiDirection.Known, lastItem: ItemStack)
	}
	
	
	/**
	 * @param toState the state block is willing to become. If track block existed at [toPos], this should be equal to
	 * `level.getBlockState(toPos)`. If `toState.block` is [FlexiTrackBlock], `to` is placed flexibly. Otherwise, it is
	 * placed in vanilla Create manner.
	 */
	fun tryConnect(
		level: Level,
		player: Player,
		toPos: BlockPos,
		toState: BlockState,
		item: ItemStack,
		parameter: Parameter,
	): FlexiPlaceResult {
		val info = resolveConnection(level, player, toPos, toState, item, parameter)
		if(info !is FlexiPlacementInfo || !info.valid) return info
		
		if(info.addToPlan) {
			if(info.pavementBlock != null) return info.placeError("pavement not supported for track plan")
			if(!level.isClientSide) BuildTrak.currentPlan.addSegment(info)
			return info
		}
		
		return connect(info, level, player)
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
			info.pavementBlock?.let { info.paveTracks(level, it, simulate = false) }
			info.placeTracks(level, simulate = false)
		}
		return info
	}
	
	
	fun resolveConnection(
		level: Level,
		player: Player,
		toPos: BlockPos,
		toState: BlockState,
		item: ItemStack,
		parameter: Parameter,
		cacheStorage: CacheStorage? = null,
	): FlexiPlaceResult {
		if(toState.block !is ITrackBlock) return PlaceError("internal error: toState is not ITrackBlock")
		
		val lookVec = player.lookAngle.multiply(1.0, 0.0, 1.0)
		val lookAngle = if(lookVec.lengthSqr() similarTo 0.0) {
			player.yRot.toDouble()
		} else {
			Mth.atan2(-lookVec.z, lookVec.x)
		}.let {
			if(parameter.isFlexible) FlexiDirection.Known.roundFrom(radian = it)
			else FlexiDirection.KnownCreate.roundFrom(radian = it)
		}
		
		if(cacheStorage != null) {
			val cache = cacheStorage.pull(pos = toPos, angle = lookAngle.known, item = item)
			if(cache != null) return cache.cached
		}
		
		val fromPoint = item.get(CreateDataComponents.TRACK_CONNECTING_FROM)?.convertToFlexi()
			?: return PlaceError("internal error: no track_connecting_from found")
		if(fromPoint.pos == toPos)
			return PlaceError.SecondPoint()
		
		var toState = toState
		val previousToState = level.getBlockState(toPos)
		val previousToBlock = previousToState.block
		val toPoint = if(previousToBlock is ITrackBlock) {
			toState = previousToState
			FlexiPlacementInfo.TrackPoint(
				pos = toPos,
				tangent = previousToBlock.getNearestTrackAxis(level, toPos, previousToState, player.lookAngle).first,
				normal = previousToBlock.getUpNormal(level, toPos, previousToState),
			)
		} else {
			if(lookAngle is FlexiDirection.KnownCreate) {
				val shape = lookAngle.createShape
				FlexiPlacementInfo.TrackPoint(
					pos = toPos,
					tangent = shape.axes.first(),
					normal = shape.normal,
				)
			} else FlexiPlacementInfo.TrackPoint(
				pos = toPos,
				tangent = lookAngle.tangent,
				normal = lookAngle.normal,
			)
		}
		
		var info = resolveTrackEnd(level, fromPoint, toPoint, toState, item, parameter)
		if(info !is FlexiPlacementInfo || !info.valid) return info
		
		info.hasGirder = parameter.hasGirder
		
		val offhandItem = player.offhandItem.item
		val shouldPave = offhandItem is BlockItem && !CreateItemTags.INVALID_FOR_TRACK_PAVING.matches(offhandItem)
		if(shouldPave) info.pavementBlock = offhandItem.block
		
		info.curve = if(info.curveFrom.pos != info.curveTo.pos) info.createCurve() else null
		
		info = prepareConnect(info, level, player)
		cacheStorage?.store(info, pos = toPos, angle = lookAngle.known, lastItem = item)
		return info
	}
	
	// Note: TrackPoint is not normalized; TrackEnd is normalized
	private fun resolveTrackEnd(
		level: Level,
		from: FlexiPlacementInfo.TrackPoint,
		to: FlexiPlacementInfo.TrackPoint,
		toState: BlockState,
		item: ItemStack,
		parameter: Parameter,
	): FlexiPlaceResult {
		val fromState = level.getBlockState(from.pos)
		val fromBlock = fromState.block as? ITrackBlock ?: return PlaceErrorCreate("original_missing")
		
		val toBlock = toState.block as? ITrackBlock
			?: return PlaceError("item is not TrackBlockItem")
		
		val material = TrackMaterial.fromItem(item.item)
		
		// 1. Tangent should look inside curve; A -> (curve) <- B
		// 2. Angle difference should be smallest, curve should be shortest
		//    -> the position where two vector intersects; tangent go towards intersection point (where t, u > 0)
		// 3. If they do not intersect, (parallel / on same line; same point -> will not happen)
		//    3-1. S curve: make u of intersect(A, B.rotY(90)) > 0, then make A dot B < 0
		//    3-2. on same line: yeah just line
		fun tryResolve(fromVec: Vec3, toVec: Vec3, fromTangent: Vec3, toTangent: Vec3): FlexiPlacementInfo? {
			val fromTangentNormalized = fromTangent.normalize()
			val toTangentNormalized = toTangent.normalize()
			
			val fromSign: Double
			val toSign: Double
			
			var fromExtent = 0
			var toExtent = 0
			
			val intersect = VecHelper.intersect(
				fromVec, toVec,
				fromTangentNormalized, toTangentNormalized,
				Direction.Axis.Y
			)
			if(intersect != null) {
				if(fromTangent.dot(toTangent) > 0) // illegal curve
					return null
				
				fromSign = sign(intersect[0])
				toSign = sign(intersect[1])
				
				if(!parameter.isFlexible) {
					val fromDistance = abs(intersect[0])
					val toDistance = abs(intersect[1])
					var fromEx = 0.0
					var toEx = 0.0
					
					if(fromDistance > toDistance) fromEx = (fromDistance - toDistance) / fromTangent.length()
					else if(toDistance > fromDistance) toEx = (toDistance - fromDistance) / toTangent.length()
					if(!parameter.extend) {
						val turnSize = min(fromDistance, toDistance) - 0.1
						val angle = abs(atan2(fromTangent.z, fromTangent.x) - atan2(toTangent.z, toTangent.x))
						val minTurnSize = FlexiPlacementInfo.multiplyByRadiusFactor(
							material,
							base = if(angle % (PI / 2) < 0.015) 7.0 else 3.25
						)
						
						if(turnSize >= minTurnSize) {
							fromEx += (turnSize - minTurnSize) / fromTangent.length()
							toEx += (turnSize - minTurnSize) / toTangent.length()
						}
					}
					fromExtent = Mth.floor(fromEx)
					toExtent = Mth.floor(toEx)
				}
			} else { // parallel
				val crossIntersect = VecHelper.intersect(
					fromVec, toVec,
					fromTangentNormalized, toTangentNormalized.cross(Vec3(0.0, 1.0, 0.0)),
					Direction.Axis.Y
				)
				
				if(crossIntersect != null) {
					fromSign = sign(crossIntersect[0])
					if(fromSign <= 0.0) return null
					toSign = -sign(fromTangentNormalized.dot(toTangentNormalized))
					
					if(!parameter.extend) {
						val t = abs(crossIntersect[0])
						val u = abs(crossIntersect[1])
						
						if(u similarTo 0.0) { // line
							val distance = Vec3.atCenterOf(from.pos).distanceTo(Vec3.atCenterOf(to.pos))
							when {
								fromState.block !is FlexiTrackBlock -> fromExtent =
									(distance / fromTangent.length()).roundToInt()
								
								toState.block !is FlexiTrackBlock -> toExtent =
									(distance / toTangent.length()).roundToInt()
							}
						} else {
							val targetT = if(u <= 1) 3.0 else u * 2.0
							if(t > targetT) {
								val correction = ((t - targetT) / fromTangent.length()).toInt()
								fromExtent = correction / 2 + (correction % 2)
								toExtent = correction / 2
							}
						}
					}
				} else { // line? normally won't happen
					fromSign = sign(fromTangentNormalized.dot(toVec - fromVec))
					toSign = -fromSign
					
					if(!parameter.extend) {
						val distance = Vec3.atCenterOf(from.pos).distanceTo(Vec3.atCenterOf(to.pos))
						when {
							fromState.block !is FlexiTrackBlock -> fromExtent =
								((distance + 1) / fromTangent.length()).roundToInt()
							
							toState.block !is FlexiTrackBlock -> toExtent =
								((distance + 1) / toTangent.length()).roundToInt()
						}
					}
				}
			}
			
			if(fromSign == 0.0 || toSign == 0.0) return null
			
			val fromTangent = fromTangent.scale(fromSign)
			val toTangent = toTangent.scale(toSign)
			val fromEnd = FlexiPlacementInfo.TrackEnd(
				state = fromState,
				pos = from.pos,
				end = fromBlock.getCurveStart(level, from.pos, fromState, fromTangent),
				tangent = fromTangent,
				normal = from.normal,
			)
			val toEnd = FlexiPlacementInfo.TrackEnd(
				state = toState,
				pos = to.pos,
				end = toBlock.getCurveStart(level, to.pos, toState, toTangent),
				tangent = toTangent,
				normal = to.normal,
			)
			
			val info = FlexiPlacementInfo(
				material = material,
				trackItem = item,
				from = fromEnd,
				to = toEnd,
			)
			
			fun offsetExtent(base: FlexiPlacementInfo.TrackEnd, extent: Int): FlexiPlacementInfo.TrackEnd {
				val offset = base.tangent.scale(extent.toDouble())
				val targetPos = base.pos.offset(BlockPos.containing(offset))
				return FlexiPlacementInfo.TrackEnd(
					state = base.state,
					pos = targetPos,
					end = base.end + offset,
					tangent = base.tangent,
					normal = base.normal,
				)
			}
			
			if(
				fromExtent != 0 && fromState.block is FlexiTrackBlock ||
				toExtent != 0 && toState.block is FlexiTrackBlock
			) {
				val common = min(fromExtent, toExtent)
				fromExtent -= common
				toExtent -= common
			}
			
			if(fromExtent != 0 && fromState.block !is FlexiTrackBlock) {
				info.fromExtent = fromExtent
				info.curveFrom = offsetExtent(fromEnd, fromExtent)
			}
			if(toExtent != 0 && toState.block !is FlexiTrackBlock) {
				info.toExtent = toExtent
				info.curveTo = offsetExtent(toEnd, toExtent)
			}
			
			return info
		}
		
		val fromVec1 = fromBlock.getCurveStart(level, from.pos, fromState, from.tangent)
		val toVec1 = toBlock.getCurveStart(level, to.pos, toState, to.tangent)
		tryResolve(fromVec1, toVec1, from.tangent, to.tangent)?.let { return it }
		
		val toVec2 = toBlock.getCurveStart(level, to.pos, toState, -to.tangent)
		tryResolve(fromVec1, toVec2, from.tangent, -to.tangent)?.let { return it }
		
		val fromVec2 = fromBlock.getCurveStart(level, from.pos, fromState, -from.tangent)
		tryResolve(fromVec2, toVec1, -from.tangent, to.tangent)?.let { return it }
		tryResolve(fromVec2, toVec2, -from.tangent, -to.tangent)?.let { return it }
		
		return PlaceError.TooSharp().noOverlay()
	}
	
	
	private fun FlexiPlacementInfo.validateConnect(level: Level): FlexiPlacementInfo {
		val curve = curve
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
		
		val gradient =
			1000 * abs(curveFrom.end.y - curveTo.end.y) / (curveFrom.end - curveTo.end).horizontalDistance() // in per mille
		if(gradient > RailXConfig.Server.flexiTrak.maxGradient.get()) return placeErrorCreate("too_steep")
		
		val intersect = VecHelper.intersect(
			curveFrom.end, curveTo.end,
			curveFrom.normalizedTangent, curveTo.normalizedTangent,
			Direction.Axis.Y
		)
		if(intersect != null) {
			if(curveFrom.tangent.dot(curveTo.tangent) > 0) // illegal curve
				return placeError(PlaceError.TooSharp().noOverlay())
			
			if(curve != null) {
				if(curve.minRadius() < minimumAllowedRadius)
					return placeError(PlaceError.TooSharp())
			} else {
				if(!(curveFrom.tangent closeTo -curveTo.tangent))
					return placeError(PlaceError.TooSharp().noOverlay())
			}
		} else {
			val fromCross = curveFrom.normalizedTangent.cross(Vec3(0.0, 1.0, 0.0))
			val sCurve = VecHelper.intersect(
				curveFrom.end, curveTo.end,
				fromCross, curveTo.normalizedTangent,
				Direction.Axis.Y
			) ?: return this
			val (u, _) = sCurve
			val t = round(abs(u) * 100) * 0.01
			if(t similarTo 0.0) {
				// straight line
			} else {
				// s curve
				if(curve != null) {
					if(curve.minRadius() < minimumAllowedRadius)
						return placeError(PlaceError.TooSharp())
				} else {
					return placeError(PlaceError.TooSharp().noOverlay())
				}
			}
		}
		return this
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
	
	private fun FlexiPlacementInfo.placeTracks(level: Level, simulate: Boolean) {
		requiredTracks = 0
		
		fun placeTrack(pos: BlockPos, end: FlexiPlacementInfo.TrackEnd, state: BlockState = end.state): BlockState? {
			val stateAtPos = level.getBlockState(pos)
			val blockAtPos = stateAtPos.block
			return when {
				blockAtPos is FlexiTrackBlock -> null
				
				blockAtPos is ITrackBlock -> {
					if(stateAtPos.block !is FlexiTrackBlock) end.toCreateShape()?.let {
						blockAtPos.overlay(level, pos, stateAtPos, state.trySetValue(TrackBlock.SHAPE, it))
							.let { ProperWaterloggedBlock.withWater(level, it, pos) }
					} else null
				}
				
				stateAtPos.canBeReplaced() || stateAtPos.`is`(BlockTags.FLOWERS) ->
					ProperWaterloggedBlock.withWater(level, state, pos)
				
				else -> null
			}
		}
		
		
		fun placeExtent(end: FlexiPlacementInfo.TrackEnd, extent: Int) {
			var state = BlockHelper.copyProperties(end.state, material.defaultBlockState())
			when(state.getValue(TrackBlock.SHAPE)) {
				TrackShape.TE, TrackShape.TW -> state = state.setValue(TrackBlock.SHAPE, TrackShape.XO)
				TrackShape.TN, TrackShape.TS -> state = state.setValue(TrackBlock.SHAPE, TrackShape.ZO)
				else -> {}
			}
			
			for(i in 0 until extent) {
				val offset = end.tangent.scale(i.toDouble())
				val pos = end.pos.offset(BlockPos.containing(offset))
				val state = placeTrack(pos, end, state) ?: continue
				requiredTracks++
				if(!simulate) level.setBlock(pos, state, Block.UPDATE_ALL)
			}
		}
		
		if(fromExtent > 0) placeExtent(from, fromExtent)
		if(toExtent > 0) placeExtent(to, toExtent)
		
		if(!simulate) {
			fun placeCurveEnd(end: FlexiPlacementInfo.TrackEnd) {
				var state = placeTrack(pos = end.pos, end = end) ?: return
				state = state.trySetValue(TrackBlock.HAS_BE, curve != null)
				level.setBlock(end.pos, state, Block.UPDATE_ALL)
				
				(level.getBlockEntity(end.pos) as? FlexiTrackBlockEntity)?.let { be ->
					be.updateState(be.overlayShape(direction = end.toKnownDirection()))
				}
			}
			
			placeCurveEnd(curveFrom)
			placeCurveEnd(curveTo)
		}
		
		curve?.let { curve ->
			val fromBe = level.getBlockEntity(curveFrom.pos)
			val toBe = level.getBlockEntity(curveTo.pos)
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
	}
	
	private fun FlexiPlacementInfo.paveTracks(level: Level, block: Block, simulate: Boolean) {
		requiredPavement = 0
		if(block is EntityBlock || block.defaultBlockState().getCollisionShape(level, from.pos).isEmpty)
			return
		
		val visited = hashSetOf<BlockPos>()
		
		fun paveExtent(end: FlexiPlacementInfo.TrackEnd, extent: Int) {
			if(extent == 0) return
			TrackPaver.paveStraight(level, end.pos.below(), end.tangent, extent, block, simulate, visited)
		}
		
		paveExtent(from, fromExtent)
		paveExtent(to, toExtent)
		
		curve?.let { requiredPavement += TrackPaver.paveCurve(level, it, block, simulate, visited) }
	}
}


fun TrackPlacement.ConnectingFrom.convertToFlexi(): FlexiPlacementInfo.TrackPoint =
	FlexiPlacementInfo.TrackPoint(pos = pos, tangent = axis, normal = normal)
