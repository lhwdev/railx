package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.RailXConfig
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import com.simibubi.create.CreateClient
import com.simibubi.create.compat.trainmap.TrainMapRenderer
import com.simibubi.create.compat.trainmap.TrainMapSync
import com.simibubi.create.compat.trainmap.TrainMapSync.TrainState
import com.simibubi.create.compat.trainmap.TrainMapSyncClient
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.infrastructure.config.AllConfigs
import com.simibubi.create.infrastructure.config.CClient.TrainMapTheme
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.Rect2i
import net.minecraft.resources.ResourceKey
import net.minecraft.util.FastColor
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.*


object BuildTrak {
	val enabled: Boolean
		get() = RailXConfig.Server.buildTrak.enabled.asBoolean
	
	// TODO: track plan by item?
	val currentPlan: TrackPlan
		get() = DummyTrackPlan
}


object TrainMapManager {
	@JvmOverloads
	fun tick(dimension: ResourceKey<Level> = Minecraft.getInstance().level!!.dimension()) {
		val map = TrainMapRenderer.INSTANCE
		if(
			map.trackingVersion != CreateClient.RAILWAYS.version ||
			map.trackingDim !== dimension ||
			map.trackingTheme != AllConfigs.client().trainMapColorTheme.get()
		) {
			redrawAll(dimension)
		}
	}
	
	fun renderAndPick(graphics: GuiGraphics, mouseX: Int, mouseY: Int, linearFiltering: Boolean, bounds: Rect2i) {
		var hoveredElement: Any? = null
		
		val offScreenMargin = 32
		bounds.x -= offScreenMargin
		bounds.y -= offScreenMargin
		bounds.width += 2 * offScreenMargin
		bounds.height += 2 * offScreenMargin
		
		TrainMapRenderer.INSTANCE.render(graphics, linearFiltering, bounds)
		hoveredElement = drawTrains(graphics, mouseX, mouseY, hoveredElement, bounds)
		hoveredElement = drawPoints(graphics, mouseX, mouseY, hoveredElement, bounds)
		
		graphics.bufferSource()
			.endBatch()
		
		// if(hoveredElement is GlobalStation) {
		// 	return listOf(Component.literal(hoveredElement.name))
		// }
		//
		// if(hoveredElement is Train) return listTrainDetails(hoveredElement)
	}
	
	/*
	private fun listTrainDetails(train: Train): List<FormattedText> {
		val output = mutableListOf<FormattedText>()
		val blue = 0xD3DEDC
		val darkBlue = 0x92A9BD
		val bright = 0xFFEFEF
		val orange = 0xFFAD60
		
		val trainEntry = TrainMapSyncClient.currentData.get(train.id) ?: return mutableListOf<FormattedText?>()
		val state = trainEntry.state
		val signalState = trainEntry.signalState
		
		CreateLang.text(train.name.string)
			.color(bright)
			.addTo(output)
		
		if(!trainEntry.ownerName.isBlank()) CreateLang.translate("train_map.train_owned_by", trainEntry.ownerName)
			.color(blue)
			.addTo(output)
		
		when(state) {
			TrainState.CONDUCTOR_MISSING -> {
				CreateLang.translate("train_map.conductor_missing")
					.color(orange)
					.addTo(output)
				return output
			}
			
			TrainState.DERAILED -> {
				CreateLang.translate("train_map.derailed")
					.color(orange)
					.addTo(output)
				return output
			}
			
			TrainState.NAVIGATION_FAILED -> {
				CreateLang.translate("train_map.navigation_failed")
					.color(orange)
					.addTo(output)
				return output
			}
			
			TrainState.SCHEDULE_INTERRUPTED -> {
				CreateLang.translate("train_map.schedule_interrupted")
					.color(orange)
					.addTo(output)
				return output
			}
			
			TrainState.RUNNING_MANUALLY -> CreateLang.translate("train_map.player_controlled")
				.color(blue)
				.addTo(output)
			
			TrainState.RUNNING -> {}
			else -> {}
		}
		
		val currentStation = trainEntry.targetStationName
		val targetStationDistance = trainEntry.targetStationDistance
		
		if(!currentStation.isBlank()) {
			if(targetStationDistance == 0) CreateLang.translate("train_map.train_at_station", currentStation)
				.color(darkBlue)
				.addTo(output)
			else CreateLang.translate("train_map.train_moving_to_station", currentStation, targetStationDistance)
				.color(darkBlue)
				.addTo(output)
		}
		
		if(signalState != TrainMapSync.SignalState.NOT_WAITING) {
			val chainSignal = signalState == TrainMapSync.SignalState.CHAIN_SIGNAL
			CreateLang.translate("train_map.waiting_at_signal")
				.color(orange)
				.addTo(output)
			
			if(signalState == TrainMapSync.SignalState.WAITING_FOR_REDSTONE) CreateLang.translate("train_map.redstone_powered")
				.color(blue)
				.addTo(output)
			else {
				val waitingFor = trainEntry.waitingForTrain
				var trainFound = false
				
				if(waitingFor != null) {
					val trainWaitingFor = CreateClient.RAILWAYS.trains.get(waitingFor)
					if(trainWaitingFor != null) {
						CreateLang.translate("train_map.for_other_train", trainWaitingFor.name.string)
							.color(blue)
							.addTo(output)
						trainFound = true
					}
				}
				
				
				if(!trainFound) {
					if(chainSignal) CreateLang.translate("train_map.cannot_traverse_section")
						.color(blue)
						.addTo(output)
					else CreateLang.translate("train_map.section_reserved")
						.color(blue)
						.addTo(output)
				}
			}
		}
		
		if(trainEntry.fueled) CreateLang.translate("train_map.fuel_boosted")
			.color(darkBlue)
			.addTo(output)
		
		return output
	}
*/
	
