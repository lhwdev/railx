package com.lhwdev.minecraft.railx.common.commands

import com.lhwdev.minecraft.railx.registry.RailXCommandBuildContext
import com.lhwdev.minecraft.utils.vectors.plus
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.ITrackBlock
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.neoforge.NeoForgeAdapter
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.createmod.catnip.data.WorldAttached
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import net.neoforged.fml.ModList


private val ErrorNoWorldEdit =
	SimpleCommandExceptionType(Component.literal("requires worldedit mod for region selection"))
private val ErrorNoSelection = SimpleCommandExceptionType(Component.literal("no selection"))


private var enabled = WorldAttached { false }


fun RailXCommandBuildContext.fixTrackGraphCommand(): LiteralArgumentBuilder<CommandSourceStack> = Commands
	.literal("fixTrackGraph")
	.requires { it.hasPermission(2) }
	.executes { context ->
		if(!ModList.get().isLoaded("worldedit")) throw ErrorNoWorldEdit.create()
		
		val source = context.source
		val session = WorldEdit.getInstance().sessionManager[NeoForgeAdapter.adaptCommandSource(source)]
		val selection = try {
			session.selection!!
		} catch(e: Throwable) {
			throw ErrorNoSelection.create()
		}
		
		fun inSelection(vec: Vec3) = NeoForgeAdapter.adapt(BlockPos.containing(vec)) in selection
		
		val level = NeoForgeAdapter.adapt(selection.world!!)
		val dimension = level.dimension()
		
		class EndInfo(
			val current: TrackNodeLocation.DiscoveredLocation,
			val next: Collection<TrackNodeLocation.DiscoveredLocation>,
			val pos: BlockPos,
		)
		
		val nodes = Object2ObjectRBTreeMap<TrackNodeLocation, EndInfo>(LocationComparator)
		for(blockVector in selection) {
			val pos = NeoForgeAdapter.toBlockPos(blockVector)
			val state = level.getBlockState(pos)
			val track = state.block as? ITrackBlock ?: continue
			val found = track.getConnected(level, pos, state, false, null)
			val base = TrackNodeLocation.DiscoveredLocation(
				level,
				pos.bottomCenter + Vec3(0.0, track.getElevationAtCenter(level, pos, state), 0.0),
			)
			for(end in found) {
				if(end == base) continue
				if(end.dimension != dimension) continue
				if(!inSelection(end.location)) continue
				
				// cannot use ITrackBlock.walkConnectedTracks as it didn't include 'node'
				val toConnections = end.allAdjacent().flatMap { pos ->
					val state = level.getBlockState(pos)
					(state.block as? ITrackBlock)?.getConnected(level, pos, state, false, null)
						?: emptyList()
				}
				
				if(end !in toConnections) continue
				
				nodes.computeIfAbsent(end) { EndInfo(current = end, next = toConnections, pos = pos) }
			}
		}
		
		for(node in nodes.values) {
			fixTrackBlock(level, node.pos)
		}
		
		source.sendSuccess({ Component.literal("Successfully updated all track nodes in selection") }, true)
		0
	}
