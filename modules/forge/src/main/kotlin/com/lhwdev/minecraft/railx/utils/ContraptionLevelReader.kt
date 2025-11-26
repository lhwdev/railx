package com.lhwdev.minecraft.railx.utils

import com.lhwdev.minecraft.railx.realisticSpeed.ContraptionBlockEntities
import com.lhwdev.minecraft.railx.realisticSpeed.blockEntities
import com.simibubi.create.content.contraptions.Contraption
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs


class ContraptionLevelReader(val level: Level, val contraption: Contraption) : LevelReader {
	val blockEntities: ContraptionBlockEntities = contraption.blockEntities(level)
	
	private val minY = nextMultipleOf16(contraption.bounds.minY - 1)
	private val height = nextMultipleOf16(contraption.bounds.maxY + 1) - minY
	
	private fun nextMultipleOf16(a: Double) =
		(((abs(a.toInt()) - 1) or 15) + 1) * Mth.sign(a)
	
	
	override fun getChunk(x: Int, z: Int, chunkStatus: ChunkStatus, requireChunk: Boolean): ChunkAccess? = TODO()
	
	@Deprecated("Deprecated in Java")
	override fun hasChunk(chunkX: Int, chunkZ: Int): Boolean = TODO()
	
	override fun getHeight(): Int = height
	
	override fun getHeight(heightmapType: Heightmap.Types, x: Int, z: Int): Int = TODO()
	
	override fun getSkyDarken(): Int = level.skyDarken
	
	override fun getBiomeManager(): BiomeManager = TODO()
	
	override fun getUncachedNoiseBiome(x: Int, y: Int, z: Int): Holder<Biome?> = TODO()
	
	override fun isClientSide(): Boolean = level.isClientSide
	
	@Deprecated("Deprecated in Java")
	override fun getSeaLevel(): Int = TODO()
	
	override fun dimensionType(): DimensionType = TODO()
	
	override fun registryAccess(): RegistryAccess = level.registryAccess()
	
	override fun enabledFeatures(): FeatureFlagSet = level.enabledFeatures()
	
	override fun getShade(direction: Direction, shade: Boolean): Float = TODO()
	
	override fun getLightEngine(): LevelLightEngine = TODO()
	
	override fun getBlockEntity(pos: BlockPos): BlockEntity? =
		blockEntities.blockEntities[pos]
	
	
	override fun getBlockState(pos: BlockPos): BlockState =
		contraption.blocks[pos]?.state() ?: Blocks.AIR.defaultBlockState()
	
	override fun getFluidState(pos: BlockPos): FluidState =
		getBlockState(pos).fluidState
	
	override fun getWorldBorder(): WorldBorder = TODO()
	
	override fun getEntityCollisions(entity: Entity?, collisionBox: AABB): List<VoxelShape> = TODO()
	
	// override fun getModelData(pos: BlockPos): ModelData =
	// 	blockEntities.blockEntities[pos]?.modelData ?: ModelData.EMPTY
}
