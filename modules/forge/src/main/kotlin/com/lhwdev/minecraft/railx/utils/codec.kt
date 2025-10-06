package com.lhwdev.minecraft.railx.utils

import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder


fun <O> PrimitiveCodec<String>.constantField(key: String, value: String): RecordCodecBuilder<O, String> = validate {
	if(it == value) {
		DataResult.success(it)
	} else {
		DataResult.error { "value not equals to defined constant" }
	}
}.fieldOf(key).forGetter { value }
