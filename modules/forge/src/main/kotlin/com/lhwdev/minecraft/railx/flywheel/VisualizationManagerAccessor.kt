package com.lhwdev.minecraft.railx.flywheel

import dev.engine_room.flywheel.api.visual.Effect
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.impl.task.Flag
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage
import dev.engine_room.flywheel.impl.visualization.storage.EffectStorage
import dev.engine_room.flywheel.impl.visualization.storage.EntityStorage
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity


@Suppress("NonExtendableApiUsage")
interface VisualizationManagerAccessor : VisualizationManager {
	val blockEntities: VisualManagerImpl<BlockEntity, BlockEntityStorage>
	val entities: VisualManagerImpl<Entity, EntityStorage>
	val effects: VisualManagerImpl<Effect, EffectStorage>
	
	val frameFlag: Flag
	val tickFlag: Flag
}
