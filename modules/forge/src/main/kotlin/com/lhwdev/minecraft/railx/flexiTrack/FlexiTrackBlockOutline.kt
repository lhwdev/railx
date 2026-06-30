package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.AllTags
import com.simibubi.create.content.trains.track.TrackBlockOutline
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.client.event.RenderHighlightEvent

object FlexiTrackBlockOutline {
	fun drawCustomBlockSelection(event: RenderHighlightEvent.Block): Boolean {
		val mc = Minecraft.getInstance()
		val level = mc.level!!
		val target = event.target
		val pos = target.blockPos
		val blockEntity = level.getBlockEntity(pos)
		
		if(!level.worldBorder.isWithinBounds(pos)) return true
		if(blockEntity !is FlexiTrackBlockEntity) return false
		
		val vb = event.multiBufferSource.getBuffer(RenderType.lines())
		val camPos = event.camera.position
		
		val ms = event.poseStack
		
		ms.pushPose()
		ms.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)
		
		val holdingTrack = AllTags.AllItemTags.TRACKS.matches(mc.player!!.mainHandItem)
		val shape = blockEntity.shape
		val canConnectFrom = !shape.isJunction && !blockEntity.isTilted
		
		walkShapes(
			blockEntity = blockEntity,
			msr = TransformStack.of(ms),
			renderer = { s ->
				TrackBlockOutline.renderShape(s, ms, vb, if(holdingTrack) canConnectFrom else null)
				event.setCanceled(true)
			}
		)
		
		ms.popPose()
		return true
	}
	
	fun walkShapes(blockEntity: FlexiTrackBlockEntity, msr: TransformStack<*>, renderer: (VoxelShape) -> Unit) {
		for(axis in blockEntity.state.shapeCache) {
			msr.pushPose()
			msr.rotateAround(axis.rotationValue, blockEntity.center)
			renderer(blockEntity.block.voxelShapes.base)
			msr.popPose()
		}
	}
}