	private fun drawPoints(
		graphics: GuiGraphics,
		mouseX: Int,
		mouseY: Int,
		hoveredElement: Any?,
		bounds: Rect2i,
	): Any? {
		var hoveredElement = hoveredElement
		val pose = graphics.pose()
		RenderSystem.enableDepthTest()
		
		for(graph in CreateClient.RAILWAYS.trackNetworks.values) {
			for(station in graph.getPoints(EdgePointType.STATION)) {
				val edgeLocation = station.edgeLocation
				val node = graph.locateNode(edgeLocation.first)
				val other = graph.locateNode(edgeLocation.second)
				if(node == null || other == null) continue
				if(node.location.dimension !== TrainMapRenderer.INSTANCE.trackingDim) continue
				
				val edge = graph.getConnection(Couple.create(node, other)) ?: continue
				
				val tLength = station.getLocationOn(edge)
				val t = tLength / edge.length
				val position = edge.getPosition(graph, t)
				
				val x = Mth.floor(position.x())
				val y = Mth.floor(position.z())
				
				if(!bounds.contains(x, y)) continue
				
				val diff = edge.getDirectionAt(tLength).normalize()
				val rotation = Mth.positiveModulo(
					Mth.floor(
						0.5 + (atan2(diff.z, diff.x) * Mth.RAD_TO_DEG + 90 +
							(if(station.isPrimary(node)) 180 else 0)) / 45
					),
					8
				)
				
				var sprite = AllGuiTextures.TRAINMAP_STATION_ORTHO
				var highlightSprite = AllGuiTextures.TRAINMAP_STATION_ORTHO_HIGHLIGHT
				if(rotation % 2 != 0) {
					sprite = AllGuiTextures.TRAINMAP_STATION_DIAGO
					highlightSprite = AllGuiTextures.TRAINMAP_STATION_DIAGO_HIGHLIGHT
				}
				
				val highlight = hoveredElement == null && max(abs(mouseX - x), abs(mouseY - y)) < 3
				
				pose.pushPose()
				pose.translate((x - 2).toFloat(), (y - 2).toFloat(), 5f)
				
				pose.translate(sprite.width / 2.0, sprite.height / 2.0, 0.0)
				pose.mulPose(Axis.ZP.rotationDegrees((90 * (rotation / 2)).toFloat()))
				pose.translate(-sprite.width / 2.0, -sprite.height / 2.0, 0.0)
				
				sprite.render(graphics, 0, 0)
				sprite.render(graphics, 0, 0)
				
				if(highlight) {
					pose.translate(0f, 0f, 5f)
					highlightSprite.render(graphics, -1, -1)
					hoveredElement = station
				}
				
				pose.popPose()
			}
		}
		
		return hoveredElement
	}
	
