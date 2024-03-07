@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.lhwdev.minecraft.railx.peripherals.redstoneLink

import com.lhwdev.minecraft.railx.Registries
import com.lhwdev.minecraft.railx.Utils
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class ComputerizedRedstoneLinkBlock : DirectionalBlock(
	Properties.of()
		.mapColor(MapColor.WOOD)
		.instrument(NoteBlockInstrument.BASS)
		.strength(2.0f)
		.sound(SoundType.WOOD)
		.destroyTime(1f)
), IBE<ComputerizedRedstoneLinkBlockEntity> {
	
	companion object {
		val FACING = DirectionalBlock.FACING
		private val shapeCache = mutableMapOf<Direction, VoxelShape>()
	}
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		return defaultBlockState().setValue(FACING, context.clickedFace)
	}
	
	override fun isPathfindable(
		state: BlockState,
		getter: BlockGetter,
		pos: BlockPos,
		type: PathComputationType,
	): Boolean = false
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		builder.add(FACING)
		super.createBlockStateDefinition(builder)
	}
	
	override fun neighborChanged(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		block: Block,
		fromPos: BlockPos,
		isMoving: Boolean,
	) {
		if(level.isClientSide) return
		
		val facing = state.getValue(FACING)
		val basePos = pos.relative(facing.opposite)
		if(fromPos == basePos) {
			if(!canSurvive(state, level, pos)) {
				level.destroyBlock(pos, true)
				return
			}
		}
		
		super.neighborChanged(state, level, pos, block, fromPos, isMoving)
	}
	
	override fun canSurvive(state: BlockState, reader: LevelReader, pos: BlockPos): Boolean {
		val basePos = pos.relative(state.getValue(FACING).opposite)
		return reader.getBlockState(basePos).canBeReplaced()
	}
	
	
	override fun getShape(
		state: BlockState,
		getter: BlockGetter,
		pos: BlockPos,
		collisionContext: CollisionContext,
	): VoxelShape {
		val facing = state.getValue(FACING)
		return shapeCache.getOrPut(facing) {
			Utils.rotate(
				Direction.UP,
				facing,
				box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
			)
		}
	}
	
	
	override fun getBlockEntityClass() = ComputerizedRedstoneLinkBlockEntity::class.java
	
	override fun getBlockEntityType() = Registries.COMPUTERIZED_REDSTONE_LINK_BLOCK_ENTITY.get()
}
