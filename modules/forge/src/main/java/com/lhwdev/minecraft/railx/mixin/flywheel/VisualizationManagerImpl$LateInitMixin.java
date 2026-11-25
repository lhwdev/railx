package com.lhwdev.minecraft.railx.mixin.flywheel;


import com.google.common.collect.ImmutableList;
import com.lhwdev.minecraft.railx.flywheel.VisualManagerExtension;
import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.lib.task.IfElsePlan;
import dev.engine_room.flywheel.lib.task.MapContextPlan;
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
	
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/task" +
		"/IfElsePlan$Builder;ifTrue(Ldev/engine_room/flywheel/api/task/Plan;)" +
		"Ldev/engine_room/flywheel/lib/task/IfElsePlan$Builder;", ordinal = 0))
	IfElsePlan.Builder<RenderContext> addManagersToRecreatePlan(
		IfElsePlan.Builder<RenderContext> builder,
		Plan<RenderContext> onTrue,
		Operation<IfElsePlan.Builder<RenderContext>> original,
		@Local(ordinal = 0) VisualizationContext visualizationContext
	) {
		var containerManagers = railx$containerManagers();
		if(containerManagers.isEmpty()) return original.call(builder, onTrue);
		
		var tasks = containerManagers.stream().<RunnableWithContext<RenderContext>>map(
			manager -> context -> {
				if(manager instanceof VisualManagerExtension extension) {
					extension.recreate(visualizationContext, context.partialTick());
				} else {
					manager.getStorage().recreateAll(visualizationContext, context.partialTick());
				}
			}
		).toList();
		
		return original.call(builder, onTrue.then(SimplePlan.of(tasks)));
	}
	
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/task" +
		"/MapContextPlan$Builder;to(Ldev/engine_room/flywheel/api/task/Plan;)" +
		"Ldev/engine_room/flywheel/lib/task/MapContextPlan;", ordinal = 0))
	MapContextPlan<RenderContext, DynamicVisual.Context> addManagerToUpdatePlan(
		MapContextPlan.Builder<RenderContext, DynamicVisual.Context> builder,
		Plan<DynamicVisual.Context> plan,
		Operation<MapContextPlan<RenderContext, DynamicVisual.Context>> original,
		@Local(ordinal = 0) VisualizationContext visualizationContext
	) {
		var containerManagers = railx$containerManagers();
		if(containerManagers.isEmpty()) return original.call(builder, plan);
		
		var plans = containerManagers.stream()
			.map((manager) -> manager.framePlan(visualizationContext))
			.toList();
		
		return original.call(builder, plan.then(new NestedPlan<>(plans)));
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
