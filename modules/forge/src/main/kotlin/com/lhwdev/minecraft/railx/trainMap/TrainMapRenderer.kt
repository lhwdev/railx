package com.lhwdev.minecraft.railx.trainMap

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import com.simibubi.create.CreateClient
import com.simibubi.create.compat.trainmap.TrainMapSync
import com.simibubi.create.compat.trainmap.TrainMapSync.TrainState
import com.simibubi.create.compat.trainmap.TrainMapSyncClient
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.station.GlobalStation
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.RemovedGuiUtils
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.data.Couple
import net.createmod.catnip.data.Iterate
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import xaero.map.gui.GuiMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import com.simibubi.create.compat.trainmap.TrainMapRenderer as CreateTrainMapRenderer


private const val offScreenMargin = 32


@OnlyIn(Dist.CLIENT)
class TrainMapRenderer(val screen: GuiMap) {
	var bounds: Rect2i = Rect2i(0, 0, 0, 0)
	
	private var hoveredElement: Any? = null
	private var mouseX = 0
	private var mouseY = 0
	
	private fun calculateBounds(bounds: Rect2i) = Rect2i(
		bounds.x - offScreenMargin,
		bounds.y - offScreenMargin,
		bounds.width + 2 * offScreenMargin,
		bounds.height + 2 * offScreenMargin
	)
	
	fun updateState(bounds: Rect2i) {
		this.bounds = calculateBounds(bounds)
	}
	
	
	fun renderAndPick(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
		this.mouseX = mouseX
		this.mouseY = mouseY
		hoveredElement = null
		
		drawPoints(graphics)
		drawTrains(graphics)
		
		when(val hovered = hoveredElement) {
			null -> {}
			is GlobalStation -> showTooltip(graphics, listOf(Component.literal(hovered.name)))
			is Train -> showTooltip(graphics, listTrainDetails(hovered))
		}
	}
	
	
	fun showTooltip(graphics: GuiGraphics, lines: List<FormattedText>) {
		val font = Minecraft.getInstance().font
		RemovedGuiUtils.drawHoveringText(graphics, lines, mouseX, mouseY, screen.width, screen.height, 256, font)
	}
	
	private fun listTrainDetails(train: Train): List<FormattedText> {
		val output = mutableListOf<FormattedText>()
		val blue = 0xD3DEDC
		val darkBlue = 0x92A9BD
		val bright = 0xFFEFEF
		val orange = 0xFFAD60
		
		val trainEntry = TrainMapSyncClient.currentData[train.id] ?: return emptyList()
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
	
	
	fun drawPoints(graphics: GuiGraphics) {
		val pose = graphics.pose()
		RenderSystem.enableDepthTest()
		
		for(graph in CreateClient.RAILWAYS.trackNetworks.values) {
			for(station in graph.getPoints(EdgePointType.STATION)) {
				val edgeLocation = station.edgeLocation
				val node = graph.locateNode(edgeLocation.first)
				val other = graph.locateNode(edgeLocation.second)
				if(node == null || other == null) continue
				if(node.location.dimension !== CreateTrainMapRenderer.INSTANCE.trackingDim) continue
				
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
	}
	
	private fun drawTrains(graphics: GuiGraphics) {
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
				if(dim == null || dim !== CreateTrainMapRenderer.INSTANCE.trackingDim) continue
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
	}
}