	private fun drawTrains(
		graphics: GuiGraphics,
		mouseX: Int,
		mouseY: Int,
		hoveredElement: Any?,
		bounds: Rect2i,
	): Any? {
		var hoveredElement = hoveredElement
		val pose = graphics.pose()
		RenderSystem.enableDepthTest()
		RenderSystem.enableBlend()
		
		val spriteYOffset = -3
		
		var time = AnimationTickHolder.getTicks().toDouble()
		time += AnimationTickHolder.getPartialTicks().toDouble()
		time -= TrainMapSyncClient.lastPacket
		time /= TrainMapSync.lightPacketInterval.toDouble()
		time = Mth.clamp(time, 0.0, 1.0)
		
		val sliceXShiftByRotationIndex = intArrayOf(0, 1, 2, 2, 3, -2, -2, -1)
		val sliceYShiftByRotationIndex = intArrayOf(3, 2, 2, 1, 0, 1, 2, 2)
		
		for(train in CreateClient.RAILWAYS.trains.values) {
			val trainEntry = TrainMapSyncClient.currentData[train.id] ?: continue
			
			var frontPos = Vec3.ZERO
			val carriages = train.carriages
			var otherDim = true
			var avgY = 0.0
			
			for(i in carriages.indices) {
				for(firstBogey in Iterate.trueAndFalse)
					avgY += trainEntry.getPosition(i, firstBogey, time).y
			}
			
			avgY /= (carriages.size * 2).toDouble()
			
			for(i in carriages.indices) {
				val carriage = carriages[i]
				
				val pos1 = trainEntry.getPosition(i, true, time)
				val pos2 = trainEntry.getPosition(i, false, time)
				
				val dim = trainEntry.dimensions[i]
				if(dim == null || dim !== TrainMapRenderer.INSTANCE.trackingDim) continue
				if(
					!bounds.contains(Mth.floor(pos1.x()), Mth.floor(pos1.z())) &&
					!bounds.contains(Mth.floor(pos2.x()), Mth.floor(pos2.z()))
				) continue
				
				otherDim = false
				
				if(!trainEntry.backwards && i == 0) frontPos = pos1
				if(trainEntry.backwards && i == train.carriages.size - 1) frontPos = pos2
				
				val diff = pos2.subtract(pos1)
				val size = carriage.bogeySpacing + 1
				val center = pos1.add(pos2).scale(0.5)
				
				val pX = center.x
				val pY = center.z
				var rotation =
					Mth.positiveModulo(Mth.floor(0.5 + (atan2(diff.x, diff.z) * Mth.RAD_TO_DEG) / 22.5), 8)
				
				if(trainEntry.state == TrainState.DERAILED) rotation = Mth.positiveModulo(
					(AnimationTickHolder.getTicks() / 8 + i * 3) * (if(i % 2 == 0) 1 else -1),
					8
				)
				
				val sprite = AllGuiTextures.TRAINMAP_SPRITES
				
				var slices = 2
				
				if(rotation == 0 || rotation == 4) {
					// Orthogonal, slices add 3 pixels
					slices += Mth.floor((size - 2) / (3.0) + 0.5)
				} else if(rotation == 2 || rotation == 6) {
					// Diagonal, slices add 2*sqrt(2) pixels
					slices += Mth.floor((size - (5 - 2 * Mth.SQRT_OF_TWO)) / (2 * Mth.SQRT_OF_TWO) + 0.5)
				} else {
					// Slanty, slices add sqrt(5) pixels
					slices += Mth.floor((size - (5 - Mth.sqrt(5f))) / (Mth.sqrt(5f)) + 0.5)
				}
				
				slices = max(2, slices)
				
				sprite.bind()
				pose.pushPose()
				
				val pivotX = 7.5f + (slices - 3) * sliceXShiftByRotationIndex[rotation] / 2.0f
				val pivotY = 6.5f + (slices - 3) * sliceYShiftByRotationIndex[rotation] / 2.0f
				// Ysort at home
				pose.translate(pX - pivotX, pY - pivotY, 10 + (avgY / 512.0) + (1024.0 + center.z() % 8192.0) / 1024.0)
				
				val trainColorIndex = train.mapColorIndex
				val colorRow = trainColorIndex / 4
				val colorCol = trainColorIndex % 4
				
				for(slice in 0..<slices) {
					val row = if(slice == 0) 1 else if(slice == slices - 1) 2 else 3
					val sliceShifts = if(slice == 0) 0 else if(slice == slices - 1) slice - 2 else slice - 1
					val col = rotation
					
					val positionX = sliceShifts * sliceXShiftByRotationIndex[rotation]
					val positionY = sliceShifts * sliceYShiftByRotationIndex[rotation] + spriteYOffset
					val sheetX = col * 16 + colorCol * 128
					val sheetY = row * 16 + colorRow * 64
					
					graphics.blit(
						sprite.location,
						positionX,
						positionY,
						sheetX.toFloat(),
						sheetY.toFloat(),
						16,
						16,
						sprite.width,
						sprite.height
					)
				}
				
				pose.popPose()
				
				val margin = 1
				val sizeX = 8 + (slices - 3) * sliceXShiftByRotationIndex[rotation]
				val sizeY = 12 + (slices - 3) * sliceYShiftByRotationIndex[rotation]
				val pXm = pX - sizeX / 2
				val pYm = pY - sizeY / 2 + spriteYOffset
				if(
					hoveredElement == null &&
					mouseX < pXm + margin + sizeX &&
					mouseX > pXm - margin &&
					mouseY < pYm + margin + sizeY &&
					mouseY > pYm - margin
				) {
					hoveredElement = train
				}
			}
			
			if(otherDim) continue
			
			if(trainEntry.signalState != TrainMapSync.SignalState.NOT_WAITING) {
				pose.pushPose()
				pose.translate(frontPos.x - 0.5, frontPos.z - 0.5, 20 + (1024.0 + frontPos.z() % 8192.0) / 1024.0)
				AllGuiTextures.TRAINMAP_SIGNAL.render(graphics, 0, -3)
				pose.popPose()
			}
		}
		
		return hoveredElement
	}
	
