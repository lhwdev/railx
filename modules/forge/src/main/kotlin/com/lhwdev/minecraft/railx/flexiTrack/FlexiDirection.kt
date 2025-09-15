package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.utils.*
import net.createmod.catnip.math.VecHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import kotlin.math.*


interface FlexiDirection {
	/*
	* 2d integer representation of tangent, if available.
	*  Uses same coordinate system as minecraft; as y increases, it moves to south.
	*/
	class Tangent2(val x: Int, val y: Int)
	
	val tangent: Vec3
	val tangent2: Tangent2?
	val normal: Vec3
	
	infix fun closeTo(other: FlexiDirection): Boolean =
		tangent closeTo other.tangent && normal closeTo other.tangent
	
	fun mirror(by: Mirror): FlexiDirection
	
	fun rotate(by: Rotation): FlexiDirection
	
	/** rotates direction clockwise; which means index **decreases**. */
	fun rotateKnown(by: Int): FlexiDirection
	
	fun write(): CompoundTag
	
	object Zero : FlexiDirection {
		override val tangent: Vec3
			get() = Vec3.ZERO
		
		override val tangent2: Tangent2?
			get() = null
		
		override val normal: Vec3
			get() = Vec3(0.0, 1.0, 0.0)
		
		override fun mirror(by: Mirror): Zero = this
		override fun rotate(by: Rotation): Zero = this
		override fun rotateKnown(by: Int): Zero = this
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x0)
		}
		
		override fun toString(): String = "FlexiDirection.Zero"
	}
	
	abstract class Flat : Normalized {
		companion object {
			val normal = Vec3(0.0, 1.0, 0.0)
		}
		
		override val base: Flat
			get() = this
		
		final override val normal: Vec3
			get() = Flat.normal
		
		fun applyNormal(normal: Vec3): Normalized = if(normal.x == 0.0 && normal.z == 0.0) {
			this
		} else {
			NormalizedImpl(this, normal)
		}
		
		abstract override fun mirror(by: Mirror): Flat
		
		abstract override fun rotate(by: Rotation): Flat
		abstract override fun rotateKnown(by: Int): Flat
	}
	
	interface Normalized : FlexiDirection {
		val base: Flat
	}
	
	
	// As index increases, tangent goes counter-clockwise, starting from (1, 0).
	// BE AWARE THAT ANGLE IS CLOCKWISE OMG
	class Known(val index: Int) : Flat(), Normalized, Comparable<Known> {
		companion object {
			const val DivisionCount = 32
			val Divisions = (0 until DivisionCount).map { index -> Known(index) }
			
			fun roundFrom(radian: Double): Known {
				val index = DivisionCount * (-radian floorMod PI) / PI
				return Divisions[index.roundToInt() % DivisionCount]
			}
			
			fun roundFrom(vector: Vec3): Known =
				roundFrom(radian = atan2(-vector.z, vector.x))
			
			fun from(index: Int): Known {
				check(index >= 0) { "index < 0" }
				check(index < DivisionCount) { "index >= DivisionCount" }
				return Divisions[index]
			}
			
			fun read(tag: CompoundTag): Known =
				from(tag.getInt("Index"))
			
			fun readInt(tag: NumericTag): Known =
				from(tag.asInt)
		}
		
		val angle = PI * (index.toDouble() / DivisionCount)
		val angleDegree = 180 * (index.toDouble() / DivisionCount)
		
		override val tangent: Vec3 = Vec3(cos(angle), 0.0, sin(angle))
		
		override val tangent2: Tangent2?
			get() = if(index % (DivisionCount / 4) == 0) {
				when(index / (DivisionCount / 4)) {
					0 -> Tangent2(1, 0)
					1 -> Tangent2(1, 1)
					2 -> Tangent2(0, 1)
					3 -> Tangent2(-1, 0)
					else -> error("unreachable")
				}
			} else {
				null
			}
		
		override fun closeTo(other: FlexiDirection): Boolean {
			if(other is Known) return index == other.index
			return super<Flat>.closeTo(other)
		}
		
		override fun mirror(by: Mirror): Known = when(by) {
			Mirror.NONE -> this
			Mirror.LEFT_RIGHT -> Divisions[(DivisionCount - index) % DivisionCount]
			Mirror.FRONT_BACK -> Divisions[(2 * DivisionCount - index) % DivisionCount]
		}
		
		override fun rotate(by: Rotation): Known =
			Divisions[(2 * DivisionCount + index - by.ordinal * (DivisionCount / 2)) % DivisionCount]
		
		override fun rotateKnown(by: Int): Known =
			Divisions[(index - by) floorMod DivisionCount]
		
		override fun compareTo(other: Known): Int =
			index - other.index
		
		operator fun minus(other: Known): Int =
			index - other.index
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x1)
			tag.putInt("Index", index)
		}
		
		fun writeInt(): Tag = IntTag.valueOf(index)
		
		override fun toString(): String = "FlexiDirection.Known(index=$index)"
	}
	
	class NormalizedImpl(
		override val base: Flat,
		override val normal: Vec3,
		override val tangent: Vec3 = run {
			check(base.tangent.y == 0.0) { "base.tangent not flat" }
			check(base.tangent.isNormalized()) { "base.tangent is not normalized" }
			check(normal.isNormalized()) { "normal is not normalized" }
			
			/*
			Using Rodrigues' rotation formula:
			t=(a,0,b)
			n=(0,1,0), n'=(x,y,z)
			k = (z, 0, -x) / sqrt(x^2 + z^2) = (z, 0, -x) / L
			cos = y, sin = sqrt(1 - y^2) = L
			t' = t cos + (k x t) sin + k(k * t)(1 - cos)
			   = y * t + L (k x t) + (1 - y)(k * t) k
			   = (ay, 0, by) + (0, -xa-zb, 0) + (az-bx)(1/(1+y))(z, 0, -x)
			   = [ay + azTz - bxTz, -xa-zb, by -azTx +bxTx
			 */
			val a = base.tangent.x
			val b = base.tangent.z
			val x = normal.x
			val y = normal.y
			val z = normal.z
			
			val temp = (1 / (1 + y)) * (a * z - b * x)
			
			// surprisingly, this works even if y = 1
			Vec3(
				a * y + temp * z,
				-(a * x + b * z),
				b * y - temp * x,
			)
		},
	) : Normalized {
		companion object {
			fun read(tag: CompoundTag): NormalizedImpl = NormalizedImpl(
				base = tag.get("Base").let { base ->
					if(base is NumericTag) Known.readInt(base)
					FlexiDirection.read(base as CompoundTag) as Flat
				},
				normal = VecHelper.readNBT(tag.getList("Normal", Tag.TAG_DOUBLE.toInt()))
			)
		}
		
		override val tangent2: Tangent2?
			get() = if(normal.x == 0.0 && normal.z == 0.0) base.tangent2 else null
		
		override fun mirror(by: Mirror): NormalizedImpl = if(by != Mirror.NONE) {
			NormalizedImpl(base.mirror(by), by.mirror(normal), by.mirror(tangent))
		} else this
		
		override fun rotate(by: Rotation): NormalizedImpl = if(by != Rotation.NONE) {
			NormalizedImpl(base.rotate(by), by.rotate(normal), by.rotate(tangent))
		} else this
		
		override fun rotateKnown(by: Int): NormalizedImpl {
			val angle = by.toFloat() / Known.DivisionCount * PI.toFloat()
			return NormalizedImpl(base.rotateKnown(by), normal.yRot(angle), tangent.yRot(angle))
		}
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x10)
			tag.put("Base", if(base is Known) base.writeInt() else base.write())
			tag.put("Normal", VecHelper.writeNBT(normal))
		}
		
		override fun toString(): String = "FlexiDirection.NormalizedImpl(base=$base, normal=$normal)"
	}
	
	class Two(override val tangent: Vec3, override val normal: Vec3) : FlexiDirection {
		init {
			require(tangent.isNormalized()) { "tangent.length != 1" }
			require(normal.isNormalized()) { "normal.length != 1" }
			require(tangent.dot(normal) similarTo 0.0) { "tangent is not perpendicular to normal" }
		}
		
		override val tangent2: Tangent2?
			get() = null
		
		override fun mirror(by: Mirror): FlexiDirection = Two(by.mirror(tangent), by.mirror(normal))
		override fun rotate(by: Rotation): FlexiDirection = Two(by.rotate(tangent), by.rotate(normal))
		override fun rotateKnown(by: Int): FlexiDirection {
			val angle = by.toFloat() / Known.DivisionCount * PI.toFloat()
			return Two(tangent.yRot(angle), normal.yRot(angle))
		}
		
		override fun write(): CompoundTag {
			TODO("Not yet implemented")
		}
	}
	
	
	companion object {
		fun read(tag: CompoundTag): FlexiDirection = when(tag.getByte("Type").toInt()) {
			0x0 -> Zero
			0x1 -> Known.read(tag)
			0x10 -> NormalizedImpl.read(tag)
			else -> error("unexpected type ${tag.getByte("Type")}")
		}
	}
}


val FlexiDirection.tangentAngle: Double
	get() = tangent2?.let { atan2(-it.y.toDouble(), it.x.toDouble()) } ?: atan2(-tangent.z, tangent.x)
