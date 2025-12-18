package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.AllTags
import com.simibubi.create.content.trains.track.TrackBlockOutline
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.client.event.RenderHighlightEvent
import kotlin.math.sqrt

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
		
		val holdingTrack = AllTags.AllBlockTags.TRACKS.matches(mc.player!!.mainHandItem)
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
		// According to Rodrigues' rotation formula, when normal = (x, y, z), k = (a, 0, c),
		// (x,y,z)=(0,1,0)cos + k*(0,1,0)sin + ky k (1-cos)
		//        =(0,1,0)cos + (-c,0,a)sin
		//        =(-c sin, cos, a sin)
		// conclusion: cos=y, sin=sqrt(1-y^2), c=-x/sin, a=z/sin
		// k = (z, 0, -x) / sqrt(1-y^2)
		val shape = blockEntity.shape
		val normal = shape.normal
		val sin = sqrt(1.0 - normal.y * normal.y)
		msr.rotate(
			Mth.atan2(sin, normal.y).toFloat(),
			(normal.z / sin).toFloat(),
			0f,
			(-normal.x / sin).toFloat(),
		)
		
		for(axis in shape.axes) {
			msr.pushPose()
			msr.rotateYCentered(axis.tangentAngle.toFloat())
			renderer(blockEntity.block.voxelShapes.base)
			msr.popPose()
		}
	}
}
