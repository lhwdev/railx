package com.lhwdev.minecraft.railx.ccAdvanced

import com.lhwdev.minecraft.railx.RailXConfig
import com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver.AdvancedTrackObserver
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag


class CarriageMetadata {
	companion object {
		init {
			AdvancedTrackObserver.ObserverEdgePointType
		}
	}
	
	private var data = CompoundTag()
	private var dataSizeInBytes = data.sizeInBytes()
	
	private val config get() = RailXConfig.Server.carriageMetadata
	
	fun writeInline(tag: CompoundTag) {
		if(data.sizeInBytes() > config.carriageMaxSize.get()) {
			println("Warning: metadata too large; ${data.sizeInBytes()} exceeds configured size.")
			return
		}
		tag.put("RailxMetadata", data)
	}
	
	fun readInline(tag: CompoundTag) {
		data = tag.getCompound("RailxMetadata")
		dataSizeInBytes = data.sizeInBytes()
	}
	
	
	val entries: Map<String, Tag> = object : AbstractMap<String, Tag>(), Set<Map.Entry<String, Tag>> {
		override val keys: Set<String>
			get() = data.allKeys
		
		override fun containsKey(key: String): Boolean =
			data.contains(key)
		
		override fun get(key: String): Tag? =
			data.get(key)
		
		override fun isEmpty(): Boolean =
			@Suppress("ReplaceSizeCheckWithIsNotEmpty", "RedundantSuppression")
			size != 0
		
		override val entries: Set<Map.Entry<String, Tag>>
			get() = this
		
		
		override fun iterator(): Iterator<Map.Entry<String, Tag>> = object : Iterator<Map.Entry<String, Tag>> {
			private val keys = data.allKeys.iterator()
			
			override fun hasNext(): Boolean = keys.hasNext()
			
			override fun next(): Map.Entry<String, Tag> = object : Map.Entry<String, Tag> {
				override val key: String = keys.next()
				override val value: Tag = data.get(key)!!
			}
		}
		
		override val size: Int
			get() = data.size()
		
		override fun contains(element: Map.Entry<String, Tag>): Boolean =
			(this as Set<Map.Entry<String, Tag>>).any { it == element }
		
		override fun containsAll(elements: Collection<Map.Entry<String, Tag>>): Boolean =
			elements.all { (this as Set<Map.Entry<String, Tag>>).contains(it) }
		
	}
	
	fun putEntry(key: String, value: Tag): Boolean {
		val size = entrySizeInBytes(key, value)
		if(size > config.entryMaxSize.get()) return false
		val newSize = Math.addExact(dataSizeInBytes, size)
		if(newSize > config.carriageMaxSize.get()) return false
		
		data.put(key, value)
		dataSizeInBytes += size
		return true
	}
	
	fun removeEntry(key: String): Tag? {
		val previous = data.get(key)
		if(previous != null) {
			data.remove(key)
			dataSizeInBytes -= entrySizeInBytes(key, previous)
		}
		return previous
	}
	
	fun getEntry(key: String): Tag =
		data.get(key) ?: throw NullPointerException("no $key in data")
	
	fun getEntryOrNull(key: String): Tag? =
		data.get(key)
	
	inline fun <reified T : Tag> getEntry(key: String): T =
		getEntry(key) as T
	
	inline fun <reified T : Tag> getEntryOrNull(key: String): T? =
		getEntry(key) as? T
	
	
	private fun entrySizeInBytes(key: String, value: Tag): Int {
		return (28 + 2 * key.length) + 36 + value.sizeInBytes()
	}
}
