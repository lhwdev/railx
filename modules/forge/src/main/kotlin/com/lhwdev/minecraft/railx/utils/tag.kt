package com.lhwdev.minecraft.railx.utils

import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.phys.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream


inline fun CompoundTag(block: (tag: CompoundTag) -> Unit): CompoundTag =
	CompoundTag().also(block)


fun CompoundTag.maybeCompound(key: String): CompoundTag? =
	if(contains(key)) getCompound(key) else null

inline fun <R> CompoundTag.maybeCompound(key: String, block: (CompoundTag) -> R): R? =
	if(contains(key)) block(getCompound(key)) else null

inline fun CompoundTag.putCompound(key: String, builder: (CompoundTag) -> Unit) {
	put(key, CompoundTag(builder))
}


inline fun <reified Tag, R> CompoundTag.getList(key: String, block: (Tag) -> R): List<R> =
	(get(key) as ListTag).map { block(it as Tag) }


fun CompoundTag.getVec3(key: String): Vec3 =
	VecHelper.readNBT(getList(key, Tag.TAG_DOUBLE.toInt()))

fun CompoundTag.getVec3OrNull(key: String): Vec3? =
	if(key in this) VecHelper.readNBT(getList(key, Tag.TAG_DOUBLE.toInt()))
	else null


fun Tag.realSizeInBytes(): Int {
	val source = ByteArrayOutputStream()
	write(DataOutputStream(source))
	return source.size()
}
