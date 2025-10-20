package com.lhwdev.minecraft.railx.registry

import com.lhwdev.minecraft.railx.RailX
import net.createmod.catnip.render.BindableTexture
import net.minecraft.resources.ResourceLocation

private const val ASSET_PATH: String = "textures/special/"


enum class AllSpecialTextures(filename: String) : BindableTexture {
	BOLD_THIN_CHECKERED("bold_thin_checkerboard.png");
	
	private val location: ResourceLocation = RailX.asResource(ASSET_PATH + filename)
	
	override fun getLocation(): ResourceLocation {
		return location
	}
}
