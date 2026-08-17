package com.lhwdev.minecraft.railx.common.gravelLayer

import com.lhwdev.minecraft.utils.vectors.minus
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
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.min


class GravelLayerByteBlock(properties: Properties) : Block(properties), IWrenchable {
	companion object {
		const val MaxLayer: Int = 2
		
		val LayersA: IntegerProperty = IntegerProperty.create("layers_a", 0, MaxLayer)
		val LayersB: IntegerProperty = IntegerProperty.create("layers_b", 0, MaxLayer)
		val LayersC: IntegerProperty = IntegerProperty.create("layers_c", 0, MaxLayer)
		val LayersD: IntegerProperty = IntegerProperty.create("layers_d", 0, MaxLayer)
	}
	
	
	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(LayersA, 1)
				.setValue(LayersB, 0)
				.setValue(LayersC, 0)
				.setValue(LayersD, 0)
		)
	}
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(LayersA, LayersB, LayersC, LayersD)
	}
	
	override fun isPathfindable(state: BlockState, pathComputationType: PathComputationType): Boolean {
		return when(pathComputationType) {
			PathComputationType.LAND ->
				state.getValue(LayersA) < MaxLayer / 2 + 1 ||
					state.getValue(LayersB) < MaxLayer / 2 + 1 ||
					state.getValue(LayersC) < MaxLayer / 2 + 1 ||
					state.getValue(LayersD) < MaxLayer / 2 + 1
			
			else -> false
		}
	}
	
	@Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
	override fun getShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext?,
	): VoxelShape =
		box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
	
	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape = getShape(state, level, pos, context)
	
	override fun getBlockSupportShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape =
		getShape(state, level, pos, null)
	
	override fun getVisualShape(
		state: BlockState,
		level: BlockGetter,
		pos: BlockPos,
		context: CollisionContext,
	): VoxelShape = getShape(state, level, pos, context)
	
	override fun useShapeForLightOcclusion(state: BlockState): Boolean = true
	
	override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float =
		if(state.getValue(LayersA) == MaxLayer) 0.2f else 1.0f
	
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
		val full =
			state.getValue(LayersA) == MaxLayer ||
				state.getValue(LayersB) == MaxLayer ||
				state.getValue(LayersC) == MaxLayer ||
				state.getValue(LayersD) == MaxLayer
		
		if(!useContext.itemInHand.`is`(asItem()) || full)
			return false
		
		return if(useContext.replacingClickedOnBlock()) {
			useContext.clickedFace == Direction.UP
		} else {
			true
		}
	}
	
	private fun getLayerByPos(localPos: Vec3): IntegerProperty = if(localPos.x > 0.5) {
		if(localPos.z > 0.5) LayersD else LayersC
	} else {
		if(localPos.z > 0.5) LayersB else LayersA
	}
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
		val previousState = context.level.getBlockState(context.clickedPos)
		return if(previousState.`is`(this)) {
			val localPos = context.clickLocation - Vec3.atBottomCenterOf(context.clickedPos)
			val layer = getLayerByPos(localPos)
			previousState.setValue(layer, min(previousState.getValue(layer) + 1, 8))
		} else {
			super.getStateForPlacement(context)
		}
	}
	
	override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
		InteractionResult.PASS
	
	override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
		val localPos = context.clickLocation - Vec3.atBottomCenterOf(context.clickedPos)
		val layer = getLayerByPos(localPos)
		val layers = state.getValue(layer)
		
		var fullCount = 0
		if(state.getValue(LayersA) > 0) fullCount++
		if(state.getValue(LayersB) > 0) fullCount++
		if(state.getValue(LayersC) > 0) fullCount++
		if(state.getValue(LayersD) > 0) fullCount++
		
		if(layers > 1 || fullCount > 1) {
			context.level.setBlockAndUpdate(context.clickedPos, state.setValue(layer, layers - 1))
		} else {
			context.level.destroyBlock(context.clickedPos, true)
		}
		return InteractionResult.SUCCESS
	}
}
