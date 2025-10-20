package com.lhwdev.minecraft.railx.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.track.ITrackBlock
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.neoforge.NeoForgeAdapter
import com.sk89q.worldedit.regions.CuboidRegion
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.fml.ModList
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.plus


private val ErrorNoWorldEdit =
	SimpleCommandExceptionType(Component.literal("requires worldedit mod for region selection"))
private val ErrorNoSelection = SimpleCommandExceptionType(Component.literal("no selection"))
private val ErrorOnlyCuboidRegion = SimpleCommandExceptionType(Component.literal("only supports cuboid selection"))


val CleanTrackGraphCommand: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("clearTrackGraph")
	.requires { it.hasPermission(2) }
	.executes { context ->
		if(!ModList.get().isLoaded("worldedit")) throw ErrorNoWorldEdit.create()
		
		val source = context.source
		val session = WorldEdit.getInstance().sessionManager[NeoForgeAdapter.adaptCommandSource(source)]
		val selection = try {
			session.selection!!
		} catch(e: Throwable) {
			throw ErrorNoSelection.create()
		} as? CuboidRegion ?: throw ErrorOnlyCuboidRegion.create()
		
		val level = NeoForgeAdapter.adapt(selection.world!!)
		val area = AABB(
			NeoForgeAdapter.toVec3(selection.pos1),
			NeoForgeAdapter.toVec3(selection.pos2) + Vec3(1e-6, 1e-6, 1e-6)
		)
		
		val nodes = ObjectRBTreeSet(LocationComparator)
		for(blockVector in selection) {
			val pos = NeoForgeAdapter.toBlockPos(blockVector)
			val state = level.getBlockState(pos)
			val track = state.block as? ITrackBlock ?: continue
			val found = track.getConnected(level, pos, state, false, null)
			nodes += found
		}
		
		1
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

