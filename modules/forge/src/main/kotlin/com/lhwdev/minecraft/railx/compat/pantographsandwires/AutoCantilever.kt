package com.lhwdev.minecraft.railx.compat.pantographsandwires

import com.lhwdev.minecraft.railx.common.CommonTrackBlockOutline
import com.lhwdev.minecraft.railx.utils.CompoundTag
import com.lhwdev.minecraft.utils.vectors.minus
import com.lhwdev.minecraft.utils.vectors.plus
import com.lhwdev.minecraft.utils.vectors.times
import com.lhwdev.minecraft.utils.vectors.toVec3
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackBlockOutline
import de.mrjulsen.paw.block.CantileverBlock
import de.mrjulsen.paw.blockentity.CantileverBlockEntity
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt


class AutoCantilever(val height: Float) {
	companion object {
		fun read(tag: CompoundTag): AutoCantilever = AutoCantilever(
			height = tag.getFloat("Height"),
		)
		
		const val MaxYOffset = 1.0
	}
	
	
	fun detectTracks(level: LevelAccessor, origin: Vec3): List<Detected> {
		val tracks = TrackBlockOutline.TRACKS_WITH_TURNS[level]
		val results = mutableListOf<Detected>()
		val distance = height + MaxYOffset + 0.1
		val target = origin.add(0.0, -distance, 0.0)
		
		for(trackBe in tracks.values) {
			var result: CommonTrackBlockOutline.PickCurveResult? = null
			
			CommonTrackBlockOutline.pickCurves(
				connections = trackBe.connections.values,
				origin = origin,
				target = target,
				maxRange = distance + 3.0,
			) { _, pick -> result = pick }
			
			if(result != null) results += Detected(position = result.position, normal = result.normal)
		}
		
		val trackHit = CommonTrackBlockOutline.pickBlock(level, origin, target, maxRange = distance + 3.0)
		if(trackHit != null) {
			val trackPos = trackHit.blockPos
			val trackState = level.getBlockState(trackPos)
			val track = trackState.block as? ITrackBlock
			if(track is ITrackBlock) {
				val normal = track.getUpNormal(level, trackPos, trackState)
				results += Detected(position = Vec3.atBottomCenterOf(trackPos), normal = normal)
			}
		}
		
		return results
	}
	
	fun write(): CompoundTag = CompoundTag { tag ->
		tag.putFloat("Height", height)
	}
	
	
	inner class Detected(val position: Vec3, val normal: Vec3) {
		fun tryCalculate(be: CantileverBlockEntity, connector: CantileverConnectorDataProvider): Calculated? {
			val pos = be.blockPos
			val state = be.blockState
			val block = state.block as? CantileverBlock ?: return null
			
			val target = position + normal * height.toDouble() - Vec3.atLowerCornerOf(pos)
			
			val cantilever = connector.attachOffset.toVec3()
			val yRotation = block.getYRotation(state).toDouble()
			val planeNormal = VecHelper.rotate(Vec3(1.0, 0.0, 0.0), yRotation, Direction.Axis.Y)
			
			// finding foot of perpendicular to plane(normal = cantileverWidth, point = cantilever)
			val distanceToPlane = planeNormal.dot(cantilever - target)
			val targetOnPlane = target + planeNormal * distanceToPlane
			
			if(abs(distanceToPlane) > 0.3f) {
				println("???")
			}
			
			val deltaYOffset = -(targetOnPlane.y - cantilever.y)
			
			val base = block.rotatedPivotPoint(state).add(block.getOffset(state))
			val targetVec2 = Vec2(targetOnPlane.x.toFloat(), targetOnPlane.z.toFloat())
			val newWidth = sqrt(base.distanceToSqr(targetVec2)) - 0.5f
			
			var yOffset = be.yOffset + deltaYOffset.toFloat()
			
			if(abs(yOffset) < 1e-5f)
				yOffset = 0f
			
			// Negative YOffset seems possible
			// if(yOffset < 0f)
			// 	return null
			
			return Calculated(yOffset = yOffset, width = newWidth)
				.also { it.detected = this }
		}
	}
	
	class Calculated(val yOffset: Float, val width: Float) {
		var detected: Detected? = null
		
		fun distanceTo(be: CantileverBlockEntity): Float =
			abs(yOffset - be.yOffset) + abs(width - be.width)
		
		fun write(buffer: FriendlyByteBuf) {
			buffer.writeFloat(yOffset)
			buffer.writeFloat(width)
		}
		
		companion object {
			fun read(buffer: FriendlyByteBuf): Calculated = Calculated(
				yOffset = buffer.readFloat(),
				width = buffer.readFloat(),
			)
		}
		
		fun applyTo(be: CantileverBlockEntity) {
			val nbt = be.saveWithoutMetadata()
			nbt.putFloat(CantileverBlockEntity.NBT_Y_OFFSET, yOffset)
			nbt.putFloat(CantileverBlockEntity.NBT_WIDTH, width)
			
			be.load(nbt)
		}
	}
}
