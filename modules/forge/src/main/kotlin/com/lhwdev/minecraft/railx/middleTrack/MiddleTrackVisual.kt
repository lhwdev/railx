package com.lhwdev.minecraft.railx.middleTrack

import com.lhwdev.minecraft.railx.utils.createInstances
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.render.ContraptionVisual
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.foundation.render.SpecialModels
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual
import dev.engine_room.flywheel.api.visual.ShaderLightVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.FlatLit
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.transform.TransformStack
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual
import it.unimi.dsi.fastutil.longs.LongArraySet
import it.unimi.dsi.fastutil.longs.LongSet
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min


class MiddleTrackVisual(context: VisualizationContext, middle: MiddleTrackBlockEntity, partialTick: Float) :
	AbstractBlockEntityVisual<MiddleTrackBlockEntity>(context, middle, partialTick), ShaderLightVisual {
	
	val connections: List<BezierConnection>
		get() = blockEntity.connections
	
	private var visuals: List<BezierTrackVisual> = emptyList()
	
	
	override fun setSectionCollector(sectionCollector: SectionTrackedVisual.SectionCollector?) {
		super.setSectionCollector(sectionCollector)
		lightSections.sections(collectLightSections())
	}
	
	override fun update(pt: Float) {
		_delete()
		visuals = connections.mapNotNull {
			if(it.primary) {
				BezierTrackVisual(it)
			} else null
		}
		
		lightSections.sections(collectLightSections())
	}
	
	override fun updateLight(partialTick: Float) {
		visuals.forEach { it.updateLight() }
	}
	
	public override fun _delete() {
		visuals.forEach { it.delete() }
		visuals = emptyList()
	}
	
	fun collectLightSections(): LongSet {
		var minX = Int.MAX_VALUE
		var minY = Int.MAX_VALUE
		var minZ = Int.MAX_VALUE
		var maxX = Int.MIN_VALUE
		var maxY = Int.MIN_VALUE
		var maxZ = Int.MIN_VALUE
		for(connection in connections) {
			for(pos in connection.bePositions) {
				minX = min(minX, pos.x)
				minY = min(minY, pos.y)
				minZ = min(minZ, pos.z)
				maxX = max(maxX, pos.x)
				maxY = max(maxY, pos.y)
				maxZ = max(maxZ, pos.z)
			}
		}
		
		val minSectionX = ContraptionVisual.minLightSection(minX.toDouble())
		val minSectionY = ContraptionVisual.minLightSection(minY.toDouble())
		val minSectionZ = ContraptionVisual.minLightSection(minZ.toDouble())
		val maxSectionX = ContraptionVisual.maxLightSection(maxX.toDouble())
		val maxSectionY = ContraptionVisual.maxLightSection(maxY.toDouble())
		val maxSectionZ = ContraptionVisual.maxLightSection(maxZ.toDouble())
		
		val out: LongSet = LongArraySet()
		
		for(x in minSectionX..maxSectionX) {
			for(y in minSectionY..maxSectionY) {
				for(z in minSectionZ..maxSectionZ) {
					out.add(SectionPos.asLong(x, y, z))
				}
			}
		}
		
		return out
	}
	
	override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
		visuals.forEach { it.collectCrumblingInstances(consumer) }
	}
	
	private inner class BezierTrackVisual(bc: BezierConnection) {
		private val ties: List<TransformedInstance>
		private val left: List<TransformedInstance>
		private val right: List<TransformedInstance>
		
		private val girder = if(bc.hasGirder) GirderVisual(bc) else null
		
		init {
			val pose = PoseStack()
			TransformStack.of(pose)
				.translate(visualPosition)
			
			val segCount = bc.segmentCount
			val modelHolder = bc.material.modelHolder
			
			ties = instancerProvider().instancer(
				InstanceTypes.TRANSFORMED,
				SpecialModels.flatChunk(modelHolder.tie())
			).createInstances(segCount)
			
			left = instancerProvider().instancer(
				InstanceTypes.TRANSFORMED,
				SpecialModels.flatChunk(modelHolder.leftSegment())
			).createInstances(segCount)
			
			right = instancerProvider().instancer(
				InstanceTypes.TRANSFORMED,
				SpecialModels.flatChunk(modelHolder.rightSegment())
			).createInstances(segCount)
			
			
			val segments = bc.getBakedSegments()
			for(i in 1..<segments.size) {
				val segment = segments[i]
				val modelIndex = i - 1
				
				ties[modelIndex].setTransform(pose)
					.mul(segment.tieTransform)
					.setChanged()
				
				for(first in Iterate.trueAndFalse) {
					val transform = segment.railTransforms.get(first)
					(if(first) this.left else this.right)[modelIndex].setTransform(pose)
						.mul(transform)
						.setChanged()
				}
			}
			
			updateLight()
		}
		
		fun delete() {
			for(d in ties) d.delete()
			for(d in left) d.delete()
			for(d in right) d.delete()
			girder?.delete()
		}
		
		fun updateLight() {
			// Light for ties/rails handled by shader light since they tend to clip into blocks
			girder?.updateLight()
		}
		
		fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
			for(d in ties) consumer.accept(d)
			for(d in left) consumer.accept(d)
			for(d in right) consumer.accept(d)
			girder?.collectCrumblingInstances(consumer)
		}
	}
	
	private inner class GirderVisual(bc: BezierConnection) {
		private val beams: Couple<List<TransformedInstance>>
		private val beamCaps: Couple<Couple<List<TransformedInstance>>>
		private val lightPos: List<BlockPos>
		
		init {
			val tePosition = bc.bePositions.first
			val pose = PoseStack()
			TransformStack.of(pose)
				.translate(visualPosition)
				.nudge(bc.bePositions.first.asLong().toInt())
			
			val segCount = bc.segmentCount
			beams = Couple.create {
				instancerProvider().instancer(
					InstanceTypes.TRANSFORMED,
					Models.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE)
				).createInstances(segCount)
			}
			
			beamCaps = Couple.createWithContext { top ->
				val partialModel = Models.partial(
					if(top) AllPartialModels.GIRDER_SEGMENT_TOP else AllPartialModels.GIRDER_SEGMENT_BOTTOM
				)
				Couple.create {
					instancerProvider().instancer(
						InstanceTypes.TRANSFORMED,
						partialModel
					).createInstances(segCount)
				}
			}
			
			val lightPos = mutableListOf<BlockPos>()
			
			val bakedGirders = bc.bakedGirders
			for(i in 1..<bakedGirders.size) {
				val segment = bakedGirders[i]
				val modelIndex = i - 1
				lightPos += segment.lightPosition.offset(tePosition)
				
				for(first in Iterate.trueAndFalse) {
					val beamTransform = segment.beams.get(first)
					beams.get(first)[modelIndex].setTransform(pose)
						.mul(beamTransform)
						.setChanged()
					for(top in Iterate.trueAndFalse) {
						val beamCapTransform = segment.beamCaps.get(top).get(first)
						beamCaps.get(top)
							.get(first)[modelIndex].setTransform(pose)
							.mul(beamCapTransform)
							.setChanged()
					}
				}
			}
			this.lightPos = lightPos
			
			updateLight()
		}
		
		fun delete() {
			beams.forEach { for(d in it) d.delete() }
			beamCaps.forEach { c ->
				c.forEach {
					for(d in it) d.delete()
				}
			}
		}
		
		fun updateLight() {
			beams.forEach {
				for(i in it.indices) updateLight(it[i], level, lightPos[i])
			}
			beamCaps.forEach { c ->
				c!!.forEach {
					for(i in it.indices) updateLight(it[i], level, lightPos[i])
				}
			}
		}
		
		fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
			beams.forEach {
				for(d in it) consumer.accept(d)
			}
			beamCaps.forEach { c ->
				c.forEach {
					for(d in it) consumer.accept(d)
				}
			}
		}
	}
	
	
	private fun updateLight(instance: FlatLit, level: Level, pos: BlockPos) {
		instance.light(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
			.setChanged()
	}
}
