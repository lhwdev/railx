package com.lhwdev.minecraft.railx.flexiTrack.graph

import com.lhwdev.minecraft.railx.utils.similarTo
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

class TrackNodeLocationDelta private constructor(
	base: TrackNodeLocation,
	@JvmField val x: Int,
	@JvmField val y: Int,
	@JvmField val z: Int,
) {
	/** returns -1 if this cannot be replaced with `yOffset`. */
	fun asYOffset(): Int {
		if(x != 0 || z != 0) return -1
		if(y % VScaleToYOffset != 0) return -1
		return y / VScaleToYOffset
	}
	
	companion object {
		// x/z: in -63..63; y in 0..127
		const val HScale = 64.0
		const val HBase = 63
		const val VScale = 128.0
		private const val VScaleToYOffset = VScale.toInt() / 16
		
		
		@JvmStatic
		fun of(base: TrackNodeLocation, vec: Vec3): TrackNodeLocationDelta? =
			of(base, vec.x, vec.y, vec.z)
		
		@JvmStatic
		fun of(base: TrackNodeLocation, x: Double, y: Double, z: Double): TrackNodeLocationDelta? {
			if(isIntTrackNodeLocation(x, y, z)) return null
			
			val dx = ((x * 2.0 - base.x) * HScale).roundToInt().coerceIn(-HBase, HBase)
			val dy = Mth.floor((y - base.y * 0.5) * VScale).coerceIn(0, 127)
			val dz = ((z * 2.0 - base.z) * HScale).roundToInt().coerceIn(-HBase, HBase)
			return TrackNodeLocationDelta(base, dx, dy, dz)
		}
		
		@JvmField
		val DefaultAsBytes: ByteArray = byteArrayOf(HBase.toByte(), 0, HBase.toByte())
		
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

private fun isIntTrackNodeLocation(x: Double, y: Double, z: Double): Boolean {
	val x = x * 2.0
	val z = z * 2.0
	return round(x) similarTo x &&
		floor(y) similarTo y &&
		round(z) similarTo z
}
