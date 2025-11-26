package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.lhwdev.minecraft.railx.registry.AllBlockEntityTypes
import com.simibubi.create.content.trains.observer.TrackObserverBlock
import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity
import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import thedarkcolour.kotlinforforge.forge.runWhenOn


class AdvancedTrackObserverBlock(properties: BlockBehaviour.Properties) : TrackObserverBlock(properties) {
	override fun use(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		player: Player,
		hand: InteractionHand,
		hitResult: BlockHitResult,
	): InteractionResult {
		runWhenOn(Dist.CLIENT) {
			val be = getBlockEntity(level, pos) as AdvancedTrackObserverBlockEntity
			displayScreen(be, player)
		}
		return InteractionResult.SUCCESS
	}
	
	@OnlyIn(Dist.CLIENT)
	private fun displayScreen(be: AdvancedTrackObserverBlockEntity, player: Player) {
		if(player !is LocalPlayer) return
		ScreenOpener.open(ObserverConfigureScreen(be))
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun getBlockEntityClass(): Class<TrackObserverBlockEntity> =
		AdvancedTrackObserverBlockEntity::class.java as Class<TrackObserverBlockEntity>
	
	override fun getBlockEntityType(): BlockEntityType<out TrackObserverBlockEntity> =
		AllBlockEntityTypes.AdvancedTrackObserver.get()
}
