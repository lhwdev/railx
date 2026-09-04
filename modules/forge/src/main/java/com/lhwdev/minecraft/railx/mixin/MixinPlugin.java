package com.lhwdev.minecraft.railx.mixin;

import com.lhwdev.minecraft.railx.compat.CompatMods;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;


public class MixinPlugin implements IMixinConfigPlugin {
	private String mixinPackage;
	
	@Override
	public void onLoad(String mixinPackage) {
		this.mixinPackage = mixinPackage;
	}
	
	@Override
	public String getRefMapperConfig() {
		return null;
	}
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if(!mixinClassName.startsWith(mixinPackage)) return true;
		var index = mixinPackage.length() + 1;
		if(mixinClassName.startsWith("compat.", index)) {
			var index2 = index + 7;
			var index3 = mixinClassName.indexOf('.', index2);
			var compatMod = mixinClassName.substring(index2, index3);
			if(!CompatMods.loaded.contains(compatMod)) return false;
		}
		return true;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	
	@Override
	public List<String> getMixins() {
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
	
	@Override
	public void postApply(
		String targetClassName, ClassNode targetClass, String mixinClassName,
		IMixinInfo mixinInfo
	) {}
}
