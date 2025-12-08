package com.lhwdev.minecraft.utils.vectors

import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc


fun Vector3dc.add(v: Vec3, destination: Vector3d) {
	add(v.x, v.y, v.z, destination)
}

fun Vector3d.add(v: Vec3) {
	add(v.x, v.y, v.z)
}
