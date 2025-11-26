package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.other.toTrackEdge
import com.lhwdev.minecraft.railx.utils.closeTo
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import thedarkcolour.kotlinforforge.forge.DIST


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
	
	private var isValid: Boolean = true
	
	val isActive: Boolean
		get() = if(DIST == Dist.CLIENT) {
			isValid && from !in MiddleTrackClient.LoadedTracks
		} else {
			true
		}
	
	val allMiddles: Set<BlockPos>
		get() = value
	
	
	fun addMiddle(middle: BlockPos) {
		value += middle
		if(!isValid() && !level.isClientSide) {
			level.destroyBlock(middle, false)
		}
	}
	
	fun removeMiddle(middle: BlockPos) {
		value -= middle
	}
	
	fun isValid(): Boolean {
		// maybe curve was removed; check the curve is still there, by TrackGraph
		val edge = curve.toTrackEdge(level)?.edge
		val valid = edge != null && curve.isSameCurve(edge.turn)
		this.isValid = valid
		return valid
	}
}

private fun BezierConnection.isSameCurve(other: BezierConnection): Boolean =
	material == other.material &&
		coupleEquals(starts, other.starts) &&
		coupleEquals(axes, other.axes) &&
		coupleEquals(normals, other.normals)

private fun coupleEquals(a: Couple<Vec3>, b: Couple<Vec3>): Boolean =
	a.first closeTo b.first &&
		a.second closeTo b.second
