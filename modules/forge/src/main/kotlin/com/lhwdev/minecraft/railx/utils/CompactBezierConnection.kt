package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.lhwdev.minecraft.railx.flexiTrack.asKnown
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.TrackMaterial
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object CompactBezierConnection {
	// size <= 127
	private val MaterialCache = mutableListOf<TrackMaterial>().apply {
		add(TrackMaterial.ANDESITE)
		add(FlexiTrackMaterial.Companion.Andesite)
	}
	
	
	fun read(tag: CompoundTag, localTo: BlockPos): BezierConnection? {
		if("D4" in tag) return try {
			readVOld(tag, localTo)
		} catch(_: Throwable) {
			null
		}
		if("B" !in tag) return null
		val bytes = tag.getByteArray("B")
		return when(bytes[0].toInt()) {
			1 -> readV1(bytes, localTo)
			else -> {
				RailX.Logger.error("Illegal version found: ${bytes[0]}")
				null
			}
		}
	}
	
	private fun readVOld(tag: CompoundTag, localTo: BlockPos): BezierConnection? {
		val bytes = tag.getByteArray("D4")
		val input = DataInputStream(ByteArrayInputStream(bytes))
		
		val flags = input.readUnsignedShort()
		
		// Note: given vec3 would be generally not too big; size would be no more than 1.
		fun readVec3MaybeFlat(flagIndex: Int): Vec3 = if(flags and (1 shl flagIndex) != 0) {
			Vec3(input.readFloat().toDouble(), 0.0, input.readFloat().toDouble())
		} else {
			Vec3(input.readFloat().toDouble(), input.readFloat().toDouble(), input.readFloat().toDouble())
		}
		
		fun readStart(blockPos: BlockPos, flagIndex: Int): Vec3 =
			readVec3MaybeFlat(flagIndex) + Vec3.atBottomCenterOf(blockPos)
		
		fun readAxis(flagIndex: Int): Vec3 {
			val flag2 = input.readUnsignedByte()
			return if(flag2 == 0) {
				readVec3MaybeFlat(flagIndex)
			} else {
				FlexiDirection.Known.fromIndex(flag2 - 1).tangent
			}
		}
		
		fun readVec3MaybeNormal(flagIndex: Int): Vec3 = if(flags and (1 shl flagIndex) != 0) {
			FlexiDirection.Flat.normal
		} else {
			Vec3(input.readFloat().toDouble(), input.readFloat().toDouble(), input.readFloat().toDouble())
		}
		
		val fromPos = BlockPos(input.readShort().toInt(), input.readByte().toInt(), input.readShort().toInt())
			.offset(localTo)
		val toPos = BlockPos(input.readShort().toInt(), input.readByte().toInt(), input.readShort().toInt())
			.offset(localTo)
		
		val bc = BezierConnection(
			Couple.create(fromPos, toPos),
			Couple.create(readStart(fromPos, 10), readStart(toPos, 11)),
			Couple.create(readAxis(12), readAxis(13)),
			Couple.create(readVec3MaybeNormal(14), readVec3MaybeNormal(15)),
			flags and 1 != 0,
			flags and 2 != 0,
			(flags shr 3 and 0x7f).let { type ->
				if(type == 0) {
					TrackMaterial.deserialize(input.readUTF())
				} else {
					MaterialCache[type - 1]
				}
			}
		)
		
		if(flags and 4 != 0) {
			bc.smoothing = Couple.create(input.readInt(), input.readInt())
		}
		
		return bc
	}
	
	private fun readV1(bytes: ByteArray, localTo: BlockPos): BezierConnection? {
		val input = DataInputStream(ByteArrayInputStream(bytes))
		input.skipBytes(1) // skip version
		
		// flag: [P G S M M M M M  M M s1 s2 a1 a2 n1 n2]
		// - P=primary G=hasGirder S=hasSmoothing M=material
		// - s=starts a=axes n=normals
		val flags = input.readUnsignedShort()
		
		// Note: given vec3 would be generally not too big; size would be no more than 1.
		fun readVec3MaybeFlat(flagIndex: Int): Vec3 = if(flags and (1 shl flagIndex) != 0) {
			Vec3(input.readFloat().toDouble(), 0.0, input.readFloat().toDouble())
		} else {
			Vec3(input.readFloat().toDouble(), input.readFloat().toDouble(), input.readFloat().toDouble())
		}
		
		fun readStart(blockPos: BlockPos, flagIndex: Int): Vec3 =
			readVec3MaybeFlat(flagIndex) + Vec3.atBottomCenterOf(blockPos)
		
		fun readAxis(flagIndex: Int): Vec3 {
			val flag2 = input.readUnsignedByte()
			return if(flag2 and 0x80 == 0) {
				readVec3MaybeFlat(flagIndex)
			} else {
				val value = (flag2 and 0x3f shl 8) or input.readUnsignedByte()
				val result = FlexiDirection.Known.fromIndex(value).tangent
				if(flag2 and 0x40 == 0) result else -result
			}
		}
		
		fun readVec3MaybeNormal(flagIndex: Int): Vec3 = if(flags and (1 shl flagIndex) != 0) {
			FlexiDirection.Flat.normal
		} else {
			Vec3(input.readFloat().toDouble(), input.readFloat().toDouble(), input.readFloat().toDouble())
		}
		
		val fromPos = BlockPos(input.readShort().toInt(), input.readByte().toInt(), input.readShort().toInt())
			.offset(localTo)
		val toPos = BlockPos(input.readShort().toInt(), input.readByte().toInt(), input.readShort().toInt())
			.offset(localTo)
		
		val bc = BezierConnection(
			Couple.create(fromPos, toPos),
			Couple.create(readStart(fromPos, 10), readStart(toPos, 11)),
			Couple.create(readAxis(12), readAxis(13)),
			Couple.create(readVec3MaybeNormal(14), readVec3MaybeNormal(15)),
			flags and 1 != 0,
			flags and 2 != 0,
			(flags shr 3 and 0x7f).let { type ->
				if(type == 0) {
					TrackMaterial.deserialize(input.readUTF())
				} else {
					MaterialCache[type - 1]
				}
			}
		)
		
		if(flags and 4 != 0) {
			bc.smoothing = Couple.create(input.readInt(), input.readInt())
		}
		
		return bc
	}
	
	fun write(bc: BezierConnection, localTo: BlockPos): CompoundTag = CompoundTag { tag ->
		val bytes = ByteArrayOutputStream()
		val output = DataOutputStream(bytes)
		output.write(1) // version
		
		var flags = 0
		output.writeShort(0) // placeholder for flags
		fun writeFlag(index: Int, data: Int) {
			flags = flags or (data shl index)
		}
		
		fun writeFlag(index: Int, data: Boolean) {
			writeFlag(index, if(data) 1 else 0)
		}
		writeFlag(0, bc.primary)
		writeFlag(1, bc.hasGirder)
		writeFlag(3, MaterialCache.indexOf(bc.material) + 1)
		
		fun write(pos: BlockPos) {
			val pos = pos.subtract(localTo)
			output.writeShort(pos.x)
			output.writeByte(pos.y)
			output.writeShort(pos.z)
		}
		
		write(bc.bePositions.first)
		write(bc.bePositions.second)
		
		fun writeMaybeFlat(vec: Vec3, flagIndex: Int) {
			output.writeFloat(vec.x.toFloat())
			if(vec.y similarTo 0.0) {
				writeFlag(flagIndex, true)
			} else {
				output.writeFloat(vec.y.toFloat())
			}
			output.writeFloat(vec.z.toFloat())
		}
		
		fun writeStart(pos: Vec3, blockPos: BlockPos, flagIndex: Int) {
			val relative = pos - Vec3.atBottomCenterOf(blockPos)
			writeMaybeFlat(relative, flagIndex)
		}
		
		writeStart(bc.starts.first, bc.bePositions.first, 10)
		writeStart(bc.starts.second, bc.bePositions.second, 11)
		
		fun writeAxis(axis: Vec3, flagIndex: Int) {
			// data: i) known -> 10?????? ????????, ii) known opposite ->  11?????? ????????
			// TODO: use asKnownSigned() on next upgrade
			axis.asKnown()?.let { return output.writeShort(it.index or 0x8000) }
			(-axis).asKnown()?.let { return output.writeShort(it.index or 0xc000) }
			
			output.writeByte(0)
			writeMaybeFlat(axis, flagIndex)
		}
		
		writeAxis(bc.axes.first, 12)
		writeAxis(bc.axes.second, 13)
		
		fun writeMaybeNormal(vec: Vec3, flagIndex: Int) {
			if(vec.x similarTo 0.0 && vec.z similarTo 0.0) {
				writeFlag(flagIndex, true)
			} else {
				output.writeFloat(vec.x.toFloat())
				output.writeFloat(vec.y.toFloat())
				output.writeFloat(vec.z.toFloat())
			}
		}
		
		writeMaybeNormal(bc.normals.first, 14)
		writeMaybeNormal(bc.normals.second, 15)
		
		if(bc.material !in MaterialCache) {
			output.writeUTF(bc.material.id.toString())
		}
		
		bc.smoothing?.let { smoothing ->
			writeFlag(2, true)
			output.writeInt(smoothing.first)
			output.writeInt(smoothing.second)
		}
		
		val result = bytes.toByteArray()
		result[1] = (flags shr 8).toByte()
		result[2] = (flags).toByte()
		
		tag.putByteArray("B", result)
	}
}
