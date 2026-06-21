package com.lhwdev.minecraft.railx.compat.pantographsandwires

import com.lhwdev.minecraft.utils.vectors.plus
import com.lhwdev.minecraft.utils.vectors.toVec3
import de.mrjulsen.paw.block.CantileverBlock
import de.mrjulsen.paw.blockentity.CantileverBlockEntity
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider
import de.mrjulsen.wires.item.CustomData
import net.createmod.catnip.outliner.Outliner
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.PacketDistributor

object AutoCantileverApplier {
	// after getNbt(); rootNbt contains all necessary information like Width, Height
	fun modifyBlockEntityData(be: CantileverBlockEntity, autoCantilever: AutoCantilever) {
		val level = be.level ?: return
		val pos = be.blockPos
		val state = be.blockState
		val block = state.block as? CantileverBlock ?: return
		
		val connector = block.getConnectorData(level, pos, CustomData(CompoundTag()), 0)
			as CantileverConnectorDataProvider
		
		val point = connector.attachOffset.toVec3()
		
		val origin = Vec3.atLowerCornerOf(pos) + point
		val detectedList = autoCantilever.detectTracks(level = level, origin = origin)
		
		val calculated = detectedList.mapNotNull { it.tryCalculate(be, connector) }
		val distances = calculated.map { it.distanceTo(be) }
		
		val single = if(distances.all { it > 0.002 }) {
			calculated.firstOrNull()
		} else {
			calculated[(distances.indexOf(distances.min()) + 1) % calculated.size]
		} ?: return
		
		single.detected?.let { detected ->
			Outliner.getInstance().showAABB(
				"AutoCantileverApplier_target",
				AABB(detected.position, detected.position + detected.normal),
				100,
			).lineWidth(0.3f)
		}
		
		Minecraft.getInstance().player?.sendSystemMessage(
			Component.literal(
				"Cantilever previous: width=${be.width} yOffset=${be.yOffset} " +
					"new: width=${single.width} yOffset=${single.yOffset}"
			)
		)
		PacketDistributor.sendToServer(UpdateCantileverAutoPacket(be.blockPos, single))
	}
}