	// Background first so we can mindlessly paint over it
	const val PHASE_BACKGROUND = 0
	
	// Straights before curves so that curves anti-alias properly at the transition
	const val PHASE_STRAIGHTS = 1
	const val PHASE_CURVES = 2
	
	fun redrawAll(dimension: ResourceKey<Level>) {
		val map = TrainMapRenderer.INSTANCE
		map.trackingVersion = CreateClient.RAILWAYS.version
		map.trackingDim = dimension
		map.trackingTheme = AllConfigs.client().trainMapColorTheme.get()
		map.startDrawing()
		
		var mainColor = 0xFF_7C57D4u.toInt()
		var darkerColor = 0xFF_70437Du.toInt()
		var darkerColorShadow = 0xFF_4A2754u.toInt()
		
		when(map.trackingTheme) {
			TrainMapTheme.GREY -> {
				mainColor = 0xFF_A8B5B5u.toInt()
				darkerColor = 0xFF_776E6Cu.toInt()
				darkerColorShadow = 0xFF_56504Eu.toInt()
			}
			
			TrainMapTheme.WHITE -> {
				mainColor = 0xFF_E8F9F9u.toInt()
				darkerColor = 0xFF_889595u.toInt()
				darkerColorShadow = 0xFF_56504Eu.toInt()
			}
			
			else -> {}
		}
		
		val collisions = ObjectArrayList<Couple<Int>>()
		
		for(phase in 0..2) renderPhase(map, collisions, mainColor, darkerColor, phase)
		
		highlightYDifferences(map, collisions, mainColor, darkerColor, darkerColor, darkerColorShadow)
		
		map.finishDrawing()
	}
	
