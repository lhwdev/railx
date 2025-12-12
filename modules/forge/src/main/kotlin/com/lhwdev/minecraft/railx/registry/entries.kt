package com.lhwdev.minecraft.railx.registry

import com.simibubi.create.foundation.data.CreateBlockEntityBuilder
import com.tterrag.registrate.AbstractRegistrate
import com.tterrag.registrate.builders.BuilderCallback
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import com.tterrag.registrate.util.nullness.NonNullSupplier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.neoforge.registries.DeferredHolder


class RailXBlockEntityBuilder<T : BlockEntity, P>(
	owner: AbstractRegistrate<*>,
	parent: P,
	name: String,
	callback: BuilderCallback,
	factory: BlockEntityFactory<T>,
) : CreateBlockEntityBuilder<T, P>(owner, parent, name, callback, factory) {
	private val additionalValidBlocks = mutableListOf<NonNullSupplier<Block>>()
	
	override fun createEntry(): BlockEntityType<T> {
		for(additional in additionalValidBlocks) validBlock(additional)
		return super.createEntry()
	}
	
	override fun createEntryWrapper(
		delegate: DeferredHolder<BlockEntityType<*>, BlockEntityType<T>>,
	): RegistryEntry<BlockEntityType<*>, BlockEntityType<T>> =
		RailXBlockEntityEntry(owner, delegate, additionalValidBlocks)
}


class RailXBlockEntityEntry<T : BlockEntity>(
	owner: AbstractRegistrate<*>,
	delegate: DeferredHolder<BlockEntityType<*>, BlockEntityType<T>>,
	private val additionalValidBlocks: MutableList<NonNullSupplier<Block>>,
) : BlockEntityEntry<T>(owner, delegate) {
	fun validBlock(block: NonNullSupplier<Block>) {
		additionalValidBlocks += block
	}
	
	fun validBlocks(vararg block: NonNullSupplier<Block>) {
		additionalValidBlocks += block
	}
}
