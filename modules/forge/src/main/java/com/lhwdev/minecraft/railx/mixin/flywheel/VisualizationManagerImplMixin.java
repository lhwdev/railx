package com.lhwdev.minecraft.railx.mixin.flywheel;


import com.lhwdev.minecraft.railx.flywheel.VisualManagers;
import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.impl.task.Flag;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.engine_room.flywheel.impl.visualization.storage.EffectStorage;
import dev.engine_room.flywheel.impl.visualization.storage.EntityStorage;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(VisualizationManagerImpl.class)
@Implements(@Interface(iface = VisualizationManagerContainer.class, prefix = "accessor$"))
public abstract class VisualizationManagerImplMixin {
	@Unique
	private VisualManagers railx$managers;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(LevelAccessor level, CallbackInfo ci) {
		railx$managers = new VisualManagers((VisualizationManagerContainer) this);
	}
	
	
	@Accessor("effects")
	public abstract @NotNull VisualManagerImpl<@NotNull Effect, @NotNull EffectStorage> accessor$getEffects();
	
	@Accessor("entities")
	public abstract @NotNull VisualManagerImpl<@NotNull Entity, @NotNull EntityStorage> accessor$getEntities();
	
	@Accessor("blockEntities")
	public abstract @NotNull VisualManagerImpl<@NotNull BlockEntity, @NotNull BlockEntityStorage> accessor$getBlockEntities();
	
	@Accessor("frameFlag")
	public abstract @NotNull Flag accessor$getFrameFlag();
	
	@Accessor("tickFlag")
	public abstract @NotNull Flag accessor$getTickFlag();
	
	public @NotNull VisualManagers accessor$getManagers() {
		return railx$managers;
	}
	
	
	@Inject(method = "onLightUpdate", at = @At("RETURN"))
	void onLightUpdate(SectionPos sectionPos, LightLayer layer, CallbackInfo ci) {
		var longPos = sectionPos.asLong();
		for(var manager : railx$managers.getValue()) {
			manager.onLightUpdate(longPos);
		}
	}
	
	@Inject(method = "delete", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/impl/visualization" +
		"/VisualManagerImpl;invalidate()V"))
	void onDelete(CallbackInfo ci) {
		for(var manager : railx$managers.getValue()) {
			manager.invalidate();
		}
	}
}
