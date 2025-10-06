package com.lhwdev.minecraft.railx.common

import com.lhwdev.minecraft.railx.middleTrack.MiddleTrackOutline
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockOutline
import net.minecraft.world.phys.Vec3


object TracksOutline {
	val result: TrackBezierPointSelection?
		get() {
			val value = calculate() ?: return null
			cache = value
			return value
		}
	
	private var cache: TrackBezierPointSelection? = null
	private var source: Any? = null
	
	private fun calculate(): TrackBezierPointSelection? {
		createTrack()?.let { return it }
		middleTrack()?.let { return it }
		return null
	}
	
	private fun createTrack(): TrackBezierPointSelection? {
		val source = TrackBlockOutline.result ?: return null
		if(source == this.source) return cache
		this.source = source
		return CreateBezierPointSelection(source)
	}
	
	private fun middleTrack(): TrackBezierPointSelection? {
		val source = MiddleTrackOutline.result ?: return null
		if(source == this.source) return cache
		this.source = source
		return source
	}
}


interface TrackBezierPointSelection {
	val curve: BezierConnection
	val segmentIndex: Int
	val position: Vec3
	val angles: Vec3
	val tangent: Vec3
}

private class CreateBezierPointSelection(private val source: TrackBlockOutline.BezierPointSelection) :
	TrackBezierPointSelection {
	override val curve: BezierConnection
		get() = source.blockEntity.connections[source.loc.curveTarget]!!
	override val segmentIndex: Int
		get() = source.loc.segment
	override val position: Vec3
		get() = source.vec
	override val angles: Vec3
		get() = source.angles
	override val tangent: Vec3
		get() = source.direction
}
