package com.lhwdev.minecraft.railx.advancedRoller

import com.simibubi.create.content.contraptions.actors.roller.RollerActorVisual
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock
import com.simibubi.create.content.contraptions.actors.roller.RollerRenderer
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.contraptions.pulley.PulleyContraption
import com.simibubi.create.content.contraptions.render.ActorVisual
import com.simibubi.create.content.contraptions.render.ContraptionMatrices
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour
import com.simibubi.create.foundation.damageTypes.CreateDamageSources
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import com.simibubi.create.AllTags as CreateTags


class UniversalRollerMovementBehavior : BlockBreakingMovementBehaviour() {
	override fun isActive(context: MovementContext): Boolean {
		val facing = context.state.getValue(RollerBlock.FACING)
		
		return super.isActive(context) &&
			(context.contraption !is PulleyContraption) &&
			VecHelper.isVecPointingTowards(context.relativeMotion, facing)
	}
	
	override fun disableBlockEntityRendering(): Boolean =
		true
	
	override fun createVisual(
		visualizationContext: VisualizationContext,
		simulationWorld: VirtualRenderWorld,
		movementContext: MovementContext,
	): ActorVisual = RollerActorVisual(visualizationContext, simulationWorld, movementContext)
	
	override fun renderInContraption(
		context: MovementContext,
		renderWorld: VirtualRenderWorld,
		matrices: ContraptionMatrices,
		buffer: MultiBufferSource,
	) {
		if(VisualizationManager.supportsVisualization(context.world)) return
		
		RollerRenderer.renderInContraption(context, renderWorld, matrices, buffer)
	}
	
	override fun getActiveAreaOffset(context: MovementContext): Vec3 {
		val facing = context.state.getValue(RollerBlock.FACING)
		return Vec3.atLowerCornerOf(facing.normal)
			.scale(.45)
			.subtract(0.0, 2.0, 0.0)
	}
	
	override fun getBlockBreakingSpeed(context: MovementContext?): Float {
		return Mth.clamp(super.getBlockBreakingSpeed(context) * 1.5f, 1 / 128f, 16f)
	}
	
	override fun canBreak(world: Level, breakingPos: BlockPos, state: BlockState): Boolean {
		for(side in Iterate.directions) {
			val blockState = world.getBlockState(breakingPos.relative(side))
			if(blockState.`is`(BlockTags.PORTALS)) return false
		}
		
		return super.canBreak(world, breakingPos, state) &&
			!state.getCollisionShape(world, breakingPos).isEmpty &&
			!CreateTags.AllBlockTags.TRACKS.matches(state)
	}
	
	override fun getDamageSource(level: Level): DamageSource =
		CreateDamageSources.roller(level)
	
}
