@file:JvmName("DiscoveredLocationUtils")

package com.lhwdev.minecraft.railx.common

import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.lang.invoke.MethodHandles


private val lookup = MethodHandles.lookup()

private val CDiscoveredLocation = DiscoveredLocation::class.java

private val DiscoveredLocation_normal = CDiscoveredLocation.getDeclaredField("normal")
	.also { it.isAccessible = true }
	.let { lookup.unreflectGetter(it) }

val DiscoveredLocation.normal: Vec3
	get() = DiscoveredLocation_normal.invokeExact(this) as Vec3


fun <T : DiscoveredLocation> MutableCollection<T>.addIfConnected(
	fromEnd: TrackNodeLocation?,
	getLocation: (isFirst: Boolean) -> T,
) {
	val first = getLocation(true)
	val second = getLocation(false)
	if(first.dimension != second.dimension) {
		first.forceNode()
		second.forceNode()
	}
	
	if(fromEnd != null) {
		val equalsFirst = first == fromEnd
		val equalsSecond = second == fromEnd
		if(!equalsFirst && !equalsSecond) return
		if(!equalsFirst) this += first
		if(!equalsSecond) this += second
	} else {
		this += first
		this += second
	}
}

fun MutableCollection<DiscoveredLocation>.addIfConnected(
	fromEnd: TrackNodeLocation?,
	getOffset: (fraction: Double, isFirst: Boolean) -> Vec3,
	getLocation: DiscoveredLocationFactory<DiscoveredLocation>.() -> DiscoveredLocation,
) {
	addIfConnected(fromEnd) { isFirst ->
		DiscoveredLocationFactory(::DiscoveredLocation, getOffset, isFirst).getLocation()
	}
}

fun <T : DiscoveredLocation> MutableCollection<T>.addIfConnected(
	fromEnd: TrackNodeLocation?,
	getOffset: (fraction: Double, isFirst: Boolean) -> Vec3,
	factory: (dimension: ResourceKey<Level>, vec: Vec3) -> T,
	getLocation: DiscoveredLocationFactory<T>.() -> T,
) {
	addIfConnected(fromEnd) { isFirst ->
		DiscoveredLocationFactory(factory, getOffset, isFirst).getLocation()
	}
}


class DiscoveredLocationFactory<T : DiscoveredLocation>(
	private val factory: (ResourceKey<Level>, Vec3) -> T,
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
	): T = factory(dimension, offsetCenter).apply {
		viaTurn(viaTurn)
		materials(firstMaterial, secondMaterial)
		withNormal(normal)
		withDirection(tangent)
		withYOffset(yOffset)
	}
	
	fun DiscoveredLocation(
		level: BlockGetter,
		normal: Vec3,
		tangent: Vec3?,
		yOffset: Int = 0,
		viaTurn: BezierConnection? = null,
		firstMaterial: TrackMaterial = ITrackBlock.getMaterialSimple(level, offsetStart, viaTurn?.material),
		secondMaterial: TrackMaterial = ITrackBlock.getMaterialSimple(level, offsetEnd, viaTurn?.material),
	): T = DiscoveredLocation(
		dimension = (level as? Level)?.dimension() ?: Level.OVERWORLD,
		normal = normal,
		tangent = tangent,
		yOffset = yOffset,
		firstMaterial = firstMaterial,
		secondMaterial = secondMaterial,
		viaTurn = viaTurn,
	)
}
