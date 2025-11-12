package com.lhwdev.minecraft.railx.utils

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor


fun LevelAccessor.dimension(): ResourceKey<Level> =
	(this as? Level)?.dimension() ?: Level.OVERWORLD
