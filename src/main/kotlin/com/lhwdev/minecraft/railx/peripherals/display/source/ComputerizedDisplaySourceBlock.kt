package com.lhwdev.minecraft.railx.peripherals.display.source

import com.lhwdev.minecraft.railx.Utils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class ComputerizedDisplaySourceBlock : Block(
	Properties.of()
		.mapColor(MapColor.WOOD)
		.instrument(NoteBlockInstrument.BASS)
		.strength(2.0f)
		.sound(SoundType.WOOD)
		.destroyTime(1f)
), EntityBlock {
	init {
		// Material.WOOD
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
	}
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		val facing = if(context.player?.isCrouching == true) {
			context.horizontalDirection
		} else {
			context.horizontalDirection.opposite
		}
		return defaultBlockState().setValue(FACING, facing)
	}
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		builder.add(FACING)
	}
	
	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
		return ComputerizedDisplaySourceBlockEntity(pos, state)
	}
	
	@Deprecated("deprecated for calling; not for overriding") // https://forums.minecraftforge.net/topic/83768-confusion-about-voxelshapes-and-blockstates-in-1151/
	override fun getShape(
		blockState: BlockState,
		blockGetter: BlockGetter,
		blockPos: BlockPos,
		collisionContext: CollisionContext,
	): VoxelShape {
		return Utils.rotate(
			Direction.NORTH,
			blockState.getValue(FACING),
			listOf(
				box(2.0, 2.0, -1.0, 14.0, 14.0, 1.0),
				box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
				box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
				box(1.0, 2.0, 1.0, 15.0, 14.0, 15.0)
			).reduce { v1, v2 -> Shapes.join(v1, v2, BooleanOp.OR) }
		)
	}
	
	override fun onNeighborChange(state: BlockState, level: LevelReader, pos: BlockPos, neighbor: BlockPos) {
		(level.getBlockEntity(pos) as ComputerizedDisplaySourceBlockEntity)
			.onNeighborChange(state, level, pos, neighbor)
	}
	
	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("deprecated for calling; not for overriding") // idk if this is true
	override fun onRemove(
		blockState: BlockState,
		level: Level,
		blockPos: BlockPos,
		blockStateIDK: BlockState,
		watIsDis: Boolean,
	) {
		(level.getBlockEntity(blockPos) as ComputerizedDisplaySourceBlockEntity).onBlockBreak()
	}
	
	companion object {
		val FACING = HorizontalDirectionalBlock.FACING
	}
}
