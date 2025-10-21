package com.lhwdev.minecraft.railx.flexiTrack.graph

import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt


// x/z: in -63..63; y in 0..63
private const val HScale = 64.0
private const val HBase = 63
private const val VScale = 128.0


class TrackNodeLocationDelta private constructor(
	base: TrackNodeLocation,
	@JvmField val x: Int,
	@JvmField val y: Int,
	@JvmField val z: Int,
) {
	companion object {
		@JvmStatic
		fun of(base: TrackNodeLocation, vec: Vec3): TrackNodeLocationDelta? {
			if(vec.isIntTrackNodeLocation()) return null
			
			val x = ((vec.x * 2.0 - base.x) * HScale).roundToInt().coerceIn(-HBase, HBase)
			val y = ((vec.y - base.y * 0.5) * VScale).roundToInt().coerceIn(0, 127)
			val z = ((vec.z * 2.0 - base.z) * HScale).roundToInt().coerceIn(-HBase, HBase)
			return TrackNodeLocationDelta(base, x, y, z)
		}
		
		@JvmField
		val DummyBytes: ByteArray = byteArrayOf(HBase.toByte(), 0, HBase.toByte())
		
		@JvmStatic
		fun fromByteArray(base: TrackNodeLocation, array: ByteArray): TrackNodeLocationDelta? {
			val x = array[0].toUByte().toInt() - HBase
			val y = array[1].toUByte().toInt()
			val z = array[2].toUByte().toInt() - HBase
			
			if(x == 0 && y == 0 && z == 0) return null
			return TrackNodeLocationDelta(base, x, y, z)
		}
	}
	
	val location: Vec3 = Vec3(
		(base.x + x / HScale) * 0.5,
		base.y * 0.5 + y / VScale,
		(base.z + z / HScale) * 0.5,
	)
	
	fun toByteArray(): ByteArray = byteArrayOf((x + HBase).toByte(), y.toByte(), (z + HBase).toByte())
	
	override fun hashCode(): Int = (x * 31 + y) * 31 + z
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is TrackNodeLocationDelta -> false
		else -> x == other.x && y == other.y && z == other.z
	}
}

private fun Vec3.isIntTrackNodeLocation(): Boolean {
	val x = x * 2.0
	val z = z * 2.0
	return round(x) similarTo x &&
		floor(y) similarTo y &&
		round(z) similarTo z
}
