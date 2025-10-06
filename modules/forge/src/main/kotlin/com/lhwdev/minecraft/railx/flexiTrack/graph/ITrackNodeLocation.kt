package com.lhwdev.minecraft.railx.flexiTrack.graph

import net.minecraft.world.phys.Vec3


interface ITrackNodeLocation {
	fun getVecLocation(): Vec3?
	
	fun setVecLocation(value: Vec3?)
}
