package com.lhwdev.minecraft.railx.mixin.flywheel;


import com.google.common.collect.ImmutableList;
import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.lib.task.NestedPlan;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.task.functional.RunnableWithContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;


@Mixin(targets = "dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl$LateInit")
public abstract class VisualizationManagerImpl$LateInitMixin {
	@Final
	@Shadow
	VisualizationManagerImpl this$0;
	
	
	@Unique
	private Collection<VisualManagerImpl<?, ?>> railx$containerManagers() {
		return ((VisualizationManagerContainer) this$0).getManagers().getValue();
	}
	
	
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/api" +
		"/backend/Engine;createVisualizationContext()Ldev/engine_room/flywheel/api/visualization/VisualizationContext;"
		, ordinal = 0))
	VisualizationContext onCreateVisualizationContext(VisualizationContext original) {
		((VisualizationManagerContainer) this$0).getManagers()
			.attachVisualizationContext(original);
		
		return original;
	}
	
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib" +
		"/task/SimplePlan;of([Ldev/engine_room/flywheel/lib/task/functional/RunnableWithContext;)" +
		"Ldev/engine_room/flywheel/lib/task/SimplePlan;", ordinal = 0))
	SimplePlan<RenderContext> addManagersToRecreatePlan(
		SimplePlan<RenderContext> original,
		@Local(ordinal = 0) VisualizationContext visualizationContext
	) {
		var containerManagers = railx$containerManagers();
		if(containerManagers.isEmpty()) return original;
		
		var tasks = containerManagers.stream()
			.map((manager) -> {
				RunnableWithContext<RenderContext> task = (context) -> manager.getStorage().recreateAll(
					visualizationContext,
					context.partialTick()
				);
				return task;
			})
			.iterator();
		
		return SimplePlan.of(ImmutableList.<RunnableWithContext<RenderContext>>builder()
			.addAll(original.parallelTasks())
			.addAll(tasks)
			.build());
	}
	
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib" +
		"/task/NestedPlan;of([Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/NestedPlan;"
		, ordinal = 0))
	NestedPlan<DynamicVisual.Context> addManagerToUpdatePlan(
		NestedPlan<DynamicVisual.Context> original,
		@Local(ordinal = 0) VisualizationContext visualizationContext
	) {
		var containerManagers = railx$containerManagers();
		if(containerManagers.isEmpty()) return original;
		
		var plans = containerManagers.stream()
			.map((manager) -> manager.framePlan(visualizationContext))
			.iterator();
		
		return new NestedPlan<>(ImmutableList.<Plan<DynamicVisual.Context>>builder()
			.addAll(original.parallelPlans())
			.addAll(plans)
			.build());
	}
	
	@ModifyExpressionValue(method = "lambda$new$4", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel" +
		"/impl/visualization/VisualManagerImpl;areGpuLightSectionsDirty()Z", ordinal = 0))
	boolean areGpuLightSectionsDirtyOrManagers(boolean original) {
		if(original) return true;
		
		return railx$containerManagers()
			.stream()
			.anyMatch(VisualManagerImpl::areGpuLightSectionsDirty);
	}
	
	@Inject(method = "lambda$new$4", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/api/backend" +
		"/Engine;lightSections(Lit/unimi/dsi/fastutil/longs/LongSet;)V"))
	void addManagerBeforeSetLightSections(CallbackInfo ci, @Local(ordinal = 0) LongOpenHashSet out) {
		for(var manager : railx$containerManagers()) {
			out.addAll(manager.gpuLightSections());
		}
	}
	
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib" +
		"/task/NestedPlan;of([Ldev/engine_room/flywheel/api/task/Plan;)Ldev/engine_room/flywheel/lib/task/NestedPlan;"
		, ordinal = 1))
	NestedPlan<TickableVisual.Context> addManagerToTickPlan(
		NestedPlan<TickableVisual.Context> original,
		@Local(ordinal = 0) VisualizationContext visualizationContext
	) {
		var containerManagers = railx$containerManagers();
		if(containerManagers.isEmpty()) return original;
		
		var plans = containerManagers.stream()
			.map((manager) -> manager.tickPlan(visualizationContext))
			.iterator();
		
		return new NestedPlan<>(ImmutableList.<Plan<TickableVisual.Context>>builder()
			.addAll(original.parallelPlans())
			.addAll(plans)
			.build());
	}
}
