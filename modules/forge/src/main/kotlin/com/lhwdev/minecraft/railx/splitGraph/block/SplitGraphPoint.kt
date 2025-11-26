package com.lhwdev.minecraft.railx.splitGraph.block

import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.forge.vectorutil.v3d.plus


interface SplitGraphPoint {
	val tangent: Vec3
	val normal: Vec3
	
	val from: TrackNodeLocation
	val fromDiscovered: DiscoveredLocation
	
	val center: TrackNodeLocation
	val centerDiscovered: DiscoveredLocation
	
	val to: TrackNodeLocation
	val toDiscovered: DiscoveredLocation
}

abstract class SplitGraphPointBase : SplitGraphPoint {
	protected abstract val track: SplitGraphTrack
	
	protected abstract val centerVec: Vec3
	protected abstract val dimension: ResourceKey<Level>
	
	override val from: TrackNodeLocation
		get() = TrackNodeLocation(centerVec + tangent.scale(-0.5)).`in`(dimension)
	
	override val fromDiscovered: DiscoveredLocation
		get() = DiscoveredLocation(dimension, centerVec + tangent.scale(-0.5)).apply {
			materials(track.material, track.material)
			withNormal(normal)
			withDirection(tangent)
			forceNode()
		}
	
	override val center: TrackNodeLocation
		get() = TrackNodeLocation(centerVec).`in`(dimension)
	
	override val centerDiscovered: DiscoveredLocation
		get() = DiscoveredLocation(dimension, centerVec).apply {
			materials(track.material, track.material)
			withNormal(normal)
			withDirection(tangent)
			forceNode()
		}
	
	override val to: TrackNodeLocation
		get() = TrackNodeLocation(centerVec + tangent.scale(0.5)).`in`(dimension)
	
	override val toDiscovered: DiscoveredLocation
		get() = DiscoveredLocation(dimension, centerVec + tangent.scale(0.5)).apply {
			materials(track.material, track.material)
			withNormal(normal)
			withDirection(tangent)
			forceNode()
		}
}
