package com.lhwdev.minecraft.railx.flexiTrack.mixin

import com.lhwdev.minecraft.railx.flexiTrack.FlexiShape
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackBlock
import com.lhwdev.minecraft.railx.flexiTrack.FlexiTrackVoxelShapes
import com.lhwdev.minecraft.railx.flexiTrack.isJunction
import com.simibubi.create.AllTags
import com.simibubi.create.content.trains.track.TrackBlockEntity
import com.simibubi.create.content.trains.track.TrackBlockOutline
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.client.event.RenderHighlightEvent
import kotlin.math.atan2
import kotlin.math.sqrt


object TrackBlockOutlineHelper {
	fun drawCustomBlockSelection(event: RenderHighlightEvent.Block): Boolean {
		val mc = Minecraft.getInstance()
		val level = mc.level!!
		val target = event.target
		val pos = target.blockPos
		val state = level.getBlockState(pos)
		
		if(state.block !is FlexiTrackBlock) return false
		if(!level.worldBorder.isWithinBounds(pos)) return true
		
		val vb = event.multiBufferSource.getBuffer(RenderType.lines())
		val camPos = event.camera.position
		
		val ms = event.poseStack
		
		ms.pushPose()
		ms.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)
		
		val holdingTrack = AllTags.AllBlockTags.TRACKS.matches(mc.player!!.mainHandItem)
		val shape = FlexiTrackBlock.flexiShape(level, pos)
		val canConnectFrom = !shape.isJunction
			&& !((level.getBlockEntity(pos) as? TrackBlockEntity)?.isTilted ?: false)
		
		walkShapes(
			shape = shape,
			msr = TransformStack.of(ms),
			renderer = { s ->
				TrackBlockOutline.renderShape(s, ms, vb, if(holdingTrack) canConnectFrom else null)
				event.setCanceled(true)
			}
		)
		
		ms.popPose()
		return true
	}
	
	fun walkShapes(shape: FlexiShape, msr: TransformStack<*>, renderer: (VoxelShape) -> Unit) {
		for(axis in shape.axes) {
			msr.pushPose()
			// when normal = (x, y, z),
			// k = (z, 0, -x) / sqrt(1-y^2)
			val normal = axis.normal
			val sin = sqrt(1.0 - normal.y * normal.y)
			msr.rotateCentered(
				atan2(normal.y, sin).toFloat(),
				(normal.z * sin).toFloat(),
				0f,
				(-normal.x * sin).toFloat(),
			)
			renderer(FlexiTrackVoxelShapes.base)
			msr.popPose()
		}
	}
}
