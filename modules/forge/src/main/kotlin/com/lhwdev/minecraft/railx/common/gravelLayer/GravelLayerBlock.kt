package com.lhwdev.minecraft.railx.common.gravelLayer

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.max
import kotlin.math.min


class GravelLayerBlock(properties: Properties) : Block(properties), IWrenchable {
	companion object {
		val Layers: IntegerProperty = BlockStateProperties.LAYERS
		
		const val MaxLayer: Int = 8
		
		val ShapeByLayer: Array<VoxelShape> = arrayOf(
			Shapes.empty(),
			box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
			box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
		)
	}
	
	
	init {
		registerDefaultState(defaultBlockState().setValue(Layers, 1))
	}
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(Layers)
	}
	
	override fun isPathfindable(state: BlockState, pathComputationType: PathComputationType): Boolean {
		return when(pathComputationType) {
			PathComputationType.LAND -> state.getValue(Layers) < 5
			else -> false
		}
	}
	
	override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
		ShapeByLayer[state.getValue(Layers)]
	
	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape = ShapeByLayer[max(0, state.getValue(Layers) - 1)]
	
	override fun getBlockSupportShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape =
		ShapeByLayer[state.getValue(Layers)]
	
	override fun getVisualShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape = ShapeByLayer[state.getValue(Layers)]
	
	override fun useShapeForLightOcclusion(state: BlockState): Boolean = true
	
	override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float =
		if(state.getValue(Layers) == 8) 0.2f else 1.0f
	
	override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
		val belowState = level.getBlockState(pos.below())
		if(belowState.isAir) return true
		if(belowState.canBeReplaced()) return false
		// if(belowState.`is`(this) && belowState.getValue(Layers) < 8) return false
		return true
	}
	
	override fun updateShape(
		state: BlockState,
		direction: Direction,
		neighborState: BlockState,
		level: LevelAccessor,
		pos: BlockPos,
		neighborPos: BlockPos,
	): BlockState =
		if(!state.canSurvive(level, pos)) Blocks.AIR.defaultBlockState()
		else super.updateShape(state, direction, neighborState, level, pos, neighborPos)
	
	override fun canBeReplaced(state: BlockState, useContext: BlockPlaceContext): Boolean {
		val layer = state.getValue(Layers)
		if(!useContext.itemInHand.`is`(asItem()) || layer >= 8)
			return false;
		
		return if(useContext.replacingClickedOnBlock()) {
			useContext.clickedFace == Direction.UP
		} else {
			true
		}
	}
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
		val previousState = context.level.getBlockState(context.clickedPos)
		return if(previousState.`is`(this)) {
			previousState.setValue(Layers, min(previousState.getValue(Layers) + 1, 8))
		} else {
			super.getStateForPlacement(context)
		}
	}
	
	override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
		InteractionResult.PASS
	
	override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
		val layers = state.getValue(Layers)
		if(layers > 1) {
			context.level.setBlockAndUpdate(context.clickedPos, state.setValue(Layers, layers - 1))
		} else {
			context.level.destroyBlock(context.clickedPos, true)
		}
		return InteractionResult.SUCCESS
	}
}
