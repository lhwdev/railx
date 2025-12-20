package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.buildTrack.BuildTrak
import com.lhwdev.minecraft.railx.common.minRadius
import com.lhwdev.minecraft.railx.flexiTrack.FlexiPlaceResult.PlaceError
import com.lhwdev.minecraft.railx.utils.pow2
import com.lhwdev.minecraft.railx.utils.similarTo
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign
import com.simibubi.create.AllTags as CreateTags
import com.simibubi.create.AllTags.AllItemTags as CreateItemTags


object FlexiTrackPlacement {
	fun isFlexible(level: LevelReader, stack: ItemStack): Boolean = if(level.isClientSide) {
		FlexiTrackPlacementClient.isFlexibleClient
	} else {
		RailXConfig.Server.flexiTrak.enabled.get() && stack.tag?.getBoolean("railx:FlexiblePlacement") ?: false
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
		girder: Boolean,
	): FlexiPlaceResult {
		val info = resolveConnection(level, player, toPos, toState, item, girder)
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
		girder: Boolean = false,
		cacheStorage: CacheStorage? = null,
	): FlexiPlaceResult {
		if(toState.block !is ITrackBlock) return PlaceError("internal error: toState is not ITrackBlock")
		
		val lookVec = player.lookAngle.multiply(1.0, 0.0, 1.0)
		val lookAngle = if(lookVec.lengthSqr() similarTo 0.0) {
			player.yRot.toDouble()
		} else {
			Mth.atan2(-lookVec.z, lookVec.x)
		}.let {
			if(toState.block is FlexiTrackBlock) FlexiDirection.Known.roundFrom(radian = it)
			else FlexiDirection.KnownCreate.roundFrom(radian = it)
		}
		
		if(cacheStorage != null) {
			val cache = cacheStorage.pull(pos = toPos, angle = lookAngle, item = item)
			if(cache != null) return cache.cached
		}
		
		val tag = item.tag ?: return PlaceError("no tag in item")
		val fromPoint = (tag.get("ConnectingFrom") as? CompoundTag)?.convertPlacementToFlexi()
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
			FlexiPlacementInfo.TrackPoint(
				pos = toPos,
				tangent = lookAngle.tangent,
				normal = lookAngle.normal,
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
		cacheStorage?.store(info, pos = toPos, angle = lookAngle, lastItem = item)
		return info
	}
	
	// Note: TrackPoint is not normalized; TrackEnd is normalized
	private fun resolveTrackEnd(
		level: Level,
		from: FlexiPlacementInfo.TrackPoint,
		to: FlexiPlacementInfo.TrackPoint,
		toState: BlockState,
		item: ItemStack,
	): FlexiPlaceResult {
		val fromState = level.getBlockState(from.pos)
		val fromBlock = fromState.block as? ITrackBlock ?: return PlaceErrorCreate("original_missing")
		
		val toBlock = toState.block as? ITrackBlock
			?: return PlaceError("item is not TrackBlockItem")
		
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
			} else {
				val crossIntersect = VecHelper.intersect(
					fromVec, toVec,
					fromTangentNormalized, toTangentNormalized.cross(Vec3(0.0, 1.0, 0.0)),
					Direction.Axis.Y
				)
				if(crossIntersect != null) {
					fromSign = sign(crossIntersect[0])
					if(fromSign <= 0.0) return null
					toSign = -sign(fromTangentNormalized.dot(toTangentNormalized))
				} else {
					fromSign = sign(fromTangentNormalized.dot(toVec - fromVec))
					toSign = -fromSign
				}
			}
			
			if(fromSign == 0.0 || toSign == 0.0) return null
			val fromTangent = fromTangentNormalized.scale(fromSign)
			val toTangent = toTangentNormalized.scale(toSign)
			val fromEnd = FlexiPlacementInfo.TrackEnd(
				state = fromState,
				pos = from.pos,
				end = fromBlock.getCurveStart(level, from.pos, fromState, fromTangent),
				tangent = fromTangent,
				normal = from.normal.normalize(),
			)
			val toEnd = FlexiPlacementInfo.TrackEnd(
				state = toState,
				pos = to.pos,
				end = toBlock.getCurveStart(level, to.pos, toState, toTangent),
				tangent = toTangent,
				normal = to.normal.normalize(),
			)
			
			return FlexiPlacementInfo(
				material = TrackMaterial.fromItem(item.item),
				trackItem = item,
				from = fromEnd,
				to = toEnd,
			)
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
		val distSqr = from.pos.distSqr(to.pos)
		if(distSqr > RailXConfig.Server.flexiTrak.placementLength.get().pow2()) {
			if(BuildTrak.enabled && distSqr <= RailXConfig.Server.buildTrak.maxPlacementLength.get().pow2()) {
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
		if(gradient > RailXConfig.Server.flexiTrak.maxGradient.get()) return placeErrorCreate("too_steep")
		
		val intersect = VecHelper.intersect(from.end, to.end, from.tangent, to.tangent, Direction.Axis.Y)
		if(intersect != null) {
			if(from.tangent.dot(to.tangent) > 0) // illegal curve
				return placeError(PlaceError.TooSharp().noOverlay())
			
			if(curve.minRadius() < minimumAllowedRadius)
				return placeError(PlaceError.TooSharp())
		} else {
			val fromCross = from.tangent.cross(Vec3(0.0, 1.0, 0.0))
			val sCurve = VecHelper.intersect(from.end, to.end, fromCross, to.tangent, Direction.Axis.Y)
				?: return this
			val (u, _) = sCurve
			val t = round(abs(u) * 100) * 0.01
			if(t similarTo 0.0) {
				// straight line
			} else {
				// s curve
				if(curve.minRadius() < minimumAllowedRadius)
					return placeError(PlaceError.TooSharp())
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
					stackInSlot.tag = null
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
		
		fun placeTrack(pos: BlockPos, targetState: BlockState, direction: FlexiDirection) {
			val stateAtPos = level.getBlockState(pos)
			when {
				stateAtPos.block is FlexiTrackBlock -> {
					val be = level.getBlockEntity(pos) as? FlexiTrackBlockEntity ?: return
					val newState = be.overlayShape(direction)
					be.updateState(newState)
				}
				
				stateAtPos.block is ITrackBlock -> {
					var state = stateAtPos.trySetValue(TrackBlock.HAS_BE, true)
					state = ProperWaterloggedBlock.withWater(level, state, pos)
					level.setBlock(pos, state, Block.UPDATE_ALL)
				}
				
				stateAtPos.canBeReplaced() || stateAtPos.`is`(BlockTags.FLOWERS) -> {
					val state = ProperWaterloggedBlock.withWater(level, targetState, pos)
					level.setBlock(pos, state, Block.UPDATE_ALL)
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
		requiredPavement += TrackPaver.paveCurve(level, curve, block, simulate, visited)
	}
}


private fun CompoundTag.convertPlacementToFlexi(): FlexiPlacementInfo.TrackPoint =
	FlexiPlacementInfo.TrackPoint(
		pos = NbtUtils.readBlockPos(getCompound("Pos")),
		tangent = VecHelper.readNBT(getList("Axis", Tag.TAG_DOUBLE.toInt())),
		normal = VecHelper.readNBT(getList("Normal", Tag.TAG_DOUBLE.toInt())),
	)
