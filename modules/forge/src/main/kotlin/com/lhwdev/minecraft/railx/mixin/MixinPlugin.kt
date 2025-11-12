package com.lhwdev.minecraft.railx.mixin

import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo


class MixinPlugin : IMixinConfigPlugin {
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
