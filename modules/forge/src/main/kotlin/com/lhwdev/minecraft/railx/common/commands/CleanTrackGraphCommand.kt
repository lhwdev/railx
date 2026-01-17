package com.lhwdev.minecraft.railx.common.commands

import com.lhwdev.minecraft.railx.registry.RailXCommandBuildContext
import com.lhwdev.minecraft.utils.vectors.plus
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.simibubi.create.Create
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.ITrackBlock
import com.simibubi.create.content.trains.track.TrackPropagator
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.neoforge.NeoForgeAdapter
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import net.neoforged.fml.ModList


private val ErrorNoWorldEdit =
	SimpleCommandExceptionType(Component.literal("requires worldedit mod for region selection"))
private val ErrorNoSelection = SimpleCommandExceptionType(Component.literal("no selection"))


fun RailXCommandBuildContext.cleanTrackGraphCommand(): LiteralArgumentBuilder<CommandSourceStack> = Commands
	.literal("clearTrackGraph")
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
		
		class TrackNodeToRemove(val graph: TrackGraph, val node: TrackNodeLocation)
		
		val manager = Create.RAILWAYS
		val nodesToRemove = mutableListOf<TrackNodeToRemove>()
		for(graph in manager.trackNetworks.values) {
			for(node in graph.nodes) {
				if(node.dimension != dimension) continue
				if(!inSelection(node.location)) continue
				if(node !in nodes) {
					nodesToRemove += TrackNodeToRemove(graph, node)
				} else {
					nodes -= node
				}
			}
		}
		
		for(nodeToRemove in nodesToRemove) {
			val graph = nodeToRemove.graph
			val node = nodeToRemove.node
			val trackNode = graph.locateNode(node)!!
			graph.removeNode(null, node)
			manager.sync.nodeRemoved(graph, trackNode)
			if(graph.isEmpty) {
				manager.removeGraphAndGroup(graph)
				manager.sync.graphRemoved(graph)
			}
		}
		
		if(nodesToRemove.isNotEmpty()) manager.markTracksDirty()
		
		for(missingInfo in nodes.values) {
			// val node = missingInfo.current
			// if( // limitation: selecting multiple linear tracks less than certain counts -> no node added
			// 	!TrackPropagator.isValidGraphNodeLocation(
			// 		node,
			// 		missingInfo.next.filter { it != node },
			// 		false
			// 	)
			// ) continue
			
			// TODO: implement this, which will improve performance a lot.
			val pos = missingInfo.pos
			TrackPropagator.onRailAdded(level, pos, level.getBlockState(pos))
		}
		
		source.sendSuccess({ Component.literal("Successfully updated all track nodes in selection") }, true)
		0
	}

private object LocationComparator : Comparator<TrackNodeLocation> {
	override fun compare(a: TrackNodeLocation, b: TrackNodeLocation): Int {
		var result = a.x.compareTo(b.x)
		if(result != 0) return result
		result = a.y.compareTo(b.y)
		if(result != 0) return result
		result = a.z.compareTo(b.z)
		if(result != 0) return result
		
		val aa = a.location
		val bb = b.location
		result = aa.x.compareTo(bb.x)
		if(result != 0) return result
		result = aa.y.compareTo(bb.y)
		if(result != 0) return result
		result = aa.z.compareTo(bb.z)
		if(result != 0) return result
		
		return 0
	}
}

