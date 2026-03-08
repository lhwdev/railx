package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlockEntity
import com.lhwdev.minecraft.utils.vectors.add
import com.lhwdev.minecraft.utils.vectors.set
import com.lhwdev.minecraft.utils.vectors.times
import com.lhwdev.minecraft.utils.vectors.unaryMinus
import com.railwayteam.railways.content.custom_tracks.casing.CasingCollisionUtils
import com.railwayteam.railways.registry.CRTrackMaterials
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackShape
import it.unimi.dsi.fastutil.longs.LongArraySet
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import kotlin.math.roundToInt


object FlexiTrackCasingUtils {
	@Suppress("UNCHECKED_CAST")
	private val offsets = CasingCollisionUtils::class.java
		.getDeclaredField("OFFSETS")
		.also { it.isAccessible = true }
		.get(null) as Map<TrackMaterial.TrackType, Map<TrackShape, Set<BlockPos>>>
	
	@Suppress("RedundantIf")
	fun isValidForCasing(be: FlexiTrackBlockEntity): Boolean {
		if(be.block.material.trackType !in offsets) return false
		
		val shape = be.shape
		if(shape.axesCount <= 0) return false
		// if(!FlexiDirection.Flat.isFlat(shape.normal)) return false
		
		return true
	}
	
	// fun manageTracks(be: FlexiTrackBlockEntity, remove: Boolean) {
	// 	if(!isValidForCasing(be)) return
	//
	// 	val level = be.level ?: return
	// 	val positions = casingPositions(be) // TODO: cache
	// 	for(offset in positions) {
	// 		val pos = be.blockPos.offset(offset)
	// 		val state = level.getBlockState(pos)
	// 		val present = CRBlocks.CASING_COLLISION.has(state)
	//
	// 		if(remove) {
	// 			if(present) level.removeBlock(pos, false)
	// 			continue
	// 		}
	//
	// 		val fluidState = state.fluidState
	// 		if(!fluidState.isEmpty && !fluidState.isSourceOfType(Fluids.WATER))
	// 			continue
	//
	//
	// 		if(!present && state.canBeReplaced()) {
	// 			level.setBlock(
	// 				pos,
	// 				ProperWaterloggedBlock.withWater(level, CRBlocks.CASING_COLLISION.defaultState, pos),
	// 				3
	// 			)
	// 		}
	// 		CasingCollisionBlock.keepAlive(level, pos)
	// 	}
	// }
	
	
	fun casingPositions(be: FlexiTrackBlockEntity): List<Vec3> {
		val factor = when(be.block.material.trackType) {
			CRTrackMaterials.CRTrackType.WIDE_GAUGE -> 1.8
			CRTrackMaterials.CRTrackType.NARROW_GAUGE -> 1.3 - 7.0 / 16.0
			else -> 1.3
		}
		
		val current = Vector3d()
		val positions = ArrayList<Vec3>()
		val takenPos = LongArraySet()
		
		for(axis in be.shape.axes) {
			val crossNormal = axis.tangent.cross(axis.normal)
			val pos1 = crossNormal * factor
			val pos2 = -pos1
			
			val steps = 4
			val step = pos1.vectorTo(pos2) * (1.0 / steps)
			
			current.set(pos1)
			
			for(i in 0..steps) {
				val x = current.x.roundToInt()
				val z = current.z.roundToInt()
				if(takenPos.add((x.toLong() shl 32) or z.toLong()))
					positions += Vec3(x.toDouble(), current.y, z.toDouble())
				
				current.add(step)
			}
		}
		
		return positions
	}
}
