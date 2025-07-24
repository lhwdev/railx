package com.lhwdev.minecraft.railx.ccAdvanced.advancedTrackObserver

import com.simibubi.create.content.trains.entity.Train
import net.minecraft.nbt.CompoundTag
import org.apache.commons.io.input.ReaderInputStream
import org.squiddev.cobalt.LuaState
import org.squiddev.cobalt.LuaTable
import org.squiddev.cobalt.compiler.LoadState
import org.squiddev.cobalt.function.Dispatch
import java.io.StringReader


open class AdvancedRule {
	var code: String = ""
	
	
	fun read(tag: CompoundTag) {
		code = tag.getString("Code")
	}
	
	fun write(): CompoundTag = CompoundTag().also { tag ->
		if(code.length <= 512) {
			tag.putString("Code", code)
		} else {
			tag.putString("Code", "")
		}
	}
}


class AdvancedLuaRule : AdvancedRule() {
	val state = LuaState()
	
	fun test(train: Train): Boolean {
		val env = LuaTable()
		val expression = LoadState.load(state, ReaderInputStream.builder().setReader(StringReader(code)).get(), "", env)
		return Dispatch.call(state, expression).toBoolean()
	}
}
