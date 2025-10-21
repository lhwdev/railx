package com.lhwdev.minecraft.railx.flexiTrack.rotate

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import kotlin.math.cos
import kotlin.math.sin


data class FlexiTrackRotation(
	val direction: Double, // 0 ~ 2PI, counter-clockwise, (1,0) -> (0,-1) -> (-1,0) -> (0,1)
	val gradient: Double, // positive for tangent going upward; 0.5PI for 90 degree upward.
	val tilt: Double, // -0.5PI for left 90 degree (normal facing left), +0.5PI for right 90 degree (normal facing right)
) {
	fun rotationValue(): Quaterniond =
		Quaterniond().rotationY(direction).rotateX(tilt).rotateZ(gradient)
}


val FlexiDirection.direction: Double
	get() = tangentAngle

val FlexiDirection.gradient: Double
	get() = Mth.atan2(tangent.y, tangent.horizontalDistance())

// point on tangent-plane: (normal dot directionRight=z,0,-x), normal dot directionUp=0,1,0)
val FlexiDirection.tilt: Double
	get() = Mth.atan2(normal.dot(Vec3(tangent.z, 0.0, -tangent.x).normalize()), normal.y)

fun FlexiDirection.rotationValue(): Quaterniond =
	Quaterniond().rotationY(direction).rotateX(tilt).rotateZ(gradient)


fun FlexiDirection.toRotation(): FlexiTrackRotation = FlexiTrackRotation(
	direction = direction,
	gradient = gradient,
	tilt = tilt,
)

private val NormalBase = Vec3(0.0, 1.0, 0.0)

fun FlexiTrackRotation.toDirection(): FlexiDirection = FlexiDirection.Two(
	tangent = Vec3(cos(gradient), sin(gradient), 0.0).yRot(direction.toFloat()),
	normal = NormalBase.zRot(-gradient.toFloat()).xRot(tilt.toFloat()).yRot(direction.toFloat()),
)
