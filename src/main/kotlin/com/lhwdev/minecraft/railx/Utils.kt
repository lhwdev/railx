package com.lhwdev.minecraft.railx

import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.registries.ForgeRegistries

object Utils {
	fun getByName(loc: ResourceLocation): Item {
		val itemRegistryObject = ForgeRegistries.ITEMS.getHolder(loc)
		return if(itemRegistryObject.isEmpty) Items.AIR else itemRegistryObject.get().get()
	}
	
	fun rotate(from: Direction, to: Direction, shape: VoxelShape): VoxelShape {
		val buffer = arrayOf(shape, Shapes.empty())
		val times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4
		for(i in 0 until times) {
			buffer[0].forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
				buffer[1] = Shapes.or(
					buffer[1], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)
				)
			}
			buffer[0] = buffer[1]
			buffer[1] = Shapes.empty()
		}
		return buffer[0]
	}
	
	fun blowNBT(tag: Tag?): Any? {
		val ret: MutableMap<Any, Any?> = HashMap()
		if(tag == null) return ret
		if(tag is CompoundTag) {
			for(key in tag.allKeys) {
				ret[key] = blowNBT(tag[key])
			}
			return ret
		}
		if(tag is CollectionTag<*>) {
			var idx = 1
			for(t in tag) {
				ret[idx] = if(t is Tag) blowNBT(t) else t
				idx++
			}
			return ret
		}
		if(tag is IntTag) {
			return tag.asNumber
		}
		if(tag is ByteTag) {
			return tag.asNumber
		}
		if(tag is ShortTag) {
			return tag.asNumber
		}
		if(tag is LongTag) {
			return tag.asNumber
		}
		if(tag is FloatTag) {
			return tag.asNumber
		}
		if(tag is DoubleTag) {
			return tag.asNumber
		}
		if(tag is StringTag) {
			return tag.getAsString()
		}
		System.err.println("Invalid tag type: " + tag.javaClass.getName())
		return null
	}
	
	interface Receiver<T> {
		fun receive(a: T)
	}
}
