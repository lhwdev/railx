@file:JvmName("ReservedSignalNixieTube")

package com.lhwdev.minecraft.railx.common

import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity
import com.simibubi.create.content.trains.signal.SignalBlockEntity
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.world.level.block.state.BlockState
import java.lang.invoke.MethodHandles
import java.lang.ref.WeakReference


private val NixieTubeBlockEntity_cachedSignalTE = NixieTubeBlockEntity::class.java
	.getDeclaredField("cachedSignalTE")
	.also { it.isAccessible = true }
	.let { MethodHandles.lookup().unreflectGetter(it) }

val NixieTubeBlockEntity.cachedSignalTE: SignalBlockEntity?
	@Suppress("UNCHECKED_CAST")
	get() = (NixieTubeBlockEntity_cachedSignalTE.invokeExact(this) as WeakReference<SignalBlockEntity>).get()


@Suppress("PropertyName")
interface ReservedAwareSignalBlockEntity {
	val `railx$isReservedBySelf`: Boolean
}

val SignalBlockEntity.isReservedBySelf: Boolean
	get() = (this as ReservedAwareSignalBlockEntity).`railx$isReservedBySelf`


object ReservedSignalNixieTubeRenderer {
	fun partialModelBase(
		be: NixieTubeBlockEntity,
		partial: PartialModel,
		referenceState: BlockState,
	): SuperByteBuffer? {
		val signalBe = be.cachedSignalTE ?: return null
		if(!signalBe.isReservedBySelf) return null
		
		val model = when(partial) {
			AllPartialModels.SIGNAL_RED_GLOW,
			AllPartialModels.SIGNAL_YELLOW_GLOW,
			AllPartialModels.SIGNAL_WHITE_GLOW,
				-> AllPartialModels.SIGNAL_WHITE_GLOW
			
			AllPartialModels.SIGNAL_RED,
			AllPartialModels.SIGNAL_YELLOW,
			AllPartialModels.SIGNAL_WHITE,
				-> AllPartialModels.SIGNAL_WHITE
			
			else -> return null
		}
		return CachedBuffers.partial(model, referenceState)
			.color(0x33ff99)
	}
}
