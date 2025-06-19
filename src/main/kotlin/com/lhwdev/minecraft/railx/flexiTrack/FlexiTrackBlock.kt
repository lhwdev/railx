package com.lhwdev.minecraft.railx.flexiTrack

import com.simibubi.create.AllPartialModels
import com.simibubi.create.AllShapes
import com.simibubi.create.AllTags
import com.simibubi.create.content.trains.graph.TrackNodeLocation
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.track.*
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.Affine
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.function.Consumer


class FlexiTrackBlock(properties: Properties, material: TrackMaterial) : TrackBlock(properties, material) {
	companion object {
		val Shape: EnumProperty<TrackShape> = SHAPE
		val TrackShapeFlexi = TrackShape.NONE
		val HasBe: BooleanProperty = HAS_BE
		val Waterlogged: BooleanProperty = WATERLOGGED
	}
	
	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(Shape, TrackShapeFlexi)
				.setValue(HasBe, false)
				.setValue(Waterlogged, false)
		)
	}
	
	override fun getConnected(
		worldIn: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		linear: Boolean,
		connectedTo: TrackNodeLocation?,
	): MutableCollection<DiscoveredLocation?>? {
		val list: MutableCollection<DiscoveredLocation?>?
		val world = if(connectedTo != null && worldIn is ServerLevel) {
			worldIn.server.getLevel(connectedTo.dimension)!!
		} else worldIn
		
		if(getTrackAxes(world, pos, state).size > 1) {
			val center = Vec3.atBottomCenterOf(pos)
				.add(0.0, getElevationAtCenter(world, pos, state), 0.0)
			val shape = state.getValue(SHAPE)
			list = ArrayList()
			for(axis in getTrackAxes(
				world,
				pos,
				state
			)) for(fromCenter in Iterate.trueAndFalse) ITrackBlock.addToListIfConnected(
				connectedTo,
				list,
				{ d: Double, b: Boolean ->
					axis.scale((if(b) 0.0 else if(fromCenter) -d else d))
						.add(center)
				},
				{ b: Boolean -> shape.normal },
				{ b: Boolean -> if(world is Level) world.dimension() else Level.OVERWORLD },
				{ v: Vec3? -> 0 },
				axis,
				null,
				{ b: Boolean, v: Vec3 -> ITrackBlock.getMaterialSimple(world, v) })
		} else list = super.getConnected(world, pos, state, linear, connectedTo)
		
		if(!state.getValue(HAS_BE)) return list
		if(linear) return list
		
		val blockEntity = world.getBlockEntity(pos)
		if(blockEntity !is TrackBlockEntity) return list
		
		val connections = blockEntity.getConnections()
		connections.forEach { (connectedPos: BlockPos?, bc: BezierConnection) ->
			ITrackBlock.addToListIfConnected(
				connectedTo,
				list,
				{ d: Double, b: Boolean ->
					if(d == 1.0) Vec3.atLowerCornerOf(bc.bePositions.get(b)) else bc.starts.get(
						b
					)
				},
				{ first: Boolean -> bc.normals.get(first) },
				{ b: Boolean -> if(world is Level) world.dimension() else Level.OVERWORLD },
				{ end: Vec3 -> bc.yOffsetAt(end) },
				null,
				bc,
				{ b: Boolean, v: Vec3 -> ITrackBlock.getMaterialSimple(world, v, bc.material) })
		}
		
		if(blockEntity.boundLocation == null || world !is ServerLevel) return list
		
		val otherDim = blockEntity.boundLocation.getFirst()
		val otherLevel = world.getServer()
			.getLevel(otherDim)
		if(otherLevel == null) return list
		val boundPos = blockEntity.boundLocation.getSecond()
		val boundState = otherLevel.getBlockState(boundPos)
		if(!AllTags.AllBlockTags.TRACKS.matches(boundState)) return list
		
		val center = Vec3.atBottomCenterOf(pos)
			.add(0.0, getElevationAtCenter(world, pos, state), 0.0)
		val boundCenter = Vec3.atBottomCenterOf(boundPos)
			.add(0.0, getElevationAtCenter(otherLevel, boundPos, boundState), 0.0)
		val shape = state.getValue(SHAPE)
		val boundShape = boundState.getValue(SHAPE)
		val boundAxis = getTrackAxes(otherLevel, boundPos, boundState).get(0)
		
		getTrackAxes(world, pos, state).forEach(Consumer { axis: Vec3? ->
			ITrackBlock.addToListIfConnected(
				connectedTo,
				list,
				{ d: Double, b: Boolean ->
					(if(b) axis else boundAxis)!!.scale(d)
						.add(if(b) center else boundCenter)
				},
				{ b: Boolean -> (if(b) shape else boundShape).getNormal() },
				{ b: Boolean -> if(b) world.dimension() else otherLevel.dimension() },
				{ v: Vec3? -> 0 },
				axis,
				null,
				{ b: Boolean, v: Vec3 -> ITrackBlock.getMaterialSimple(if(b) world else otherLevel, v) })
		})
		
		return list
	}
	
	override fun getShape(
		state: BlockState,
		world: BlockGetter,
		pos: BlockPos,
		collisionContext: CollisionContext,
	): VoxelShape {
		
		return getFullShape(world.flexiShape(pos))
	}
	
	override fun getInteractionShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape {
		return getFullShape(level.flexiShape(pos))
	}
	
	private fun getFullShape(shape: FlexiShape): VoxelShape {
		return AllShapes.TRACK_FALLBACK // TODO
	}
	
	override fun getCollisionShape(
		state: BlockState,
		world: BlockGetter,
		pos: BlockPos,
		collisionContext: CollisionContext,
	): VoxelShape {
		val shape = world.flexiShape(pos)
		return when {
			shape.normal.x != 0.0 || shape.normal.y != 0.0 -> Shapes.empty()
			else -> AllShapes.TRACK_COLLISION
		}
	}
	
	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
		TODO()
	}
	
	override fun getBlockEntityClass(): Class<TrackBlockEntity> =
		@Suppress("UNCHECKED_CAST")
		(FlexiTrackBlockEntity::class.java as Class<TrackBlockEntity>)
	
	override fun getBlockEntityType(): BlockEntityType<out FlexiTrackBlockEntity> =
		TODO()
	
	override fun getUpNormal(world: BlockGetter, pos: BlockPos, state: BlockState): Vec3 =
		world.flexiShape(pos).normal
	
	override fun getTrackAxes(world: BlockGetter, pos: BlockPos, state: BlockState): List<Vec3> =
		world.flexiShape(pos).tangents
	
	override fun overlay(world: BlockGetter, pos: BlockPos, existing: BlockState, placed: BlockState): BlockState {
		TODO()
	}
	
	override fun rotate(state: BlockState, level: LevelAccessor, pos: BlockPos, direction: Rotation): BlockState {
		level.flexiState(pos).updateShape { it.rotate(direction) }
		return state
	}
	
	override fun rotate(state: BlockState, pRotation: Rotation): BlockState {
		throw UnsupportedOperationException("use rotate(BlockState, LevelAccessor, BlockPos, Rotation) variant")
	}
	
	override fun mirror(state: BlockState, pMirror: Mirror): BlockState {
		throw UnsupportedOperationException("TODO: mirror with BE?")
	}
	
	@OnlyIn(Dist.CLIENT)
	override fun <Self : Affine<Self>> prepareTrackOverlay(
		affine: Affine<Self>,
		world: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		bezierPoint: BezierTrackPointLocation?,
		direction: Direction.AxisDirection,
		type: RenderedTrackOverlayType,
	): PartialModel? {
		var axis: Vec3? = null
		var diff: Vec3? = null
		var normal: Vec3? = null
		
		val be = world.getBlockEntity(pos)
		
		if(bezierPoint != null && be is TrackBlockEntity) {
			val bc = be.connections.get(bezierPoint.curveTarget())
			if(bc != null) {
				val length = Mth.floor(bc.getLength() * 2).toDouble()
				val seg = bezierPoint.segment() + 1
				val t = seg / length
				val tPre = (seg - 1) / length
				val tPost = (seg + 1) / length
				
				val offset = bc.getPosition(t)
				normal = bc.getNormal(t)
				diff = bc.getPosition(tPost)
					.subtract(bc.getPosition(tPre))
					.normalize()
				
				affine.translate(offset.subtract(Vec3.atBottomCenterOf(pos)))
				affine.translate(0f, -4 / 16f, 0f)
			} else return null
		}
		
		if(normal == null) {
			axis = world.flexiShape(pos).axes[0].tangent
			diff = axis.scale(direction.step.toDouble())
				.normalize()
			normal = getUpNormal(world, pos, state)
		}
		
		val angles = TrackRenderer.getModelAngles(normal, diff)
		
		affine.center()
			.rotateY(angles.y.toFloat())
			.rotateX(angles.x.toFloat())
			.uncenter()
		
		if(axis != null) affine.translate(
			0f,
			if(axis.y != 0.0) 7 / 16f else 0f,
			if(axis.y != 0.0) direction.step * 2.5f / 16f else 0f
		)
		else {
			affine.translate(0f, 4 / 16f, 0f)
			if(direction == Direction.AxisDirection.NEGATIVE) affine.rotateCentered(Mth.PI, Direction.UP)
		}
		
		if(bezierPoint == null && be is TrackBlockEntity && be.isTilted) {
			var yOffset = 0.0
			for(bc in be.connections.values) {
				yOffset += bc.starts.getFirst().y - pos.y
			}
			
			affine.center()
				.rotateXDegrees((-direction.step * be.tilt.smoothingAngle.get()).toFloat())
				.uncenter()
				.translate(0.0, yOffset / 2, 0.0)
		}
		
		return when(type) {
			RenderedTrackOverlayType.DUAL_SIGNAL -> AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY
			RenderedTrackOverlayType.OBSERVER -> AllPartialModels.TRACK_OBSERVER_OVERLAY
			RenderedTrackOverlayType.SIGNAL -> AllPartialModels.TRACK_SIGNAL_OVERLAY
			RenderedTrackOverlayType.STATION -> AllPartialModels.TRACK_STATION_OVERLAY
		}
	}
}
