package com.lhwdev.minecraft.railx.realisticSpeed

import com.simibubi.create.content.fluids.tank.FluidTankBlock
import com.simibubi.create.content.logistics.vault.ItemVaultBlock
import com.simibubi.create.foundation.utility.BlockHelper
import it.unimi.dsi.fastutil.objects.Reference2ShortArrayMap
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import java.lang.invoke.MethodHandles


object DefaultMaterialResolver : BlockMaterialResolver {
	override val priority: Int
		get() = Int.MIN_VALUE
	
	val Default: BlockMaterial = BlockMaterial(priority = -100, mass = 1000.0, debugSource = "default")
	
	private sealed class Mapping(val priority: Int) {
		abstract fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial?
	}
	
	private abstract class MappingFullBlock(priority: Int) : Mapping(priority) {
		private val cache = Reference2ShortArrayMap<Block>()
		
		final override fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial? {
			val material = resolveFullBlock(level, pos, state) ?: return null
			var volume = cache.getShort(state.block).toInt()
			if(volume == 0) {
				volume = state.block.defaultBlockState().getCollisionShape(level, pos).calculateVolume()
				cache.put(state.block, volume.toShort())
			}
			return BlockMaterial(
				priority = material.priority,
				mass = material.mass * volume.toDouble() / BlockVolume,
				debugSource = material.debugSource,
			)
		}
		
		abstract fun resolveFullBlock(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial?
	}
	
	private class MappingBlock(val block: Block, priority: Int) : MappingFullBlock(priority) {
		override fun resolveFullBlock(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial? =
			DatapackMaterialResolver.resolve(level, pos, block.defaultBlockState())
	}
	
	private class MappingMaterial(val material: BlockMaterial, priority: Int) : Mapping(priority) {
		override fun resolve(level: LevelReader, pos: BlockPos, state: BlockState): BlockMaterial =
			material
	}
	
