package com.lhwdev.minecraft.railx.buildTrack

import com.lhwdev.minecraft.railx.registry.AllDataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level


class TrackPlanItem(properties: Properties) : Item(properties) {
	companion object {
		fun getPlan(stack: ItemStack): TrackPlan =
			stack[AllDataComponents.TrackBuildPlan] ?: DummyTrackPlan
	}
	
	override fun getName(stack: ItemStack): Component =
		getPlan(stack).name
	
	override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
		
		
		return super.use(level, player, usedHand)
	}
}
