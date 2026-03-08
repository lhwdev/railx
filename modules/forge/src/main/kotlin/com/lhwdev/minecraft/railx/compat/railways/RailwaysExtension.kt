package com.lhwdev.minecraft.railx.compat.railways

import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackBlockEntity
import net.minecraft.world.level.block.Block


object RailwaysCasingExtension {
	@JvmStatic
	var TrackBlockEntity.isAlternate: Boolean
		get() = (this as IHasTrackCasing).`railways$isAlternate`()
		set(value) {
			(this as IHasTrackCasing).`railways$setAlternate`(value)
		}
	
	@JvmStatic
	var TrackBlockEntity.trackCasing: Block?
		get() = (this as IHasTrackCasing).`railways$getTrackCasing`()
		set(value) {
			(this as IHasTrackCasing).`railways$setTrackCasing`(value)
		}
	
	
	@JvmStatic
	var BezierConnection.isAlternate: Boolean
		get() = (this as IHasTrackCasing).`railways$isAlternate`()
		set(value) {
			(this as IHasTrackCasing).`railways$setAlternate`(value)
		}
	
	@JvmStatic
	var BezierConnection.trackCasing: Block?
		get() = (this as IHasTrackCasing).`railways$getTrackCasing`()
		set(value) {
			(this as IHasTrackCasing).`railways$setTrackCasing`(value)
		}
}
