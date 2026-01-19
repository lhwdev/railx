package com.lhwdev.minecraft.railx.common.commands

import com.lhwdev.minecraft.railx.realisticSpeed.CustomContraptionBlockEntities
import com.lhwdev.minecraft.railx.realisticSpeed.entities
import com.lhwdev.minecraft.railx.registry.RailXCommandBuildContext
import com.lhwdev.minecraft.railx.utils.ContraptionLevelReader
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.simibubi.create.Create
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.trains.entity.Carriage
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraftforge.fml.ModList
import xfacthd.framedblocks.api.block.IFramedBlock
import java.lang.invoke.MethodHandles


private val ErrorNoFramedBlock =
	SimpleCommandExceptionType(Component.literal("requires FramedBlocks mod"))

fun RailXCommandBuildContext.fixBrokenFramedTrainsCommand(): LiteralArgumentBuilder<CommandSourceStack> = Commands
	.literal("fixBrokenFramedTrains")
	.requires { it.hasPermission(2) }
	.executes { context ->
		if(!ModList.get().isLoaded("framedblocks")) throw ErrorNoFramedBlock.create()
		
		val source = context.source
		val anyLevel = source.level
		
		for(train in Create.RAILWAYS.trains.values) {
			for(carriage in train.carriages) {
				val contraptionEntity = carriage.entities.values.firstNotNullOfOrNull { it.entity.get() }
				if(contraptionEntity != null) {
					val contraption = contraptionEntity.contraption ?: continue
					fixContraption(anyLevel, contraption)
				} else {
					val contraption = FixBrokenFramedContraptionCommandReflection.getRawContraption(anyLevel, carriage)
					fixContraption(anyLevel, contraption)
					FixBrokenFramedContraptionCommandReflection.writeRawContraption(anyLevel, carriage, contraption)
				}
			}
		}
		
		source.sendSuccess(
			{ Component.literal("Successfully updated every trains. Please rejoin world from client.") },
			true
		)
		1
	}

private fun fixContraption(anyLevel: Level, contraption: Contraption) {
	val updateTags = FixBrokenFramedContraptionCommandReflection.getUpdateTags(contraption)
	val blockEntities = LazyContraptionBlockEntities(contraption, anyLevel)
	val level = ContraptionLevelReader(anyLevel, contraption, blockEntities)
	for((pos, info) in contraption.blocks) {
		if(info.state.block !is IFramedBlock) continue
		if(updateTags[pos] == null) continue
		
		try {
			val be = blockEntities.readBlockEntity(info, legacy = false) ?: continue
			blockEntities.blockEntities[pos] = be
			
			updateTags[pos] = be.updateTag
		} catch(e: Throwable) {
			System.err.println("Error happened while handling block $info")
			e.printStackTrace()
		}
		
	}
}

private class LazyContraptionBlockEntities(contraption: Contraption, level: Level) :
	CustomContraptionBlockEntities(contraption, level) {
	override val isClientSide: Boolean
		get() = false
}


@Suppress("UNCHECKED_CAST")
private object FixBrokenFramedContraptionCommandReflection {
	val updateTags = Contraption::class.java.getDeclaredField("updateTags")
		.also { it.isAccessible = true }
		.let { MethodHandles.lookup().unreflectGetter(it) }
	
	private val serialisedEntity = Carriage::class.java.getDeclaredField("serialisedEntity")
		.also { it.isAccessible = true }
		.let { MethodHandles.lookup().unreflectGetter(it) }
	
	fun getUpdateTags(self: Contraption): MutableMap<BlockPos, CompoundTag> =
		updateTags.invokeExact(self) as MutableMap<BlockPos, CompoundTag>
	
	fun getRawContraption(level: Level, carriage: Carriage): Contraption {
		val serialisedEntity = serialisedEntity.invokeExact(carriage) as CompoundTag
		return Contraption.fromNBT(level, serialisedEntity.getCompound("Contraption"), false)
	}
	
	fun writeRawContraption(level: Level, carriage: Carriage, contraption: Contraption) {
		val serialisedEntity = serialisedEntity.invokeExact(carriage) as CompoundTag
		serialisedEntity.put("Contraption", contraption.writeNBT(false))
	}
}