	private fun renderPhase(
		map: TrainMapRenderer, collisions: MutableList<Couple<Int>>, mainColor: Int,
		darkerColor: Int, phase: Int,
	) {
		val outlineColor = 0xFF_000000u.toInt()
		
		val portalFrameColor = 0xFF_4C2D5Bu.toInt()
		val portalColor = 0xFF_FF7FD6u.toInt()
		
		for(graph in CreateClient.RAILWAYS.trackNetworks.values) {
			for(nodeLocation in graph.getNodes()) {
				if(nodeLocation.dimension !== map.trackingDim) continue
				val node = graph.locateNode(nodeLocation)
				val connectionsFrom = graph.getConnectionsFrom(node)
				
				val hashCode = node.hashCode()
				for(entry in connectionsFrom.entries) {
					val other = entry.key!!
					val otherLocation = other.location
					val edge = entry.value!!
					val turn = edge.turn
					
					// Portal track
					if(edge.isInterDimensional) {
						val vec = node.location.location
						val x = Mth.floor(vec.x)
						val z = Mth.floor(vec.z)
						if(phase == PHASE_CURVES) continue
						if(phase == PHASE_BACKGROUND) {
							map.setPixels(x - 3, z - 2, x + 3, z + 2, outlineColor)
							map.setPixels(x - 2, z - 3, x + 2, z + 3, outlineColor)
							continue
						}
						
						val a = mapYtoAlpha(Mth.floor(vec.y()).toDouble())
						for(xi in x - 2..x + 2) {
							for(zi in z - 2..z + 2) {
								val alphaAt = map.alphaAt(xi, zi)
								if(alphaAt > 0 && alphaAt != a) collisions.add(Couple.create(xi, zi))
								val c =
									if((xi - x) * (xi - x) + (zi - z) * (zi - z) > 2) portalFrameColor else portalColor
								if(alphaAt <= a) {
									map.setPixel(xi, zi, markY(c, vec.y()))
								}
							}
						}
						continue
					}
					
					if(other.hashCode() > hashCode) continue
					
					if(turn == null) {
						if(phase == PHASE_CURVES) continue
						
						var x1 = nodeLocation.x.toFloat()
						var z1 = nodeLocation.z.toFloat()
						var x2 = otherLocation.x.toFloat()
						var z2 = otherLocation.z.toFloat()
						
						val y1 = nodeLocation.location.y
						val y2 = otherLocation.location.y
						
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
								if(alphaAt > 0 && alphaAt != a) collisions.add(Couple.create(x, z))
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
							
							continue
						}
						
						// Straight
						if(phase == PHASE_BACKGROUND) {
							val x1i = Mth.floor(min(x1, x2))
							val z1i = Mth.floor(min(z1, z2))
							val x2i = Mth.floor(max(x1, x2))
							val z2i = Mth.floor(max(z1, z2))
							
							map.setPixels(x1i - 1, z1i, x2i + 1, z2i, outlineColor)
							map.setPixels(x1i, z1i - 1, x2i, z2i + 1, outlineColor)
							continue
						}
						
						var z = Mth.floor(z1)
						var x = Mth.floor(x1)
						val diff = max(abs(x1 - x2), abs(z1 - z2))
						val yStep = (y2 - y1) / diff
						
						var s = 0
						while(s <= diff) {
							val alphaAt = map.alphaAt(x, z)
							if(alphaAt > 0 && alphaAt != a) collisions.add(Couple.create(x, z))
							if(alphaAt <= a) {
								map.setPixel(x, z, markY(mainColor, y.toDouble()))
							}
							x = (x + xDiffSign).toInt()
							y = (y + yStep).toInt()
							z = (z + zDiffSign).toInt()
							s++
						}
						
						continue
					}
					
					if(phase == PHASE_STRAIGHTS) continue
					
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
								if(alphaAt > 0 && alphaAt != a) collisions.add(Couple.create(x, z))
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
								if(alphaAt > 0 && alphaAt != a) collisions.add(Couple.create(x, z))
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
			}
		}
	}
	
	private fun highlightYDifferences(
		map: TrainMapRenderer, collisions: MutableList<Couple<Int>>, mainColor: Int,
		darkerColor: Int, mainColorShadow: Int, darkerColorShadow: Int,
	) {
		for(couple in collisions) {
			val x = couple.first
			val z = couple.second
			val a = map.alphaAt(x, z)
			if(a == 0) continue
			
			for(xi in x - 2..x + 2) {
				for(zi in z - 2..z + 2) {
					if(map.alphaAt(xi, zi) >= a) continue
					if(map.`is`(xi, zi, mainColor)) map.setPixel(xi, zi, FastColor.ABGR32.color(a, mainColorShadow))
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
}
