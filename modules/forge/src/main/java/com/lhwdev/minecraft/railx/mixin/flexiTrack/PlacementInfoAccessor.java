package com.lhwdev.minecraft.railx.mixin.flexiTrack;


import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(value = TrackPlacement.PlacementInfo.class, remap = false)
public interface PlacementInfoAccessor {
	@Accessor
	int getRequiredTracks();
	
	@Accessor
	void setRequiredTracks(int requiredTracks);
	
	@Accessor
	boolean getHasRequiredTracks();
	
	@Accessor
	void setHasRequiredTracks(boolean hasRequiredTracks);
	
	@Accessor
	int getRequiredPavement();
	
	@Accessor
	void setRequiredPavement(int requiredPavement);
	
	@Accessor
	boolean getHasRequiredPavement();
	
	@Accessor
	void setHasRequiredPavement(boolean hasRequiredPavement);
	
	@Accessor
	TrackMaterial getTrackMaterial();
	
	@Invoker
	TrackPlacement.PlacementInfo callWithMessage(String message);
	
	@Invoker
	TrackPlacement.PlacementInfo callTooJumbly();
	
	@Accessor
	BezierConnection getCurve();
	
	@Accessor
	void setCurve(BezierConnection curve);
	
	@Accessor
	boolean getValid();
	
	@Accessor
	void setValid(boolean valid);
	
	@Accessor
	int getEnd1Extent();
	
	@Accessor
	void setEnd1Extent(int end1Extent);
	
	@Accessor
	int getEnd2Extent();
	
	@Accessor
	void setEnd2Extent(int end2Extent);
	
	@Accessor
	String getMessage();
	
	@Accessor
	void setMessage(String message);
	
	@Accessor
	Vec3 getEnd1();
	
	@Accessor
	void setEnd1(Vec3 end1);
	
	@Accessor
	Vec3 getEnd2();
	
	@Accessor
	void setEnd2(Vec3 end2);
	
	@Accessor
	Vec3 getNormal1();
	
	@Accessor
	void setNormal1(Vec3 normal1);
	
	@Accessor
	Vec3 getNormal2();
	
	@Accessor
	void setNormal2(Vec3 normal2);
	
	@Accessor
	Vec3 getAxis1();
	
	@Accessor
	void setAxis1(Vec3 axis1);
	
	@Accessor
	Vec3 getAxis2();
	
	@Accessor
	void setAxis2(Vec3 axis2);
	
	@Accessor
	BlockPos getPos1();
	
	@Accessor
	void setPos1(BlockPos pos1);
	
	@Accessor
	BlockPos getPos2();
	
	@Accessor
	void setPos2(BlockPos pos2);
}
