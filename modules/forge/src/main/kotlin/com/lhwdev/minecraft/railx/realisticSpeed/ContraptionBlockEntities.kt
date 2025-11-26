package com.lhwdev.minecraft.railx.realisticSpeed

import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.render.ClientContraption
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.phys.shapes.VoxelShape
import java.lang.invoke.MethodHandles


interface ContraptionWithBlockEntity {
	fun `railx$serverSideBlockEntities`(level: Level): ServerSideContraptionBlockEntities
	fun `railx$blockEntities`(level: Level): ContraptionBlockEntities
}

fun Contraption.blockEntities(level: Level): ContraptionBlockEntities =
	(this as ContraptionWithBlockEntity).`railx$blockEntities`(level)

interface ContraptionBlockEntities {
	fun load()
	
	val blockEntities: Map<BlockPos, BlockEntity>
}

class ClientSideContraptionBlockEntities(private val base: ClientContraption) : ContraptionBlockEntities {
	companion object {
		private val CVirtualRenderWorld = VirtualRenderWorld::class.java
		private val CVirtualRenderWorld_blockEntities = CVirtualRenderWorld.getDeclaredField("blockEntities")
			.also { it.isAccessible = true }
			.let { MethodHandles.lookup().unreflectGetter(it) }
	}
	
	override val blockEntities: Map<BlockPos, BlockEntity>
		@Suppress("UNCHECKED_CAST")
		get() = CVirtualRenderWorld_blockEntities.invokeExact(base.renderLevel) as Map<BlockPos, BlockEntity>
	
	override fun load() {}
}

class ServerSideContraptionBlockEntities(private val contraption: Contraption, private val level: Level) :
	ContraptionBlockEntities {
	init {
		check(!level.isClientSide) { "ServerSideContraptionBlockEntity created on client side" }
	}
	
	override val blockEntities = HashMap<BlockPos, BlockEntity>()
	
	override fun load() {
		for(info in contraption.blocks.values) {
			val be = readBlockEntity(info, contraption.isLegacy.getBoolean(info.pos)) ?: continue
			
			blockEntities[info.pos] = be
		}
	}
	
	private fun readBlockEntity(info: StructureTemplate.StructureBlockInfo, legacy: Boolean): BlockEntity? {
		val state = info.state
		val pos = info.pos
		val nbt = info.nbt
		
		if(legacy) {
			if(nbt == null) return null
			nbt.putInt("x", pos.x)
			nbt.putInt("y", pos.y)
			nbt.putInt("z", pos.z)
			val be = BlockEntity.loadStatic(pos, state, nbt)
			postprocessReadBlockEntity(be, state)
			return be
		}
		
		val block = state.block
		if(!state.hasBlockEntity() || block !is EntityBlock) return null
		
		val be = block.newBlockEntity(pos, state)
		postprocessReadBlockEntity(be, state)
		if(be != null && nbt != null)
			be.handleUpdateTag(nbt)
		return be
	}
	
	private fun postprocessReadBlockEntity(be: BlockEntity?, blockState: BlockState) {
		if(be == null) return
		be.level = level
		@Suppress("DEPRECATION")
		be.blockState = blockState
		if(be is KineticBlockEntity) {
			be.speed = 0f
		}
	}
}


fun VoxelShape.calculateVolume(): Int {
	var sum = 0.0
	forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
		sum += (maxX - minX) * (maxY - minY) * (maxZ - minZ)
	}
	return (sum * BlockVolume).toInt()
}

const val BlockVolume: Int = 16 * 16 * 16
