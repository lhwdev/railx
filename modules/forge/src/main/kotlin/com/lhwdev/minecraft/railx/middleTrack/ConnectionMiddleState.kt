package com.lhwdev.minecraft.railx.middleTrack

import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.neoforged.api.distmarker.Dist
import thedarkcolour.kotlinforforge.neoforge.forge.DIST


class ConnectionMiddleState(
	val level: LevelAccessor,
	var curve: BezierConnection,
) {
	val pos: Couple<BlockPos> get() = curve.bePositions
	val from: BlockPos get() = pos.first
	val to: BlockPos get() = pos.second
	
	private val value = mutableSetOf<BlockPos>()
	
	fun isEmpty(): Boolean =
		value.isEmpty()
	
	val isActive: Boolean
		get() = if(DIST == Dist.CLIENT) {
			from !in MiddleTrackClient.LoadedTracks
		} else {
			true
		}
	
	
	fun addMiddle(middle: BlockPos) {
		value += middle
	}
	
	fun removeMiddle(middle: BlockPos) {
		value -= middle
	}
}
