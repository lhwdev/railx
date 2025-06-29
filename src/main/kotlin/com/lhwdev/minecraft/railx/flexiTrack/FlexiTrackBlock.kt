package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackShape
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.IHaveBigOutline
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.Vec3
import kotlin.math.min


class FlexiTrackBlock(properties: Properties, val material: TrackMaterial) : Block(properties),
	IBE<FlexiTrackBlockEntity>,
	IWrenchable,
	ITrackBlock,
	SpecialBlockItemRequirement,
	ProperWaterloggedBlock,
	IHaveBigOutline {
	companion object {
		val BaseDirection = FlexiDirectionProperty.create("direction")
		val HasBe: BooleanProperty = TrackBlock.HAS_BE
		val Waterlogged: BooleanProperty = TrackBlock.WATERLOGGED
	}
	
	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(BaseDirection, FlexiDirection.FlatDivision.Divisions[0])
				.setValue(HasBe, true)
				.setValue(Waterlogged, false)
		)
	}
	
	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder.add(BaseDirection, HasBe, Waterlogged))
	}
	
	
	override fun getBlockPathType(state: BlockState, level: BlockGetter, pos: BlockPos, mob: Mob?): PathType? {
		return PathType.RAIL
	}
	
	override fun getFluidState(state: BlockState): FluidState =
		fluidState(state)
	
	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		val stateForPlacement = withWater(super.getStateForPlacement(context), context)
		val player = context.player
		if(player == null) return stateForPlacement
		
		var lookAngle = player.lookAngle
		lookAngle = lookAngle.multiply(1.0, 0.0, 1.0)
		if(Mth.equal(lookAngle.length(), 0.0)) {
			lookAngle = VecHelper.rotate(Vec3(0.0, 0.0, 1.0), -player.yRot.toDouble(), Direction.Axis.Y)
		}
		
		lookAngle = lookAngle.normalize()
		
		var best = TrackShape.ZO
		var bestValue = Float.MAX_VALUE.toDouble()
		for(shape in TrackShape.entries) {
			if(shape.isJunction || shape.isPortal) continue
			val axis = shape.axes[0]
			val distance = min(
				axis.distanceToSqr(lookAngle), axis.normalize()
					.scale(-1.0)
					.distanceToSqr(lookAngle)
			)
			if(distance > bestValue) continue
			bestValue = distance
			best = shape
		}
		
		val level: Level = context.level
		val bestAxis = best.axes[0]
		if(bestAxis.lengthSqr() == 1.0) for(neg in Iterate.trueAndFalse) {
			val offset: BlockPos = context.clickedPos
				.offset(BlockPos.containing(bestAxis.scale((if(neg) -1 else 1).toDouble())))
			
			if(level.getBlockState(offset)
					.isFaceSturdy(level, offset, Direction.UP)
				&& !level.getBlockState(offset.above())
					.isFaceSturdy(level, offset, Direction.DOWN)
			) {
				if(best == TrackShape.XO) best = if(neg) TrackShape.AW else TrackShape.AE
				if(best == TrackShape.ZO) best = if(neg) TrackShape.AN else TrackShape.AS
			}
		}
		
		return stateForPlacement.setValue<TrackShape?, TrackShape?>(TrackBlock.SHAPE, best)
	}
}
