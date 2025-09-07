package com.lhwdev.minecraft.railx.utils

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag


inline fun <reified Tag, R> CompoundTag.getList(key: String, block: (Tag) -> R): List<R> =
	(get(key) as ListTag).map { block(it as Tag) }