	private val soundTypeMapping = mapOf(
		SoundType.WOOD to MappingBlock(Blocks.OAK_WOOD, 3),
		SoundType.GRAVEL to MappingBlock(Blocks.GRAVEL, 2),
		SoundType.GRASS to MappingBlock(Blocks.GRASS_BLOCK, 3),
		SoundType.LILY_PAD to MappingBlock(Blocks.LILY_PAD, 3),
		SoundType.STONE to MappingBlock(Blocks.STONE, 2),
		SoundType.METAL to MappingBlock(Blocks.IRON_BLOCK, 2),
		SoundType.GLASS to MappingBlock(Blocks.GLASS, 3),
		SoundType.WOOL to MappingBlock(Blocks.WHITE_WOOL, 3),
		SoundType.SAND to MappingBlock(Blocks.SAND, 3),
		SoundType.SNOW to MappingBlock(Blocks.SNOW, 3),
		SoundType.POWDER_SNOW to MappingBlock(Blocks.POWDER_SNOW, 3),
		SoundType.LADDER to MappingBlock(Blocks.OAK_WOOD, 3),
		SoundType.ANVIL to MappingBlock(Blocks.IRON_BLOCK, 3),
		SoundType.SLIME_BLOCK to MappingBlock(Blocks.SLIME_BLOCK, 3),
		SoundType.HONEY_BLOCK to MappingBlock(Blocks.HONEY_BLOCK, 3),
		SoundType.WET_GRASS to MappingBlock(Blocks.GRASS_BLOCK, 3),
		SoundType.BAMBOO to MappingBlock(Blocks.BAMBOO_BLOCK, 3),
		SoundType.BAMBOO_SAPLING to MappingBlock(Blocks.BAMBOO_BLOCK, 3),
		SoundType.SCAFFOLDING to MappingBlock(Blocks.SCAFFOLDING, 2),
		SoundType.CROP to MappingBlock(Blocks.WHEAT, 3),
		SoundType.HARD_CROP to MappingBlock(Blocks.PUMPKIN_STEM, 3),
		SoundType.VINE to MappingBlock(Blocks.VINE, 3),
		SoundType.NETHER_WART to MappingBlock(Blocks.NETHER_WART, 3),
		SoundType.LANTERN to MappingBlock(Blocks.IRON_BLOCK, 3),
		SoundType.SHROOMLIGHT to MappingBlock(Blocks.SHROOMLIGHT, 3),
		SoundType.WEEPING_VINES to MappingBlock(Blocks.WEEPING_VINES, 3),
		SoundType.TWISTING_VINES to MappingBlock(Blocks.TWISTING_VINES, 3),
		SoundType.SOUL_SAND to MappingBlock(Blocks.SOUL_SAND, 3),
		SoundType.SOUL_SOIL to MappingBlock(Blocks.SOUL_SOIL, 3),
		SoundType.BASALT to MappingBlock(Blocks.BASALT, 3),
		SoundType.NETHERRACK to MappingBlock(Blocks.NETHERRACK, 3),
		SoundType.NETHER_BRICKS to MappingBlock(Blocks.NETHER_BRICKS, 3),
		SoundType.NETHER_SPROUTS to MappingBlock(Blocks.NETHER_SPROUTS, 3),
		SoundType.BONE_BLOCK to MappingBlock(Blocks.BONE_BLOCK, 3),
		SoundType.NETHERITE_BLOCK to MappingBlock(Blocks.NETHERITE_BLOCK, 2),
		SoundType.ANCIENT_DEBRIS to MappingBlock(Blocks.ANCIENT_DEBRIS, 3),
		SoundType.LODESTONE to MappingBlock(Blocks.LODESTONE, 3),
		SoundType.CHAIN to MappingBlock(Blocks.CHAIN, 3),
		SoundType.NETHER_GOLD_ORE to MappingBlock(Blocks.NETHER_GOLD_ORE, 3),
		SoundType.GILDED_BLACKSTONE to MappingBlock(Blocks.GILDED_BLACKSTONE, 3),
		SoundType.CANDLE to MappingBlock(Blocks.CANDLE, 3),
		SoundType.AMETHYST to MappingBlock(Blocks.AMETHYST_BLOCK, 3),
		SoundType.AMETHYST_CLUSTER to MappingBlock(Blocks.AMETHYST_CLUSTER, 3),
		SoundType.SMALL_AMETHYST_BUD to MappingBlock(Blocks.SMALL_AMETHYST_BUD, 3),
		SoundType.MEDIUM_AMETHYST_BUD to MappingBlock(Blocks.MEDIUM_AMETHYST_BUD, 3),
		SoundType.LARGE_AMETHYST_BUD to MappingBlock(Blocks.LARGE_AMETHYST_BUD, 3),
		SoundType.TUFF to MappingBlock(Blocks.TUFF, 3),
		SoundType.CALCITE to MappingBlock(Blocks.CALCITE, 3),
		SoundType.DRIPSTONE_BLOCK to MappingBlock(Blocks.DRIPSTONE_BLOCK, 3),
		SoundType.POINTED_DRIPSTONE to MappingBlock(Blocks.POINTED_DRIPSTONE, 3),
		SoundType.COPPER to MappingBlock(Blocks.COPPER_BLOCK, 3),
		SoundType.CAVE_VINES to MappingBlock(Blocks.CAVE_VINES, 3),
		SoundType.SPORE_BLOSSOM to MappingBlock(Blocks.SPORE_BLOSSOM, 3),
		SoundType.AZALEA to MappingBlock(Blocks.AZALEA, 3),
		SoundType.FLOWERING_AZALEA to MappingBlock(Blocks.FLOWERING_AZALEA, 3),
		SoundType.MOSS_CARPET to MappingBlock(Blocks.MOSS_CARPET, 3),
		SoundType.PINK_PETALS to MappingBlock(Blocks.PINK_PETALS, 3),
		SoundType.MOSS to MappingBlock(Blocks.MOSS_BLOCK, 3),
		SoundType.BIG_DRIPLEAF to MappingBlock(Blocks.BIG_DRIPLEAF, 3),
		SoundType.SMALL_DRIPLEAF to MappingBlock(Blocks.SMALL_DRIPLEAF, 3),
		SoundType.DEEPSLATE to MappingBlock(Blocks.DEEPSLATE, 3),
		SoundType.DEEPSLATE_BRICKS to MappingBlock(Blocks.DEEPSLATE_BRICKS, 3),
		SoundType.DEEPSLATE_TILES to MappingBlock(Blocks.DEEPSLATE_TILES, 3),
		SoundType.POLISHED_DEEPSLATE to MappingBlock(Blocks.POLISHED_DEEPSLATE, 3),
		SoundType.FROGLIGHT to MappingBlock(Blocks.OCHRE_FROGLIGHT, 3),
		SoundType.MANGROVE_ROOTS to MappingBlock(Blocks.MANGROVE_ROOTS, 3),
		SoundType.MUDDY_MANGROVE_ROOTS to MappingBlock(Blocks.MUDDY_MANGROVE_ROOTS, 3),
		SoundType.MUD to MappingBlock(Blocks.MUD, 3),
		SoundType.MUD_BRICKS to MappingBlock(Blocks.MUD_BRICKS, 3),
		SoundType.PACKED_MUD to MappingBlock(Blocks.PACKED_MUD, 3),
		SoundType.HANGING_SIGN to MappingBlock(Blocks.OAK_HANGING_SIGN, 3),
		SoundType.NETHER_WOOD_HANGING_SIGN to MappingBlock(Blocks.OAK_HANGING_SIGN, 3),
		SoundType.BAMBOO_WOOD_HANGING_SIGN to MappingBlock(Blocks.OAK_HANGING_SIGN, 3),
		SoundType.BAMBOO_WOOD to MappingBlock(Blocks.BAMBOO_BLOCK, 3),
		SoundType.CHERRY_WOOD to MappingBlock(Blocks.CHERRY_WOOD, 3),
		SoundType.CHERRY_SAPLING to MappingBlock(Blocks.CHERRY_SAPLING, 3),
		SoundType.CHERRY_LEAVES to MappingBlock(Blocks.CHERRY_LEAVES, 3),
		SoundType.CHERRY_WOOD_HANGING_SIGN to MappingBlock(Blocks.OAK_HANGING_SIGN, 3),
		SoundType.CHISELED_BOOKSHELF to MappingBlock(Blocks.CHISELED_BOOKSHELF, 3),
		
		// create mod
		FluidTankBlock.SILENCED_METAL to MappingBlock(Blocks.COPPER_BLOCK, 8),
		ItemVaultBlock.SILENCED_METAL to MappingBlock(Blocks.IRON_BLOCK, 8),
	)
	
