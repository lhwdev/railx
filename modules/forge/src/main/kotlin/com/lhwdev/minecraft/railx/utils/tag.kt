package com.lhwdev.minecraft.railx.utils

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream


inline fun <reified Tag, R> CompoundTag.getList(key: String, block: (Tag) -> R): List<R> =
	(get(key) as ListTag).map { block(it as Tag) }

fun Tag.realSizeInBytes(): Int {
	val source = ByteArrayOutputStream()
	write(DataOutputStream(source))
	return source.size()
}
