package com.lhwdev.minecraft.utils.vectors

import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3fc


fun Vec3i.toVec3(): Vec3 =
	Vec3(x.toDouble(), y.toDouble(), z.toDouble())

fun Vec3i.toVector3d(): Vector3d =
	Vector3d(x.toDouble(), y.toDouble(), z.toDouble())


fun Vec3.toVector3d(): Vector3d =
	Vector3d(x, y, z)


fun Vector3dc.toVec3(): Vec3 =
	Vec3(x(), y(), z())

fun Vector3fc.toVec3(): Vec3 =
	Vec3(x().toDouble(), y().toDouble(), z().toDouble())