	private val mapColorMapping = mapOf(
		MapColor.GRASS to MappingBlock(Blocks.GRASS_BLOCK, priority = 3),
		MapColor.SAND to MappingBlock(Blocks.SAND, priority = 1),
		MapColor.WOOL to MappingBlock(Blocks.WHITE_WOOL, priority = 2),
		MapColor.FIRE to MappingBlock(Blocks.FIRE, priority = 2),
		MapColor.ICE to MappingBlock(Blocks.ICE, priority = 10),
		MapColor.METAL to MappingBlock(Blocks.IRON_BLOCK, priority = 3),
		MapColor.PLANT to MappingBlock(Blocks.DANDELION, priority = 3),
		MapColor.SNOW to MappingBlock(Blocks.WHITE_WOOL, priority = 1),
		MapColor.CLAY to MappingBlock(Blocks.CLAY, priority = 3),
		MapColor.DIRT to MappingBlock(Blocks.DIRT, priority = 1),
		MapColor.STONE to MappingBlock(Blocks.STONE, priority = 3),
		MapColor.WATER to MappingBlock(Blocks.WATER, priority = 3),
		MapColor.WOOD to MappingBlock(Blocks.OAK_WOOD, priority = 3),
		MapColor.QUARTZ to MappingBlock(Blocks.QUARTZ_BLOCK, priority = 3),
		MapColor.COLOR_ORANGE to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_MAGENTA to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_LIGHT_BLUE to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_YELLOW to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_LIGHT_GREEN to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_PINK to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_GRAY to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_LIGHT_GRAY to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_CYAN to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_PURPLE to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_BLUE to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_BROWN to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_GREEN to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_RED to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.COLOR_BLACK to MappingBlock(Blocks.WHITE_CONCRETE, priority = 1),
		MapColor.GOLD to MappingBlock(Blocks.GOLD_BLOCK, priority = 2),
		MapColor.DIAMOND to MappingBlock(Blocks.DIAMOND_BLOCK, priority = 3),
		MapColor.LAPIS to MappingBlock(Blocks.LAPIS_BLOCK, priority = 3),
		MapColor.EMERALD to MappingBlock(Blocks.EMERALD_BLOCK, priority = 3),
		MapColor.PODZOL to MappingBlock(Blocks.PODZOL, priority = 2), // andesite
		MapColor.NETHER to MappingBlock(Blocks.NETHER_WART, priority = 3),
		MapColor.TERRACOTTA_WHITE to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_ORANGE to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_MAGENTA to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_LIGHT_BLUE to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_YELLOW to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_LIGHT_GREEN to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_PINK to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_GRAY to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_LIGHT_GRAY to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_CYAN to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_PURPLE to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_BLUE to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_BROWN to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_GREEN to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_RED to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.TERRACOTTA_BLACK to MappingBlock(Blocks.TERRACOTTA, priority = 1),
		MapColor.CRIMSON_NYLIUM to MappingBlock(Blocks.CRIMSON_NYLIUM, priority = 3),
		MapColor.CRIMSON_STEM to MappingBlock(Blocks.CRIMSON_STEM, priority = 3),
		MapColor.CRIMSON_HYPHAE to MappingBlock(Blocks.CRIMSON_HYPHAE, priority = 3),
		MapColor.WARPED_NYLIUM to MappingBlock(Blocks.WARPED_NYLIUM, priority = 3),
		MapColor.WARPED_STEM to MappingBlock(Blocks.WARPED_STEM, priority = 3),
		MapColor.WARPED_HYPHAE to MappingBlock(Blocks.WARPED_HYPHAE, priority = 3),
		MapColor.WARPED_WART_BLOCK to MappingBlock(Blocks.WARPED_WART_BLOCK, priority = 3),
		MapColor.DEEPSLATE to MappingBlock(Blocks.DEEPSLATE, priority = 3),
		MapColor.RAW_IRON to MappingBlock(Blocks.RAW_IRON_BLOCK, priority = 3),
		MapColor.GLOW_LICHEN to MappingBlock(Blocks.GLOW_LICHEN, priority = 3),
	)
	
	
	override fun resolve(
		level: LevelReader,
		pos: BlockPos,
		state: BlockState,
	): BlockMaterial {
		state.block.properties.initialPropertiesSource?.let { base ->
			val result = BlockMaterials.resolve(level, pos, BlockHelper.copyProperties(state, base.defaultBlockState()))
			val volume = state.getCollisionShape(level, pos).calculateVolume()
			return if(volume == BlockVolume) {
				result
			} else {
				@Suppress("DEPRECATION")
				BlockMaterial(
					priority = result.priority,
					mass = result.mass * volume / BlockVolume,
					debugSource = "$state <- initialProperties of ${base.builtInRegistryHolder().key().location()}",
				)
			}
		}
		
		
		val candidates = mutableListOf<Mapping>()
		
		val soundType = soundTypeMapping[state.getSoundType(level, pos, null)]
		val mapColor = mapColorMapping[state.getMapColor(level, pos)]
		
		soundType?.let { candidates += it }
		mapColor?.let { candidates += it }
		
		// if matches multiple, returns first
		val result = candidates.maxByOrNull { it.priority } ?: return Default
		return result.resolve(level, pos, state) ?: Default
	}
	
	override fun cacheKey(level: LevelReader, pos: BlockPos, state: BlockState): BlockState =
		state
}
