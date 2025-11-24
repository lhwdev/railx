package com.lhwdev.minecraft.railx.utils

import io.netty.buffer.ByteBuf
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.StreamDecoder
import net.minecraft.network.codec.StreamEncoder


object StreamCodecs {
	fun <B : FriendlyByteBuf, V : Any> nullable(base: StreamCodec<in ByteBuf, V>): StreamCodec<B, V?> =
		@Suppress("UNCHECKED_CAST", "WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
		object : StreamCodec<B, V?> {
			override fun decode(buffer: B): V? =
				buffer.readNullable(base as StreamDecoder<in FriendlyByteBuf, V?>)
			
			override fun encode(buffer: B, value: V?) {
				buffer.writeNullable(value, base as StreamEncoder<in FriendlyByteBuf, V?>)
			}
		}
	
	fun <T : Enum<T>> enum(type: Class<T>): StreamCodec<in FriendlyByteBuf, T> =
		object : StreamCodec<FriendlyByteBuf, T> {
			override fun decode(buffer: FriendlyByteBuf): T =
				buffer.readEnum(type)
			
			override fun encode(buffer: FriendlyByteBuf, value: T) {
				buffer.writeEnum(value)
			}
		}
	
	inline fun <reified T : Enum<T>> enum(): StreamCodec<in FriendlyByteBuf, T> =
		enum(T::class.java)
}
