package com.lhwdev.minecraft.railx.common.rendering

import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.utils.createInstances
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.trains.track.BezierConnection
import com.simibubi.create.foundation.render.SpecialModels
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector
import dev.engine_room.flywheel.api.visual.ShaderLightVisual
import dev.engine_room.flywheel.api.visual.Visual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.lib.instance.InstanceTypes
import dev.engine_room.flywheel.lib.instance.TransformedInstance
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.transform.TransformStack
import it.unimi.dsi.fastutil.longs.LongArraySet
import it.unimi.dsi.fastutil.longs.LongSet
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.core.SectionPos
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.minus
import kotlin.math.max
import kotlin.math.min


abstract class CommonTrackVisual(
	context: VisualizationContext,
	protected val curve: BezierConnection,
) : Visual, ShaderLightVisual {
	protected var lightSections: SectionCollector? = null
	
	protected val instancerProvider = context.instancerProvider()
	protected val visualPosition = curve.from - context.renderOrigin()
	
	
	override fun update(partialTick: Float) {
		lightSections?.sections(collectLightSections())
	}
	
	override fun delete() {}
	
	override fun setSectionCollector(collector: SectionCollector?) {
		lightSections = collector
	}
	
	
	protected open fun collectLightSections(): LongSet {
		var minX = Int.MAX_VALUE
		var minY = Int.MAX_VALUE
		var minZ = Int.MAX_VALUE
		var maxX = Int.MIN_VALUE
		var maxY = Int.MIN_VALUE
		var maxZ = Int.MIN_VALUE
		for(pos in curve.bePositions) {
			minX = min(minX, pos.x - 1)
			minY = min(minY, pos.y - 1)
			minZ = min(minZ, pos.z - 1)
			maxX = max(maxX, pos.x + 1)
			maxY = max(maxY, pos.y + 1)
			maxZ = max(maxZ, pos.z + 1)
		}
		
		val minSectionX = SectionPos.blockToSectionCoord(minX.toDouble())
		val minSectionY = SectionPos.blockToSectionCoord(minY.toDouble())
		val minSectionZ = SectionPos.blockToSectionCoord(minZ.toDouble())
		val maxSectionX = SectionPos.blockToSectionCoord(maxX.toDouble())
		val maxSectionY = SectionPos.blockToSectionCoord(maxY.toDouble())
		val maxSectionZ = SectionPos.blockToSectionCoord(maxZ.toDouble())
		
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
	
	
	open inner class Bezier(curve: BezierConnection) : Visual {
		protected val girder: Girder?
		
		protected val ties: List<TransformedInstance>
		protected val left: List<TransformedInstance>
		protected val right: List<TransformedInstance>
		
		init {
			girder = if(curve.hasGirder) Girder(curve) else null
			
			val pose = PoseStack()
			TransformStack.of(pose)
				.translate(visualPosition)
			
			val segCount = curve.segmentCount
			
			val models = curve.material.modelHolder
			ties = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.tie))
				.createInstances(segCount)
			left = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.leftSegment))
				.createInstances(segCount)
			right = instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(models.rightSegment))
				.createInstances(segCount)
			
			val segment = curve.bakedSegments
			for(i in 1..<segment.length) {
				val modelIndex = i - 1
				
				ties[modelIndex].setTransform(pose)
					.mul(segment.tieTransform[i])
					.setChanged()
				
				for(first in Iterate.trueAndFalse) {
					val transform = segment.railTransforms[i][first]
					(if(first) left else right)[modelIndex].setTransform(pose)
						.mul(transform)
						.setChanged()
				}
			}
		}
		
		override fun update(partialTick: Float) {}
		
		override fun delete() {
			for(d in ties) d.delete()
			for(d in left) d.delete()
			for(d in right) d.delete()
		}
	}
	
	open inner class Girder(curve: BezierConnection) : Visual {
		private val beams: Couple<List<TransformedInstance>>
		private val beamCaps: Couple<Couple<List<TransformedInstance>>>
		
		init {
			val pose = PoseStack()
			TransformStack.of(pose)
				.translate(visualPosition)
				.nudge(curve.from.asLong().toInt())
			
			val segCount = curve.segmentCount
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
			
			val segment = curve.bakedGirders
			for(i in 1..<segment.length) {
				val modelIndex = i - 1
				for(first in Iterate.trueAndFalse) {
					val beamTransform = segment.beams[i][first]
					beams[first][modelIndex]
						.setTransform(pose)
						.mul(beamTransform)
						.setChanged()
					for(top in Iterate.trueAndFalse) {
						val beamCapTransform = segment.beamCaps[i][top][first]
						beamCaps[top][first][modelIndex]
							.setTransform(pose)
							.mul(beamCapTransform)
							.setChanged()
					}
				}
			}
		}
		
		
		override fun update(partialTick: Float) {}
		
		override fun delete() {
			beams.forEach { for(d in it) d.delete() }
			beamCaps.forEach { c ->
				c.forEach {
					for(d in it) d.delete()
				}
			}
		}
	}
}
