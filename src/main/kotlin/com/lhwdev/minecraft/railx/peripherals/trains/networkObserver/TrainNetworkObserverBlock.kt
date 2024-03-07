package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.lhwdev.minecraft.railx.Registries
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor

class TrainNetworkObserverBlock : Block(
	Properties.of()
		.mapColor(MapColor.WOOD)
		.instrument(NoteBlockInstrument.BASS)
		.strength(2.0f)
		.sound(SoundType.WOOD)
		.destroyTime(1f)
), IBE<TrainNetworkObserverBlockEntity> {
	
	override fun getBlockEntityClass() = TrainNetworkObserverBlockEntity::class.java
	override fun getBlockEntityType() = Registries.TRAIN_NETWORK_OBSERVER_BLOCK_ENTITY.get()
	
	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("deprecated for calling; not for overriding")
	override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, p_60519_: Boolean) {
		if(state.hasBlockEntity() && (!state.`is`(newState.block) || !newState.hasBlockEntity())) {
			level.removeBlockEntity(pos)
		}
	}
	
	//	@Deprecated("deprecated for calling; not for overriding")
	//	override fun getShape(
	//		blockState: BlockState,
	//		blockGetter: BlockGetter,
	//		blockPos: BlockPos,
	//		collisionContext: CollisionContext
	//	): VoxelShape {
	//		return Stream.of(
	//			box(2.0, 15.0, 2.0, 14.0, 17.0, 14.0),
	//			box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
	//			box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
	//			box(1.0, 2.0, 1.0, 15.0, 14.0, 15.0),
	//			box(15.0, 2.0, 0.0, 16.0, 14.0, 1.0),
	//			box(15.0, 2.0, 15.0, 16.0, 14.0, 16.0),
	//			box(0.0, 2.0, 15.0, 1.0, 14.0, 16.0),
	//			box(0.0, 2.0, 0.0, 1.0, 14.0, 1.0)
	//		).reduce { v1, v2 -> Shapes.join(v1, v2, BooleanOp.OR) }.get()
	//	}
}
