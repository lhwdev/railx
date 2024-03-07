package com.lhwdev.minecraft.railx.config

import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.ForgeConfigSpec

open class RailXConfigServer(builder: ForgeConfigSpec.Builder) {
	val BANNED_LINK_ITEMS: ForgeConfigSpec.ConfigValue<List<String>> =
		builder.comment("These are the items the computerized redstone link cannot use.").define(
			"computerized_redstone_link.banned_link_items",
			listOf(
				ResourceLocation("minecraft", "dragon_egg").toString(),
				ResourceLocation("minecraft", "nether_star").toString()
			)
		)
	
	val MAXIMUM_CONCURRENT_LINKS: ForgeConfigSpec.LongValue =
		builder.comment("This is the maximum amount of concurrent handles one computerized redstone link is allowed to have.")
			.defineInRange(
				"computerized_redstone_link.maximum_concurrent_links",
				8,
				1, Long.MAX_VALUE
			)
	
	val spec = builder.build()
	
	
	companion object : RailXConfigServer(ForgeConfigSpec.Builder())
}
