package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackMaterial
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity
import com.simibubi.create.content.trains.track.TrackMaterial
import net.createmod.catnip.data.Couple
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


class MiddleTrackBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
	FakeTrackBlockEntity(type, pos, state) {
	
	var connections: List<BezierConnection> = emptyList()
		private set
	
	
	private var pendingConnections: List<BezierConnection>? = null
	
	fun updateConnections(value: List<BezierConnection>) {
		val level = level
		if(level == null || level.getBlockEntity(blockPos) !is MiddleTrackBlockEntity) {
			pendingConnections = value
			return
		}
		
		if(value.isEmpty() && !level.isClientSide) {
			Error("value.isEmpty()").printStackTrace()
			connections = emptyList()
			level.removeBlock(blockPos, false)
			return
		}
		
		notifyUpdate()
		
		if(level.isClientSide) {
			GlobalConnections[level].updateMiddle(this, value)
		}
		connections = value
	}
	
	override fun setLevel(level: Level) {
		super.setLevel(level)
		
		// pendingConnections?.let {
		// 	pendingConnections = null
		// 	updateConnections(it)
		// }
	}
	
	override fun onLoad() {
		super.onLoad()
		
		pendingConnections?.let {
			pendingConnections = null
			updateConnections(it)
		}
	}
	
	// Be aware that this is also called on chunk unload!
	override fun setRemoved() {
		super.setRemoved()
		
		val level = level
		if(level != null) {
			GlobalConnections[level].removeMiddle(this)
		}
	}
	
	
	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.put("Connections", connections.mapTo(ListTag()) { CompactBezierConnection.write(it, blockPos) })
	}
	
	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		updateConnections((tag.get("Connections") as ListTag).mapNotNull { t ->
			CompactBezierConnection.read(t as CompoundTag, blockPos)
				?.also { require(it.primary) { "curve is not primary" } }
		})
	}
}


private object CompactBezierConnection {
	// size <= 127
	private val MaterialCache = mutableListOf<TrackMaterial>().apply {
		add(TrackMaterial.ANDESITE)
		add(FlexiTrackMaterial.Andesite)
	}
	
	
	fun read(tag: CompoundTag, localTo: BlockPos): BezierConnection? {
		if("D3" !in tag) return null
		val bytes = tag.getByteArray("D3")
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
			Couple.create(readVec3MaybeFlat(12), readVec3MaybeFlat(13)),
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
	
	fun write(bc: BezierConnection, localTo: BlockPos): CompoundTag = CompoundTag().also { tag ->
		val bytes = ByteArrayOutputStream()
		val output = DataOutputStream(bytes)
		
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
			if(vec.y == 0.0) {
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
		
		writeMaybeFlat(bc.axes.first, 12)
		writeMaybeFlat(bc.axes.second, 13)
		
		fun writeMaybeNormal(vec: Vec3, flagIndex: Int) {
			if(vec.x == 0.0 && vec.z == 0.0) {
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
		result[0] = (flags shr 8).toByte()
		result[1] = (flags).toByte()
		
		tag.putByteArray("D3", result)
	}
}
