package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.simibubi.create.content.trains.track.TrackBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = TrackBlockEntity.class, remap = false)
public interface TrackBlockEntityAccessor {
	@Accessor
	boolean getCancelDrops();
	
	@Accessor
	void setCancelDrops(boolean value);
}
