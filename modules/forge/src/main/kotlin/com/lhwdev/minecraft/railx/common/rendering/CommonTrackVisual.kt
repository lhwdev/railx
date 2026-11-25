package com.lhwdev.minecraft.railx.common.rendering

import com.lhwdev.minecraft.railx.common.from
import com.lhwdev.minecraft.railx.utils.createInstances
import com.lhwdev.minecraft.utils.vectors.minus
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
import dev.engine_room.flywheel.lib.transform.TransformStack
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.core.SectionPos
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth


abstract class CommonTrackVisual(context: VisualizationContext) : Visual, ShaderLightVisual {
	protected val instancerProvider = context.instancerProvider()
	protected val renderOrigin = context.renderOrigin()
	
	protected var lightSections: SectionCollector? = null
	
	
	protected abstract val beziers: Collection<Bezier>
	
	
	override fun update(partialTick: Float) {
		update()
	}
	
	fun update() {
		updateLightSections()
	}
	
	protected fun updateLightSections() {
		lightSections?.sections(collectLightSections())
	}
	
	override fun delete() {
		for(bezier in beziers) bezier.delete()
	}
	
	
	override fun setSectionCollector(collector: SectionCollector?) {
		lightSections = collector
		updateLightSections()
	}
	
	protected open fun collectLightSections(): LongSet {
		val beziers = beziers
		if(beziers.isEmpty()) return LongSet.of()
		
		val out = LongOpenHashSet()
		for(bezier in beziers) addLightSections(bezier.curve, out)
		return out
	}
	
	protected open fun addLightSections(curve: BezierConnection, out: LongSet) {
		val bounds = curve.bounds
		val minSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX) - 1)
		val minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY) - 1)
		val minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ) - 1)
		val maxSectionX = SectionPos.blockToSectionCoord(Mth.ceil(bounds.maxX) + 1)
		val maxSectionY = SectionPos.blockToSectionCoord(Mth.ceil(bounds.maxY) + 1)
		val maxSectionZ = SectionPos.blockToSectionCoord(Mth.ceil(bounds.maxZ) + 1)
		
		for(x in minSectionX..maxSectionX) {
			for(y in minSectionY..maxSectionY) {
				for(z in minSectionZ..maxSectionZ) {
					out.add(SectionPos.asLong(x, y, z))
				}
			}
		}
	}
	
	
	open inner class Bezier(val curve: BezierConnection) : Visual {
		protected val girder: Girder?
		
		protected val ties: List<TransformedInstance>
		protected val left: List<TransformedInstance>
		protected val right: List<TransformedInstance>
		
		init {
			val visualPosition = curve.from - renderOrigin
			girder = if(curve.hasGirder) Girder(curve, visualPosition) else null
			
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
			girder?.delete()
		}
	}
	
	open inner class Girder(curve: BezierConnection, visualPosition: Vec3i) : Visual {
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
					SpecialModels.flatChunk(AllPartialModels.GIRDER_SEGMENT_MIDDLE)
				).createInstances(segCount)
			}
			
			beamCaps = Couple.createWithContext { top ->
				Couple.create {
					instancerProvider.instancer(
						InstanceTypes.TRANSFORMED,
						SpecialModels.flatChunk(if(top) AllPartialModels.GIRDER_SEGMENT_TOP else AllPartialModels.GIRDER_SEGMENT_BOTTOM),
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
