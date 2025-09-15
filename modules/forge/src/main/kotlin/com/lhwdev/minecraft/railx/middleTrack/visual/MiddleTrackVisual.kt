package com.lhwdev.minecraft.railx.middleTrack.visual

import com.lhwdev.minecraft.railx.middleTrack.ConnectionMiddleState
import com.lhwdev.minecraft.railx.utils.createInstances
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.render.ContraptionVisual
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.foundation.render.SpecialModels
import dev.engine_room.flywheel.api.instance.Instancer
import dev.engine_room.flywheel.api.visual.LightUpdatedVisual
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector
import dev.engine_room.flywheel.api.visual.ShaderLightVisual
import dev.engine_room.flywheel.api.visual.Visual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.FlatLit
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.transform.TransformStack
import it.unimi.dsi.fastutil.longs.LongArraySet
import it.unimi.dsi.fastutil.longs.LongSet
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LightLayer
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import kotlin.math.max
import kotlin.math.min


class MiddleTrackVisual(
	private val context: VisualizationContext,
	val connection: ConnectionMiddleState,
) : Visual, LightUpdatedVisual, ShaderLightVisual {
	private var lightSections: SectionCollector? = null
	
	private val instancerProvider get() = context.instancerProvider()
	
	private val visualPosition
		get() = connection.from - context.renderOrigin()
	
	private val level get() = connection.level
	
	
	private var visual: BezierTrackVisual? = null
	
	override fun setSectionCollector(sectionCollector: SectionCollector) {
		lightSections = sectionCollector
		sectionCollector.sections(collectLightSections())
	}
	
	override fun update(pt: Float) {
		delete()
		val curve = connection.curve
		visual = if(connection.isActive) {
			BezierTrackVisual(curve)
		} else null
		
		lightSections?.sections(collectLightSections())
	}
	
	override fun updateLight(partialTick: Float) {
		visual?.updateLight()
	}
	
	override fun delete() {
		visual?.delete()
		visual = null
	}
	
	fun collectLightSections(): LongSet {
		var minX = Int.MAX_VALUE
		var minY = Int.MAX_VALUE
		var minZ = Int.MAX_VALUE
		var maxX = Int.MIN_VALUE
		var maxY = Int.MIN_VALUE
		var maxZ = Int.MIN_VALUE
		for(pos in connection.curve.bePositions) {
			minX = min(minX, pos.x)
			minY = min(minY, pos.y)
			minZ = min(minZ, pos.z)
			maxX = max(maxX, pos.x)
			maxY = max(maxY, pos.y)
			maxZ = max(maxZ, pos.z)
		}
		
		val minSectionX = ContraptionVisual.minLightSection(minX.toDouble())
		val minSectionY = ContraptionVisual.minLightSection(minY.toDouble())
		val minSectionZ = ContraptionVisual.minLightSection(minZ.toDouble())
		val maxSectionX = ContraptionVisual.maxLightSection(maxX.toDouble())
		val maxSectionY = ContraptionVisual.maxLightSection(maxY.toDouble())
		val maxSectionZ = ContraptionVisual.maxLightSection(maxZ.toDouble())
		
		val out = LongArraySet()
		
		for(x in minSectionX..maxSectionX) {
			for(y in minSectionY..maxSectionY) {
				for(z in minSectionZ..maxSectionZ) {
					out.add(SectionPos.asLong(x, y, z))
				}
			}
		}
		
		return out
	}
	
	// override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
	// 	visual?.collectCrumblingInstances(consumer)
	// }
	
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
			
			val models = bc.material.modelHolder
			ties = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.tie))
				.createInstances(segCount)
			left = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.leftSegment))
				.createInstances(segCount)
			right = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.rightSegment))
				.createInstances(segCount)
			
			
			val segments = bc.bakedSegments
			for(i in 1..<segments.size) {
				val segment = segments[i]
				val modelIndex = i - 1
				
				ties[modelIndex].setTransform(pose)
					.mul(segment.tieTransform)
					.setChanged()
				
				for(first in Iterate.trueAndFalse) {
					val transform = segment.railTransforms[first]
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
		
		// fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
		// 	for(d in ties) consumer.accept(d)
		// 	for(d in left) consumer.accept(d)
		// 	for(d in right) consumer.accept(d)
		// 	girder?.collectCrumblingInstances(consumer)
		// }
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
				instancerProvider.instancer(
					InstanceTypes.TRANSFORMED,
					Models.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE)
				).createInstances(segCount)
			}
			
			beamCaps = Couple.createWithContext { top ->
				Couple.create {
					instancerProvider.instancer<TransformedInstance>(
						InstanceTypes.TRANSFORMED,
						Models.partial(if(top) AllPartialModels.GIRDER_SEGMENT_TOP else AllPartialModels.GIRDER_SEGMENT_BOTTOM),
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
		
		// fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
		// 	beams.forEach {
		// 		for(d in it) consumer.accept(d)
		// 	}
		// 	beamCaps.forEach { c ->
		// 		c.forEach {
		// 			for(d in it) consumer.accept(d)
		// 		}
		// 	}
		// }
	}
	
	
	private fun updateLight(instance: FlatLit, level: LevelAccessor, pos: BlockPos) {
		instance.light(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
			.setChanged()
	}
}


class TrackInstancers(
	val tie: Instancer<TransformedInstance>,
	val left: Instancer<TransformedInstance>,
	val right: Instancer<TransformedInstance>,
	val beam: Instancer<TransformedInstance>,
	val girderSegmentTop: Instancer<TransformedInstance>,
	val girderSegmentBottom: Instancer<TransformedInstance>,
)
