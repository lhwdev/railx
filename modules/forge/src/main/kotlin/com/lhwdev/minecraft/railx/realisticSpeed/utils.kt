package com.lhwdev.minecraft.railx.realisticSpeed

import com.simibubi.create.api.behaviour.movement.MovementBehaviour
import com.simibubi.create.content.contraptions.Contraption
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.shapes.VoxelShape


private val DummyTag = CompoundTag()

fun Contraption.loadBlockEntities(world: Level) {
	if(presentBlockEntities.isNotEmpty()) return
	val contraption = this as IContraptionBlockEntity
	
	for(info in blocks.values) {
		val be = contraption.onReadBlockEntity(world, info, DummyTag)
		if(be == null) continue
		
		presentBlockEntities[info.pos] = be
		if(!world.isClientSide) continue
		
		modelData[info.pos] = be.modelData
		val movementBehaviour = MovementBehaviour.REGISTRY.get(info.state())
		if(movementBehaviour == null || !movementBehaviour.disableBlockEntityRendering()) {
			renderedBlockEntities.add(be)
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
