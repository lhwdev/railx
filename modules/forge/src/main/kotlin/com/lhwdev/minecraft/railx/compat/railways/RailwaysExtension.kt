package com.lhwdev.minecraft.railx.compat.railways

import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockEntity
import net.minecraft.world.level.block.SlabBlock


object RailwaysCasingExtension {
	@JvmStatic
	var TrackBlockEntity.isAlternate: Boolean
		get() = (this as IHasTrackCasing).isAlternate
		set(value) {
			(this as IHasTrackCasing).isAlternate = value
		}
	
	@JvmStatic
	var TrackBlockEntity.trackCasing: SlabBlock?
		get() = (this as IHasTrackCasing).trackCasing
		set(value) {
			(this as IHasTrackCasing).trackCasing = value
		}
	
	
	@JvmStatic
	var BezierConnection.isAlternate: Boolean
		get() = (this as IHasTrackCasing).isAlternate
		set(value) {
			(this as IHasTrackCasing).isAlternate = value
		}
	
	@JvmStatic
	var BezierConnection.trackCasing: SlabBlock?
		get() = (this as IHasTrackCasing).trackCasing
		set(value) {
			(this as IHasTrackCasing).trackCasing = value
		}
}
