package com.lhwdev.minecraft.railx.utils

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream


fun CompoundTag.maybeCompound(key: String): CompoundTag? =
	if(contains(key)) getCompound(key) else null

inline fun <R> CompoundTag.maybeCompound(key: String, block: (CompoundTag) -> R): R? =
	if(contains(key)) block(getCompound(key)) else null

inline fun CompoundTag.putCompound(key: String, builder: (CompoundTag) -> Unit) {
	put(key, CompoundTag().also(builder))
}


inline fun <reified Tag, R> CompoundTag.getList(key: String, block: (Tag) -> R): List<R> =
	(get(key) as ListTag).map { block(it as Tag) }

fun Tag.realSizeInBytes(): Int {
	val source = ByteArrayOutputStream()
	write(DataOutputStream(source))
	return source.size()
}
