package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.other.toTrackEdge
import com.lhwdev.minecraft.railx.utils.math.closeTo
import com.simibubi.create.content.trains.track.BezierConnection
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import thedarkcolour.kotlinforforge.neoforge.forge.DIST


class ConnectionMiddleState(
	val level: LevelAccessor,
	curve: BezierConnection,
) {
	var curve = curve
		private set
	
	val pos: Couple<BlockPos> get() = curve.bePositions
	val from: BlockPos get() = pos.first
	val to: BlockPos get() = pos.second
	
	private val value = mutableSetOf<BlockPos>()
	
	fun isEmpty(): Boolean =
		value.isEmpty()
	
	var canRender: Boolean = true
		private set
	
	val isActive: Boolean
		get() = DIST != Dist.CLIENT || canRender && from !in MiddleTrackClient.LoadedTracks[level]
	
	val allMiddles: Set<BlockPos>
		get() = value
	
	
	internal fun matches(candidate: BezierConnection): Boolean =
		curve.matchesMiddleCurve(candidate)
	
	internal fun addEndpoint(endpoint: BlockPos, candidate: BezierConnection) {
		curve = candidate
		value += endpoint
		canRender = true
	}
	
	internal fun addMiddle(middle: BlockPos, valid: Boolean) {
		value += middle
		canRender = valid
	}
	
	fun removeMiddle(middle: BlockPos) {
		value -= middle
	}
}

internal fun BezierConnection.matchesMiddleCurve(other: BezierConnection): Boolean =
	bePositions == other.bePositions && hasGirder == other.hasGirder && isSameCurve(other)

internal fun BezierConnection.isInTrackGraph(level: LevelAccessor): Boolean {
	val turn = toTrackEdge(level)?.edge?.turn ?: return false
	return isSameCurve(turn)
}

private fun BezierConnection.isSameCurve(other: BezierConnection): Boolean =
	material == other.material &&
		coupleEquals(starts, other.starts) &&
		coupleEquals(axes, other.axes) &&
		coupleEquals(normals, other.normals)

private fun coupleEquals(a: Couple<Vec3>, b: Couple<Vec3>): Boolean =
	a.first closeTo b.first &&
		a.second closeTo b.second
