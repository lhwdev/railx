package com.lhwdev.minecraft.railx.trainMap

import com.lhwdev.minecraft.railx.utils.component1
import com.lhwdev.minecraft.railx.utils.component2
import com.simibubi.create.Create
import com.simibubi.create.CreateClient
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.infrastructure.config.AllConfigs
import com.simibubi.create.infrastructure.config.CClient
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.createmod.catnip.data.Iterate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.util.FastColor
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import com.simibubi.create.compat.trainmap.TrainMapRenderer as CreateMapRenderer


@OnlyIn(Dist.CLIENT)
class TrainMapBlocksRenderer(
	val container: MapWidgetContainer<*>,
	val dimension: ResourceKey<Level>,
	val linearFiltering: Boolean,
) : AbstractWidget(0, 0, 0, 0, Component.literal("train map blocks")) {
	companion object {
		// Background first so we can mindlessly paint over it
		private const val PHASE_BACKGROUND = 0
		
		// Straights before curves so that curves anti-alias properly at the transition
		private const val PHASE_STRAIGHTS = 1
		private const val PHASE_CURVES = 2
		
		private const val outlineColor = 0xFF_000000L.toInt()
		private const val portalFrameColor = 0xFF_4C2D5BL.toInt()
		private const val portalColor = 0xFF_FF7FD6L.toInt()
	}
	
	private val map = CreateMapRenderer.INSTANCE
	
	private val mainColor: Int
	private val darkerColor: Int
	private val darkerColorShadow: Int
	
	private val collisions = ObjectArrayList<MapPos>()
	private var phase = 0
	private var hashCode = 0
	
	init {
		map.trackingVersion = CreateClient.RAILWAYS.version
		map.trackingDim = dimension
		map.trackingTheme = AllConfigs.client().trainMapColorTheme.get()
		
		when(map.trackingTheme) {
			CClient.TrainMapTheme.RED -> {
				mainColor = 0xFF_7C57D4u.toInt()
				darkerColor = 0xFF_70437Du.toInt()
				darkerColorShadow = 0xFF_4A2754u.toInt()
			}
			
			CClient.TrainMapTheme.GREY -> {
				mainColor = 0xFF_A8B5B5u.toInt()
				darkerColor = 0xFF_776E6Cu.toInt()
				darkerColorShadow = 0xFF_56504Eu.toInt()
			}
			
			CClient.TrainMapTheme.WHITE -> {
				mainColor = 0xFF_E8F9F9u.toInt()
				darkerColor = 0xFF_889595u.toInt()
				darkerColorShadow = 0xFF_56504Eu.toInt()
			}
		}
	}
	
	override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		val bufferSource = graphics.bufferSource()
		val pose = graphics.pose()
		
		for((key, instance) in CreateMapRenderer.INSTANCE.maps) {
			if(instance.canBeSkipped(container.bounds)) return
			val (x, y) = key
			pose.pushPose()
			pose.translate((x * CreateMapRenderer.WIDTH).toFloat(), (y * CreateMapRenderer.HEIGHT).toFloat(), 0f)
			instance.draw(pose, bufferSource, linearFiltering)
			pose.popPose()
		}
	}
	
	
	fun isInvalid(): Boolean = map.trackingVersion != Create.RAILWAYS.version ||
		map.trackingDim != Minecraft.getInstance().level?.dimension() ||
		dimension != map.trackingDim ||
		map.trackingTheme != AllConfigs.client().trainMapColorTheme.get()
	
	fun redrawAll() {
		map.startDrawing()
		
		for(phase in 0..2) renderPhase(phase)
		
		highlightYDifferences()
		
		map.finishDrawing()
	}
	
	private fun renderPhase(phase: Int) {
		this.phase = phase
		for(graph in CreateClient.RAILWAYS.trackNetworks.values) {
			for(fromLocation in graph.getNodes()) {
				if(fromLocation.dimension !== map.trackingDim) continue
				val from = graph.locateNode(fromLocation)
				val connectionsFrom = graph.getConnectionsFrom(from)
				
				hashCode = from.hashCode()
				for(edge in connectionsFrom.values) {
					renderEdge(edge)
				}
			}
		}
	}
	
	
	private fun renderEdge(edge: TrackEdge) {
		val from = edge.node1
		val to = edge.node2
		val turn = edge.turn
		
		// Portal track
		if(edge.isInterDimensional) {
			val vec = from.location.location
			val x = Mth.floor(vec.x)
			val z = Mth.floor(vec.z)
			if(phase == PHASE_CURVES) return
			if(phase == PHASE_BACKGROUND) {
				map.setPixels(x - 3, z - 2, x + 3, z + 2, outlineColor)
				map.setPixels(x - 2, z - 3, x + 2, z + 3, outlineColor)
				return
			}
			
			val a = mapYtoAlpha(Mth.floor(vec.y()).toDouble())
			for(xi in x - 2..x + 2) {
				for(zi in z - 2..z + 2) {
					val alphaAt = map.alphaAt(xi, zi)
					if(alphaAt > 0 && alphaAt != a) collisions.add(MapPos(xi, zi))
					val c =
						if((xi - x) * (xi - x) + (zi - z) * (zi - z) > 2) portalFrameColor else portalColor
					if(alphaAt <= a) {
						map.setPixel(xi, zi, markY(c, vec.y()))
					}
				}
			}
			return
		}
		
		if(to.hashCode() > hashCode) return
		
		if(turn == null) {
			if(phase == PHASE_CURVES) return
			
			var x1 = from.location.x.toFloat()
			var z1 = from.location.z.toFloat()
			var x2 = to.location.x.toFloat()
			var z2 = to.location.z.toFloat()
			
			val y1 = from.location.location.y
			val y2 = to.location.location.y
			
			val xDiffSign = sign(x2 - x1)
			val zDiffSign = sign(z2 - z1)
			val diagonal = xDiffSign != 0f && zDiffSign != 0f
			
			if(xDiffSign != 0f) {
				x2 -= (xDiffSign * .25).toFloat()
				x1 += (xDiffSign * .25).toFloat()
			}
			
			if(zDiffSign != 0f) {
				z2 -= (zDiffSign * .25).toFloat()
				z1 += (zDiffSign * .25).toFloat()
			}
			
			x1 /= 2f
			x2 /= 2f
			z1 /= 2f
			z2 /= 2f
			
			var y = Mth.floor(y1)
			val a = mapYtoAlpha(y.toDouble())
			
			// Diagonal
			if(diagonal) {
				var z = Mth.floor(z1)
				var x = Mth.floor(x1)
				
				var s = 0
				while(s <= abs(x1 - x2)) {
					if(phase == PHASE_BACKGROUND) {
						map.setPixels(x - 1, z, x + 1, z + 1, outlineColor)
						map.setPixels(x, z - 1, x, z + 2, outlineColor)
						x = (x + xDiffSign).toInt()
						z = (z + zDiffSign).toInt()
						s++
						continue
					}
					
					val alphaAt = map.alphaAt(x, z)
					if(alphaAt > 0 && alphaAt != a) collisions.add(MapPos(x, z))
					if(alphaAt <= a) {
						map.setPixel(x, z, markY(mainColor, y.toDouble()))
					}
					
					if(map.alphaAt(x, z + 1) < a) {
						map.setPixel(x, z + 1, markY(darkerColor, y.toDouble()))
					}
					
					x = (x + xDiffSign).toInt()
					z = (z + zDiffSign).toInt()
					s++
				}
				
				return
			}
			
			// Straight
			if(phase == PHASE_BACKGROUND) {
				val x1i = Mth.floor(min(x1, x2))
				val z1i = Mth.floor(min(z1, z2))
				val x2i = Mth.floor(max(x1, x2))
				val z2i = Mth.floor(max(z1, z2))
				
				map.setPixels(x1i - 1, z1i, x2i + 1, z2i, outlineColor)
				map.setPixels(x1i, z1i - 1, x2i, z2i + 1, outlineColor)
				return
			}
			
			var z = Mth.floor(z1)
			var x = Mth.floor(x1)
			val diff = max(abs(x1 - x2), abs(z1 - z2))
			val yStep = (y2 - y1) / diff
			
			var s = 0
			while(s <= diff) {
				val alphaAt = map.alphaAt(x, z)
				if(alphaAt > 0 && alphaAt != a) collisions.add(MapPos(x, z))
				if(alphaAt <= a) {
					map.setPixel(x, z, markY(mainColor, y.toDouble()))
				}
				x = (x + xDiffSign).toInt()
				y = (y + yStep).toInt()
				z = (z + zDiffSign).toInt()
				s++
			}
			
			return
		}
		
		if(phase == PHASE_STRAIGHTS) return
		
		val origin = turn.bePositions.first
		val rasterise = turn.rasterise()
		
		for(antialias in Iterate.falseAndTrue) {
			for(offset in rasterise.entries) {
				val xz = offset.key!!
				val x = origin.x + xz.first
				val y = Mth.floor(origin.y + offset.value!! + 0.5)
				val z = origin.z + xz.second
				
				if(phase == PHASE_BACKGROUND) {
					map.setPixels(x - 1, z, x + 1, z, outlineColor)
					map.setPixels(x, z - 1, x, z + 1, outlineColor)
					continue
				}
				
				val a = mapYtoAlpha(y.toDouble())
				
				if(!antialias) {
					val alphaAt = map.alphaAt(x, z)
					if(alphaAt > 0 && alphaAt != a) collisions.add(MapPos(x, z))
					if(alphaAt > a) continue
					
					map.setPixel(x, z, markY(mainColor, y.toDouble()))
					continue
				}
				
				val mainColorBelowLeft =
					map.`is`(x + 1, z + 1, mainColor) && abs(map.alphaAt(x + 1, z + 1) - a) <= 1
				val mainColorBelowRight =
					map.`is`(x - 1, z + 1, mainColor) && abs(map.alphaAt(x - 1, z + 1) - a) <= 1
				
				if(mainColorBelowLeft || mainColorBelowRight) {
					val alphaAt = map.alphaAt(x, z + 1)
					if(alphaAt > 0 && alphaAt != a) collisions.add(MapPos(x, z))
					if(alphaAt >= a) continue
					
					map.setPixel(x, z + 1, markY(darkerColor, y.toDouble()))
					
					// Adjust background
					if(map.isEmpty(x + 1, z + 1)) map.setPixel(x + 1, z + 1, outlineColor)
					if(map.isEmpty(x - 1, z + 1)) map.setPixel(x - 1, z + 1, outlineColor)
					if(map.isEmpty(x, z + 2)) map.setPixel(x, z + 2, outlineColor)
				}
			}
			if(phase == PHASE_BACKGROUND) break
		}
	}
	
	private fun highlightYDifferences() {
		for((x, z) in collisions) {
			val a = map.alphaAt(x, z)
			if(a == 0) continue
			
			for(xi in x - 2..x + 2) {
				for(zi in z - 2..z + 2) {
					if(map.alphaAt(xi, zi) >= a) continue
					if(map.`is`(xi, zi, mainColor)) map.setPixel(xi, zi, FastColor.ABGR32.color(a, darkerColor))
					else if(map.`is`(xi, zi, darkerColor)) map.setPixel(
						xi,
						zi,
						FastColor.ABGR32.color(a, darkerColorShadow)
					)
				}
			}
		}
	}
	
	private fun mapYtoAlpha(y: Double): Int {
		val minY = Minecraft.getInstance().level!!.minBuildHeight
		return Mth.clamp(32 + Mth.floor((y - minY) / 4.0), 0, 255)
	}
	
	private fun markY(color: Int, y: Double): Int {
		return FastColor.ABGR32.color(mapYtoAlpha(y), color)
	}
	
	
	override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
		TODO()
	}
}
