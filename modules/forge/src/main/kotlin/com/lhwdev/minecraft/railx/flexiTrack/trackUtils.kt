package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


fun MutableCollection<DiscoveredLocation>.addIfConnected(
	fromEnd: TrackNodeLocation?,
	getLocation: (isFirst: Boolean) -> DiscoveredLocation,
) {
	val first = getLocation(true)
	val second = getLocation(false)
	if(first.dimension != second.dimension) {
		first.forceNode()
		second.forceNode()
	}
	
	if(fromEnd != null) {
		val skipFirst = first == fromEnd
		val skipSecond = second == fromEnd
		if(!skipFirst && !skipSecond) return
		if(!skipFirst) this += first
		if(!skipSecond) this += second
	} else {
		this += first
		this += second
	}
}

fun MutableCollection<DiscoveredLocation>.addIfConnected(
	fromEnd: TrackNodeLocation?,
	getOffset: (fraction: Double, isFirst: Boolean) -> Vec3,
	factory: (dimension: ResourceKey<Level>, vec: Vec3) -> DiscoveredLocation = ::DiscoveredLocation,
	getLocation: DiscoveredLocationFactory.() -> DiscoveredLocation,
) {
	addIfConnected(fromEnd) { isFirst ->
		DiscoveredLocationFactory(factory, getOffset, isFirst).getLocation()
	}
}


class DiscoveredLocationFactory(
	private val factory: (ResourceKey<Level>, Vec3) -> DiscoveredLocation = ::DiscoveredLocation,
	private val getOffset: (fraction: Double, isFirst: Boolean) -> Vec3,
	val isFirst: Boolean,
) {
	
	fun offset(fraction: Double): Vec3 =
		getOffset(fraction, isFirst)
	
	val offsetStart: Vec3
		get() = offset(0.0)
	
	val offsetEnd: Vec3
		get() = offset(1.0)
	
	val offsetCenter: Vec3 = offset(0.5)
	
	
	fun DiscoveredLocation(
		dimension: ResourceKey<Level>,
		normal: Vec3,
		tangent: Vec3?,
		yOffset: Int = 0,
		firstMaterial: TrackMaterial,
		secondMaterial: TrackMaterial,
		viaTurn: BezierConnection? = null,
	): DiscoveredLocation = factory(dimension, offsetCenter)
		.viaTurn(viaTurn)
		.materials(firstMaterial, secondMaterial)
		.withNormal(normal)
		.withDirection(tangent)
		.withYOffset(yOffset)
	
	fun DiscoveredLocation(
		level: BlockGetter,
		normal: Vec3,
		tangent: Vec3?,
		yOffset: Int = 0,
		viaTurn: BezierConnection? = null,
		firstMaterial: TrackMaterial = ITrackBlock.getMaterialSimple(level, offsetStart, viaTurn?.material),
		secondMaterial: TrackMaterial = ITrackBlock.getMaterialSimple(level, offsetEnd, viaTurn?.material),
	): DiscoveredLocation = DiscoveredLocation(
		dimension = (level as? Level)?.dimension() ?: Level.OVERWORLD,
		normal = normal,
		tangent = tangent,
		yOffset = yOffset,
		firstMaterial = firstMaterial,
		secondMaterial = secondMaterial,
		viaTurn = viaTurn,
	)
}
