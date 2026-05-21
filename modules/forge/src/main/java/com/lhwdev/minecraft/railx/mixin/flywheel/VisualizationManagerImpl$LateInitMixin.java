package com.lhwdev.minecraft.railx.mixin.flywheel;


import com.lhwdev.minecraft.railx.flywheel.VisualizationManagerContainer;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.impl.task.RaisePlan;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.lib.task.IfElsePlan;
import dev.engine_room.flywheel.lib.task.MapContextPlan;
import dev.engine_room.flywheel.lib.task.NestedPlan;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.task.functional.RunnableWithContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;


@Mixin(targets = "dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl$LateInit")
public abstract class VisualizationManagerImpl$LateInitMixin {
	@Final
	@Shadow
	VisualizationManagerImpl this$0;
	
	@Final
	@Shadow
	private Engine engine;
	
	@Mutable
	@Final
	@Shadow
	private Plan<RenderContext> framePlan;
	
	@Mutable
	@Final
	@Shadow
	private Plan<TickableVisual.Context> tickPlan;
	
	@Shadow
	protected abstract DynamicVisual.Context createVisualFrameContext(RenderContext ctx);
	
	
	@Unique
	private Stream<VisualManagerImpl<?, ?>> railx$m() {
		return ((VisualizationManagerContainer) this$0).getManagers().getAll().stream();
	}
	
	
	// effectively (mostly) overwriting the constructor
	@Inject(method = "<init>", at = @At("RETURN"))
	void onInitialize(VisualizationManagerImpl $this, LevelAccessor level, CallbackInfo ci) {
		var self = (VisualizationManagerContainer) $this;
		
		// this is not that heavy; see the source
		var visualizationContext = engine.createVisualizationContext();
		self.getManagers().attachVisualizationContext(visualizationContext);
		
		var recreate = SimplePlan.of(railx$m().<RunnableWithContext<RenderContext>>map(
			manager ->
				context -> manager.getStorage().recreateAll(visualizationContext, context.partialTick())
		).toList());
		
		var update = MapContextPlan.map(this::createVisualFrameContext)
			.to(new NestedPlan<>(
				railx$m().map(manager -> manager.framePlan(visualizationContext)).toList()
			));
		
		framePlan = IfElsePlan.on((RenderContext ctx) -> engine.updateRenderOrigin(ctx.camera()))
			.ifTrue(recreate)
			.ifFalse(update)
			.plan()
			.then(SimplePlan.of(() -> {
				if(railx$m().anyMatch(VisualManagerImpl::areGpuLightSectionsDirty)) {
					var out = new LongOpenHashSet();
					railx$m().forEach(manager -> out.addAll(manager.gpuLightSections()));
					engine.lightSections(out);
				}
			}))
			.then(engine.createFramePlan())
			.then(RaisePlan.raise(self.getFrameFlag()));
		
		tickPlan = new NestedPlan<>(railx$m().map(manager -> manager.tickPlan(visualizationContext)).toList())
			.then(RaisePlan.raise(self.getTickFlag()));
	}
}
