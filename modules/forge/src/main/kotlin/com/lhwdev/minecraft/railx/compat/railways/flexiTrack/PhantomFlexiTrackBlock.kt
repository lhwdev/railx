package com.lhwdev.minecraft.railx.compat.railways.flexiTrack

import com.railwayteam.railways.content.custom_tracks.phantom.PhantomSpriteManager
import com.simibubi.create.content.trains.track.BezierTrackPointLocation
import com.simibubi.create.content.trains.track.TrackMaterial
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.transform.Affine
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState


class PhantomFlexiTrackBlock(properties: Properties, material: TrackMaterial) :
	UniversalFlexiTrackBlock(properties, material) {
	override fun <Self : Affine<Self>> prepareTrackOverlay(
		affine: Affine<Self>,
		world: BlockGetter,
		pos: BlockPos,
		state: BlockState,
		bezierPoint: BezierTrackPointLocation?,
		direction: Direction.AxisDirection,
		type: TrackTargetingBehaviour.RenderedTrackOverlayType,
	): PartialModel? {
		if(bezierPoint == null && !PhantomSpriteManager.isVisible()) return null
		return super.prepareTrackOverlay(affine, world, pos, state, bezierPoint, direction, type)
	}
}
