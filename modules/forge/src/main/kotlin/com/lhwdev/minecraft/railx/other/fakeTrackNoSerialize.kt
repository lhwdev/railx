@file:Suppress("unused")

package com.lhwdev.minecraft.railx.other

import com.lhwdev.minecraft.railx.RailX
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.level.ChunkDataEvent


@EventBusSubscriber(modid = RailX.Id)
internal object FakeTrackNoSerialize {
	@SubscribeEvent
	private fun onSaveChunkData(event: ChunkDataEvent.Save) {
		val blockEntities = event.data["block_entities"] as? ListTag ?: return
		val iterator = blockEntities.iterator()
		while(iterator.hasNext()) {
			val blockEntity = iterator.next() as CompoundTag
			if(blockEntity.getString("id") == "create:fake_track") {
				iterator.remove()
			}
		}
	}
}
