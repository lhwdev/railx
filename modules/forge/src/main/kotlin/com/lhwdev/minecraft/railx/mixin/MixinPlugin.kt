package com.lhwdev.minecraft.railx.mixin

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import org.spongepowered.asm.mixin.extensibility.IRemapper
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper


class MixinPlugin : IMixinConfigPlugin {
	init {
		val env = MixinEnvironment.getDefaultEnvironment()
		val resource = env.getOptionValue(MixinEnvironment.Option.REFMAP_REMAP_RESOURCE)
			.orEmpty()
			.ifEmpty {
				RemappingReferenceMapper::class.java.getDeclaredField("DEFAULT_RESOURCE_PATH_PROPERTY")
					.also { it.isAccessible = true }
					.get(null)
			}
		@Suppress("UNCHECKED_CAST")
		val mapping = RemappingReferenceMapper::class.java.getDeclaredMethod("loadSrgs", String::class.java)
			.also { it.isAccessible = true }
			.invoke(null, resource) as Map<String, String>
		
		val remapper = object : IRemapper {
			override fun mapMethodName(owner: String, name: String, desc: String): String =
				mapping[name] ?: name
			
			override fun mapFieldName(owner: String, name: String, desc: String): String =
				mapping[name] ?: name
			
			override fun map(typeName: String): String  =
				mapping[typeName] ?: typeName
			
			override fun unmap(typeName: String): String =
				error("does not supports")
			
			override fun mapDesc(desc: String): String =
				desc
			
			override fun unmapDesc(desc: String): String =
				desc
		}
		env.remappers.add(remapper)
	}
	
	override fun onLoad(mixinPackage: String) {}
	
	override fun getRefMapperConfig(): String? = null
	
	override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = true
	
	override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) {}
	
	override fun getMixins(): List<String>? = null
	
	override fun preApply(
		targetClassName: String,
		targetClass: ClassNode,
		mixinClassName: String,
		mixinInfo: IMixinInfo,
	) {
	}
	
	override fun postApply(
		targetClassName: String,
		targetClass: ClassNode,
		mixinClassName: String,
		mixinInfo: IMixinInfo,
	) {
	}
}
