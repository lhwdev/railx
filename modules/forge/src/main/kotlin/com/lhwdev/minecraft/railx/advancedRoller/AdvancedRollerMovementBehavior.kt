package com.lhwdev.minecraft.railx.advancedRoller

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity
import com.lhwdev.minecraft.railx.compat.CompatMods
import com.simibubi.create.content.contraptions.actors.roller.PaveTask
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.logistics.filter.FilterItemStack
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity
import com.simibubi.create.content.trains.entity.TravellingPoint
import com.simibubi.create.content.trains.entity.TravellingPoint.SteerDirection
import com.simibubi.create.content.trains.graph.TrackEdge
import com.simibubi.create.content.trains.graph.TrackGraph
import com.simibubi.create.foundation.item.ItemHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import io.netty.util.collection.LongObjectHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.NbtUtils
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.phys.Vec3
import java.lang.invoke.MethodHandles
import kotlin.math.abs
import kotlin.math.floor

class AdvancedRollerMovementBehavior : RollerMovementBehaviour() {
	
	fun advancedMode(context: MovementContext): AdvancedRollerBlockEntity.AdvancedRollingMode =
		AdvancedRollerBlockEntity.AdvancedRollingMode.Options[context.blockEntityData.getInt("ScrollValue")]
	
	fun filterStack(context: MovementContext): ItemStack =
		ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("Filter"))
	
	override fun getPositionsToBreak(context: MovementContext, visitedPos: BlockPos): List<BlockPos> {
		if(advancedMode(context) != AdvancedRollerBlockEntity.AdvancedRollingMode.TunnelPave) return emptyList()
		
		val filter = context.filterFromBE
		val positions = ArrayList<BlockPos>()
		
		var startingY = 1
		if(!getStateToPaveWith(context).isAir) {
			if(!ItemHelper.extract(
					context.contraption.storage.allItems,
					{ stack -> filter.test(context.world, stack) },
					1,
					true
				).isEmpty
			) startingY = 0
		}
		
		val profileForTracks = createHeightProfileForTracks(context)
		if(profileForTracks != null) {
			for(coords in profileForTracks.keys()) {
				val height = profileForTracks[coords]
				addPositionsForBreakCoords(
					context = context,
					startingY = startingY,
					x = coords.first, y = height, z = coords.second,
					into = positions
				)
			}
			return positions
		}
		
		val stateToPaveWith = getStateToPaveWith(filter.item())
		for(i in startingY..2) {
			val paveState = applyHeightToState(stateToPaveWith, height = if(i == 0) 16 else 0)
			val willBreak = testBreakerTarget(context, visitedPos.above(i), paveState, filter.item())
			if(willBreak) positions.add(visitedPos.above(i))
		}
		
		return positions
	}
	
	private fun addPositionsForBreakCoords(
		context: MovementContext,
		startingY: Int,
		x: Int, y: Float, z: Int,
		into: MutableList<BlockPos>,
	) {
		val level = context.world
		val stack = context.filterFromBE.item()
		val stateToPaveWith = getStateToPaveWith(stack)
		
		val ceilPos = BlockPos.containing(x.toDouble(), y.toDouble(), z.toDouble());
		val ceilState = level.getBlockState(ceilPos)
		val previousCeilHeight = getExistingHeight(ceilState)
		
		val height = getExpectedHeight(y - floor(y), stateToPaveWith)
		for(offsetY in (if(previousCeilHeight == 0) 2 else 3) downTo startingY) {
			val pos = ceilPos.above(offsetY)
			val paveHeight = when(offsetY) {
				0 -> 16
				1 -> height
				else -> 0
			}
			val paveState = applyHeightToState(stateToPaveWith, paveHeight)
			if(testBreakerTarget(context, pos, paveState, stack)) {
				into += pos
				if(paveHeight > 0) break
			}
		}
	}
	
	private fun testBreakerTarget(
		context: MovementContext,
		pos: BlockPos,
		paveState: BlockState,
		paveStack: ItemStack,
	): Boolean {
		val level = context.world
		val stateAtPos = level.getBlockState(pos)
		
		if(stateAtPos == paveState) {
			if(CompatMods.copycats) {
				if(paveState.block is ICopycatBlock) {
					val copycat = CopycatItemStack.parse(level, paveStack)
					val be = level.getBlockEntity(pos) as? ICopycatBlockEntity
					if(be == null) {
						if(!paveStack.has(DataComponents.BLOCK_ENTITY_DATA))
							return false
					} else if(copycat != null) {
						if(be is IMultiStateCopycatBlockEntity) {
							for((key, material) in be.materialItemStorage.materialMap) {
								val newMaterial = copycat[key]
								if(newMaterial.material != material) {
									return canBreak(level, pos, stateAtPos)
								}
							}
						} else {
							if(copycat.materials.first().material != be.material)
								return canBreak(level, pos, stateAtPos)
						}
					}
				}
			}
			return false
		}
		
		return canBreak(level, pos, stateAtPos)
	}
	
	private val rollerScout = RollerTravelingPoint()
	
	override fun createHeightProfileForTracks(context: MovementContext): PaveTask? {
		val contraption = context.contraption ?: return null
		val entity = contraption.entity as? CarriageContraptionEntity ?: return null
		val carriage = entity.carriage ?: return null
		
		val train = carriage.train ?: return null
		if(train.graph == null) return null
		
		val mainBogey = carriage.bogeys.first
		val point = mainBogey.trailing()
		
		rollerScout.node1 = point.node1
		rollerScout.node2 = point.node2
		rollerScout.edge = point.edge
		rollerScout.position = point.position
		
		var axis = Direction.Axis.X
		val info = contraption.blocks.get(BlockPos.ZERO)
		if(info != null && info.state.hasProperty(StandardBogeyBlock.AXIS))
			axis = info.state.getValue(StandardBogeyBlock.AXIS)
		
		val orientation = entity.initialOrientation
		val rollerFacing = context.state.getValue(RollerBlock.FACING)
		
		val step = orientation.axisDirection.step
		val widthWiseOffset = (axis.choose(-context.localPos.z, 0, -context.localPos.x) * step).toDouble()
		var lengthWiseOffset = (axis.choose(-context.localPos.x, 0, context.localPos.z) * step - 1).toDouble()
		
		if(rollerFacing == orientation.clockWise) lengthWiseOffset += 1.0
		
		val distanceToTravel = 2.0
		val heightProfile = PaveTask(widthWiseOffset, widthWiseOffset)
		val steering = rollerScout.steer(SteerDirection.NONE, Vec3(0.0, 1.0, 0.0))
		
		rollerScout.traversalCallback = { _, _, _ -> }
		rollerScout.travel(train.graph, lengthWiseOffset + 1, steering)
		
		rollerScout.traversalCallback = callback@{ edge, from, to ->
			if(edge == null) return@callback
			if(edge.isInterDimensional) return@callback
			if(edge.node1.location.dimension !== context.world.dimension()) return@callback
			TrackPaverV3.pave(heightProfile, train.graph, edge, from, to)
		}
		rollerScout.travel(train.graph, distanceToTravel, steering)
		rollerScout.traversalCallback = { _, _, _ -> }
		
		for(entry in heightProfile.keys()) heightProfile.put(
			entry.first,
			entry.second,
			context.localPos.y + heightProfile.get(entry)
		)
		
		return heightProfile
	}
	
	override fun triggerPaver(context: MovementContext, basePos: BlockPos) {
		val stateToPaveWith = getStateToPaveWith(context)
		val mode = advancedMode(context)
		if(mode != AdvancedRollerBlockEntity.AdvancedRollingMode.TunnelPave && stateToPaveWith.isAir) return
		
		var paveResult = PaveResult.PASS
		var yOffset = 0
		
		data class Pave(val pos: BlockPos, val height: Int) {
			fun maxOf(other: Pave): Pave = when {
				other.height > height -> other
				height > other.height -> this
				other.pos.y > pos.y -> other
				pos.y > other.pos.y -> this
				else -> this
			}
		}
		
		val paveSet = ArrayList<Pave>()
		val profileForTracks = createHeightProfileForTracks(context)
		if(profileForTracks == null) paveSet += Pave(basePos, height = 16)
		else for(coords in profileForTracks.keys()) {
			val height = profileForTracks[coords]
			val layer = getExpectedHeight(height - floor(height), stateToPaveWith)
			val targetPosition =
				BlockPos.containing(coords.first.toDouble(), height.toDouble(), coords.second.toDouble())
			paveSet.add(Pave(targetPosition, height = layer))
		}
		
		if(paveSet.isEmpty()) return
		
		fun keyOf(pos: BlockPos): Long = (pos.x.toLong() shl 32) or (pos.z.toLong() and 0xffffffffL)
		
		val topLayer = LongOpenHashSet()
		
		while(true) {
			if(yOffset > AllConfigs.server().kinetics.rollerFillDepth.get()) {
				paveResult = PaveResult.FAIL
				break
			}
			
			val currentLayer = LongObjectHashMap<Pave>()
			
			/**
			 * @param slope 1 block lower on distance `slope`
			 */
			fun fillWide(slope: Int) {
				for(anchor in paveSet) {
					val anchorHeight = anchor.height
					val radius = (yOffset * 16 + anchorHeight) * slope / 16
					
					for(i in -radius..radius) for(j in -radius..radius) {
						val distance = abs(i) + abs(j)
						if(distance > radius) continue
						
						val pos = anchor.pos.offset(i, -yOffset, j)
						val key = keyOf(pos)
						
						val previous = currentLayer[key]
						val height = yOffset * 16 + anchorHeight - distance * 16 / slope
						
						val current = Pave(pos, height = height.coerceIn(0, 16))
						currentLayer[key] = if(previous == null) current else current.maxOf(previous)
					}
				}
			}
			
			when(mode) {
				AdvancedRollerBlockEntity.AdvancedRollingMode.WideFill -> fillWide(slope = 1)
				
				AdvancedRollerBlockEntity.AdvancedRollingMode.SmoothWideFill -> fillWide(slope = 8)
				
				else -> for(anchor in paveSet) {
					val pos = anchor.pos.below(yOffset)
					currentLayer[keyOf(pos)] = Pave(pos, anchor.height)
				}
			}
			
			var completelyBlocked = true
			var anyBlockPlaced = false
			
			val iterator = currentLayer.entries.iterator()
			while(iterator.hasNext()) {
				val entry = iterator.next()
				val key = entry.key
				val pave = entry.value
				val (currentPos, height) = pave
				if(key !in topLayer) {
					if(height > 0) {
						val layeredState = applyHeightToState(stateToPaveWith, height)
						tryFill0(context, currentPos.above(), layeredState, height)
					}
					topLayer += key
				}
				
				val fullState = applyHeightToState(stateToPaveWith, height = 16)
				
				// if(fullState.block == AllBlocks.GravelLayer)
				// 	fullState = Blocks.GRAVEL.defaultBlockState()
				
				paveResult = tryFill0(context, currentPos, fullState, height = 16)
				if(paveResult != PaveResult.FAIL) completelyBlocked = false
				if(paveResult == PaveResult.SUCCESS) anyBlockPlaced = true
			}
			
			if(anyBlockPlaced) paveResult = PaveResult.SUCCESS
			else if(!completelyBlocked || yOffset == 0) paveResult = PaveResult.PASS
			
			if(paveResult == PaveResult.SUCCESS && stateToPaveWith.block is FallingBlock)
				paveResult = PaveResult.PASS
			if(paveResult != PaveResult.PASS) break
			if(mode == AdvancedRollerBlockEntity.AdvancedRollingMode.TunnelPave) break
			
			yOffset++
		}
		
		if(paveResult == PaveResult.SUCCESS) {
			context.data.putInt("WaitingTicks", 2)
			context.data.put("LastPos", NbtUtils.writeBlockPos(basePos))
			context.stall = true
		}
	}
	
	
	enum class PaveResult { FAIL, PASS, SUCCESS }
	
	protected fun tryFill0(
		context: MovementContext,
		targetPos: BlockPos,
		toPlace: BlockState,
		height: Int,
	): PaveResult {
		val level = context.world
		if(!level.isLoaded(targetPos)) return PaveResult.FAIL
		
		val existing = level.getBlockState(targetPos)
		if(existing == toPlace) return PaveResult.PASS
		
		val block = toPlace.block
		if(existing.`is`(block)) {
			val previous = getExistingHeight(existing)
			if(height <= previous) return PaveResult.PASS
		} else if(
			!existing.`is`(BlockTags.LEAVES) &&
			!existing.canBeReplaced() &&
			(!existing.getCollisionShape(level, targetPos).isEmpty || existing.`is`(BlockTags.PORTALS))
		) return PaveResult.FAIL
		
		val filter = context.getFilterFromBE()
		if(!consumeItem(context, filter, simulate = true))
			return PaveResult.FAIL
		
		if(!consumeItem(context, filter, simulate = false))
			return PaveResult.FAIL
		
		val success = level.setBlockAndUpdate(targetPos, toPlace)
		
		val stack = filter.item()
		val item = stack.item
		val state = level.getBlockState(targetPos)
		if(success && item is BlockItem && state.`is`(block)) {
			// emulate BlockItem.place
			// do not apply updateBlockStateFromTag
			PaveReflection.BlockItem_updateCustomBlockEntityTag(item, targetPos, level, player = null, stack, state)
			
			// -> emulate updateBlockEntityComponents()
			val blockEntity = level.getBlockEntity(targetPos)
			if(blockEntity != null) {
				blockEntity.applyComponentsFromItemStack(stack)
				blockEntity.setChanged()
			}
		}
		
		return PaveResult.SUCCESS
	}
	
	protected fun consumeItem(context: MovementContext, filter: FilterItemStack, simulate: Boolean): Boolean {
		val held = ItemHelper.extract(
			context.contraption.storage.allItems,
			{ stack -> filter.test(context.world, stack) },
			1,
			simulate
		)
		if(held.isEmpty) return false
		val stack = filter.item()
		val item = stack.item as? BlockItem ?: return false
		val block = item.block
		if(CompatMods.copycats) {
			if(block is ICopycatBlock) {
				val copycat = CopycatItemStack.parse(context.world, stack)
				if(copycat != null) for(material in copycat.materials) {
					if(material.isEmpty) continue
					
					val consumedItem = material.consumedItem
					ItemHelper.extract(
						context.contraption.storage.allItems,
						{ stack -> stack.`is`(consumedItem.item) },
						consumedItem.count,
						simulate,
					)
				}
			}
		}
		
		return true
	}
	
	
	private object PaveReflection {
		val BlockItem_updateCustomBlockEntityTag = BlockItem::class.java
			.getDeclaredMethod(
				"updateCustomBlockEntityTag",
				BlockPos::class.java, Level::class.java, Player::class.java,
				ItemStack::class.java, BlockState::class.java
			)
			.also { it.isAccessible = true }
			.let { MethodHandles.lookup().unreflect(it) }
		
		fun BlockItem_updateCustomBlockEntityTag(
			self: BlockItem,
			pos: BlockPos,
			level: Level,
			player: Player?,
			stack: ItemStack,
			state: BlockState,
		): Boolean = BlockItem_updateCustomBlockEntityTag.invokeExact(self, pos, level, player, stack, state) as Boolean
	}
	
	
	private fun getExpectedHeight(y: Float, stateToPaveWith: BlockState): Int = when {
		stateToPaveWith.hasProperty(SlabBlock.TYPE) -> if(y > 0.45) 8 else 16
		
		stateToPaveWith.hasProperty(BlockStateProperties.LAYERS) -> Mth.floor(y * 8) * 2
		
		else -> 16
	}
	
	private fun getExistingHeight(state: BlockState): Int = when {
		state.hasProperty(SlabBlock.TYPE) -> when(state.getValue(SlabBlock.TYPE)) {
			SlabType.BOTTOM -> 8
			SlabType.DOUBLE -> 16
			SlabType.TOP -> 17 // 'need to be broken'
		}
		
		state.hasProperty(BlockStateProperties.LAYERS) -> state.getValue(BlockStateProperties.LAYERS) * 2
		
		else -> 16
	}
	
	private fun applyHeightToState(state: BlockState, height: Int): BlockState = when {
		state.hasProperty(SlabBlock.TYPE) -> when(height) {
			in 0..4 -> Blocks.AIR.defaultBlockState()
			in 5..12 -> state.setValue(SlabBlock.TYPE, SlabType.BOTTOM)
			else -> state.setValue(SlabBlock.TYPE, SlabType.DOUBLE)
		}
		
		state.hasProperty(BlockStateProperties.LAYERS) -> when {
			height <= 1 -> Blocks.AIR.defaultBlockState()
			else -> state.setValue(BlockStateProperties.LAYERS, (height / 2).coerceIn(1, 8))
		}
		
		height < 8 -> Blocks.AIR.defaultBlockState()
		
		else -> state
	}
	
	
	private class RollerTravelingPoint : TravellingPoint() {
		lateinit var traversalCallback: (edge: TrackEdge?, from: Double, to: Double) -> Unit
		
		override fun edgeTraversedFrom(
			graph: TrackGraph?,
			forward: Boolean,
			edgePointListener: IEdgePointListener?,
			turnListener: ITurnListener?,
			prevPos: Double,
			totalDistance: Double,
		): Double? {
			val from = if(forward) prevPos else position
			val to = if(forward) position else prevPos
			traversalCallback(edge, from, to)
			return super.edgeTraversedFrom(graph, forward, edgePointListener, turnListener, prevPos, totalDistance)
		}
	}
}
