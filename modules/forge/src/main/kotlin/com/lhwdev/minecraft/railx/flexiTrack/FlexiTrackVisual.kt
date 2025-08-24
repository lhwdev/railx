package com.lhwdev.minecraft.railx.flexiTrack

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.trains.track.TrackBlock
import com.simibubi.create.content.trains.track.TrackShape
import com.simibubi.create.content.trains.track.TrackVisual
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.world.level.LightLayer
import java.util.function.Consumer


class FlexiTrackVisual(context: VisualizationContext, track: FlexiTrackBlockEntity, partialTick: Float) :
	TrackVisual(context, track, partialTick) {
	protected val blockEntity: FlexiTrackBlockEntity
		get() = super.blockEntity as FlexiTrackBlockEntity
	
	private var blockVisual: BlockVisual? = null
	
	
	override fun _delete() {
		super._delete()
		blockVisual?.delete()
		blockVisual = null
		println("railx:visual delete blockVisual")
	}
	
	override fun update(pt: Float) {
		super.update(pt)
		blockVisual = BlockVisual()
		println("railx:visual update blockVisual")
	}
	
	override fun updateLight(partialTick: Float) {
		super.updateLight(partialTick)
		blockVisual?.updateLight()
	}
	
	override fun collectCrumblingInstances(to: Consumer<Instance?>) {
		super.collectCrumblingInstances(to)
		blockVisual?.collectCrumblingInstances(to)
	}
	
	
	private inner class BlockVisual {
		private val blocks: List<TransformedInstance>
		
		init {
			val trackBlock = blockEntity.block.material.block
			val trackState = trackBlock.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO)
			
			val instancer = instancerProvider()
				.instancer(InstanceTypes.TRANSFORMED, Models.block(trackState))
			
			blocks = blockEntity.shape.axes.map { axe ->
				val block = instancer.createInstance()
				val pose = PoseStack()
				TransformStack.of(pose)
					.translate(visualPosition)
					.rotateYCentered(axe.tangentAngle.toFloat())
				
				block.setTransform(pose)
				block.setChanged()
				block
			}
			
			updateLight()
		}
		
		fun delete() {
			blocks.forEach { it.delete() }
		}
		
		fun updateLight() {
			val blockLight = level.getBrightness(LightLayer.BLOCK, pos)
			val skyLight = level.getBrightness(LightLayer.SKY, pos)
			blocks.forEach { block ->
				block.light(blockLight, skyLight)
				block.setChanged()
			}
		}
		
		fun collectCrumblingInstances(to: Consumer<Instance?>) {
			blocks.forEach { to.accept(it) }
		}
	}
}
