package com.lhwdev.minecraft.railx.flexiTrack

import com.lhwdev.minecraft.railx.flexiTrack.FlexiDirection.Known.Companion.DivisionCount
import com.lhwdev.minecraft.railx.utils.*
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.times
import thedarkcolour.kotlinforforge.neoforge.forge.vectorutil.v3d.unaryMinus
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
	
	infix fun closeToUnsigned(other: FlexiDirection): Boolean =
		tangent closeToUnsigned other.tangent && normal closeToUnsigned other.normal
	
	fun mirror(by: Mirror): FlexiDirection
	
	fun rotate(by: Rotation): FlexiDirection
	
	fun rotateKnown(by: Int): FlexiDirection
	
	fun applyNormal(normal: Vec3): FlexiDirection
	
	fun optimize(): FlexiDirection = this
	
	
	operator fun unaryMinus(): Signed = Two(-tangent, normal)
	
	
	fun write(): CompoundTag
	
	
	interface Signed : FlexiDirection {
		override fun unaryMinus(): Signed = Two(-tangent, normal)
	}
	
	object Zero : FlexiDirection, Signed {
		override val tangent: Vec3
			get() = Vec3.ZERO
		
		override val tangent2: Tangent2?
			get() = null
		
		override val normal: Vec3
			get() = Vec3(0.0, 1.0, 0.0)
		
		
		override fun mirror(by: Mirror): Zero = this
		override fun rotate(by: Rotation): Zero = this
		override fun rotateKnown(by: Int): Zero = this
		override fun unaryMinus(): Zero = this
		
		override fun applyNormal(normal: Vec3): Zero = this
		
		override fun optimize(): Zero = Zero
		
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
		
		abstract override fun mirror(by: Mirror): Flat
		
		abstract override fun rotate(by: Rotation): Flat
		abstract override fun rotateKnown(by: Int): Flat
		
		override fun applyNormal(normal: Vec3): Normalized = if(normal.x == 0.0 && normal.z == 0.0) {
			this
		} else {
			NormalizedImpl(this, normal)
		}
		
		override fun optimize(): Flat = this
	}
	
	interface Normalized : FlexiDirection {
		val base: Flat
		
		override fun optimize(): Normalized
	}
	
	
	// As index increases, tangent goes counter-clockwise, starting from (1, 0).
	class Known(val index: Int, val ordinal: Int) : Flat(), Normalized, Comparable<Known> {
		companion object {
			private const val DivisionCountBase = 32
			private const val DivisionRepeat = 1
			const val DivisionCount = DivisionCountBase * (1 shl DivisionRepeat)
			internal val DivisionsByOrdinal: Array<Known>
			internal val DivisionsByIndex: Array<Known>
			
			// DivisionRepeat =    0     // 1            // 2                           // 3
			//                     0        0      1        0      1       2               0       1        2                3
			// DivisionCountBase = <--->
			//                     0 1 2 // 0 2 4  1 3 5 // 0 4 8  2 6 10  1 3 5 7 9 11 // 0 8 16  4 12 20  2 6 10 14 18 22  1 3 5 7 9 11 13 15 17 19 21 23
			init {
				val byOrdinal = arrayOfNulls<Known>(DivisionCount)
				val byIndex = arrayOfNulls<Known>(DivisionCount)
				
				var index = 0
				for(level in 0..DivisionRepeat) {
					val scale = 1 shl (DivisionRepeat - level)
					val ordinalOffset = if(level == 0) 0 else scale
					val gap = 1 shl (if(level == 0) DivisionRepeat else DivisionRepeat + 1 - level)
					for(offset in 0..<DivisionCountBase * max(1 shl (level - 1), 1)) {
						val ordinal = ordinalOffset + offset * gap
						val known = Known(index, ordinal)
						byOrdinal[ordinal] = known
						byIndex[index++] = known
					}
				}
				
				DivisionsByOrdinal = byOrdinal.requireNoNulls()
				DivisionsByIndex = byIndex.requireNoNulls()
			}
			
			fun roundFrom(radian: Double): Known {
				val index = DivisionCount * (-radian floorMod PI) / PI
				return DivisionsByOrdinal[index.roundToInt() % DivisionCount]
			}
			
			fun roundFrom(vector: Vec3): Known =
				roundFrom(radian = Mth.atan2(-vector.z, vector.x))
			
			fun fromIndex(index: Int): Known {
				check(index >= 0) { "index < 0" }
				if(index >= DivisionCount) {
					TODO("index -> ordinal then approach to nearest ordinal: $index")
				}
				return DivisionsByIndex[index]
			}
			
			fun read(tag: CompoundTag): Known =
				fromIndex(tag.getInt("Index"))
			
			fun readInt(tag: NumericTag): Known =
				fromIndex(tag.asInt)
		}
		
		val angle = PI * (ordinal.toDouble() / DivisionCount)
		val angleDegree get() = 180 * (ordinal.toDouble() / DivisionCount)
		
		override val tangent: KnownVec3 = KnownVec3(this)
		
		override val tangent2: Tangent2?
			get() = if(ordinal % (DivisionCount / 4) == 0) {
				when(ordinal / (DivisionCount / 4)) {
					0 -> Tangent2(1, 0)
					1 -> Tangent2(1, 1)
					2 -> Tangent2(0, 1)
					3 -> Tangent2(-1, 0)
					else -> error("unreachable")
				}
			} else {
				null
			}
		
		override fun closeToUnsigned(other: FlexiDirection): Boolean {
			if(other is Known) return index == other.index
			return super<Flat>.closeToUnsigned(other)
		}
		
		override fun mirror(by: Mirror): Known = when(by) {
			Mirror.NONE -> this
			Mirror.LEFT_RIGHT -> DivisionsByOrdinal[(DivisionCount - ordinal) % DivisionCount]
			Mirror.FRONT_BACK -> DivisionsByOrdinal[(2 * DivisionCount - ordinal) % DivisionCount]
		}
		
		override fun rotate(by: Rotation): Known =
			DivisionsByOrdinal[(2 * DivisionCount + ordinal - by.ordinal * (DivisionCount / 2)) % DivisionCount]
		
		override fun rotateKnown(by: Int): Known =
			DivisionsByOrdinal[(ordinal - by) floorMod DivisionCount]
		
		override fun optimize(): Known = this
		
		override fun unaryMinus(): SignedKnown =
			SignedKnown(this, sign = Direction.AxisDirection.NEGATIVE)
		
		override fun compareTo(other: Known): Int =
			ordinal - other.ordinal
		
		operator fun minus(other: Known): Int =
			ordinal - other.ordinal
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x1)
			tag.putInt("Index", index)
		}
		
		fun writeInt(): Tag = IntTag.valueOf(index)
		
		override fun toString(): String = "FlexiDirection.Known(index=$index, ordinal=$ordinal)"
	}
	
	class KnownVec3(val known: Known) : Vec3(cos(known.angle), 0.0, sin(known.angle))
	
	class SignedKnown(val from: Known, val sign: Direction.AxisDirection) : Flat(), Signed {
		companion object {
			fun roundFrom(radian: Double): SignedKnown {
				val PI2 = PI * 2
				val index = (DivisionCount * (-radian floorMod PI2) / PI2).roundToInt()
				return SignedKnown(
					from = Known.DivisionsByOrdinal[index % DivisionCount],
					sign = if(index >= DivisionCount) Direction.AxisDirection.NEGATIVE else Direction.AxisDirection.POSITIVE,
				)
			}
			
			fun roundFrom(vector: Vec3): SignedKnown =
				roundFrom(radian = Mth.atan2(-vector.z, vector.x))
		}
		
		override val tangent: Vec3 = from.tangent * sign.step.toDouble()
		override val tangent2: Tangent2?
			get() = from.tangent2?.let { if(sign == Direction.AxisDirection.POSITIVE) it else Tangent2(-it.x, -it.y) }
		
		override fun mirror(by: Mirror): SignedKnown = TODO()
		override fun rotate(by: Rotation): SignedKnown = TODO()
		override fun rotateKnown(by: Int): SignedKnown = TODO()
		override fun optimize(): SignedKnown = this
		override fun unaryMinus(): SignedKnown = SignedKnown(from, sign.opposite())
		
		override fun write(): CompoundTag {
			TODO("Not yet implemented")
		}
	}
	
	
	class FlatImpl(override val tangent: Vec3) : Flat(), Signed {
		override fun mirror(by: Mirror): FlatImpl = FlatImpl(by.mirror(tangent))
		override fun rotate(by: Rotation): FlatImpl = FlatImpl(by.rotate(tangent))
		override fun rotateKnown(by: Int): Flat =
			FlatImpl(tangent.yRot(by.toFloat() / Known.DivisionCount * PI.toFloat())).optimize()
		
		override fun optimize(): Flat {
			tangent.asKnownVec3()?.let { return it.known }
			val tangent = tangent.optimize()
			if(tangent != this.tangent) return FlatImpl(tangent)
			return this
		}
		
		override fun unaryMinus(): FlatImpl = FlatImpl(-tangent)
		
		override val tangent2: Tangent2? get() = null
		
		override fun write(): CompoundTag {
			TODO("Not yet implemented")
		}
		
		override fun toString(): String = "FlexiDirection.FlatImpl(tangent=$tangent)"
	}
	
	
	class NormalizedImpl(
		override val base: Flat,
		override val normal: Vec3,
		override val tangent: Vec3 = run {
			check(base.tangent.y similarTo 0.0) { "base.tangent not flat" }
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
	) : Normalized, Signed {
		companion object {
			fun read(tag: CompoundTag): NormalizedImpl = NormalizedImpl(
				base = tag.get("Base").let { base ->
					if(base is NumericTag) Known.readInt(base)
					else FlexiDirection.read(base as CompoundTag) as Flat
				},
				normal = VecHelper.readNBT(tag.getList("Normal", Tag.TAG_DOUBLE.toInt()))
			)
		}
		
		override val tangent2: Tangent2?
			get() = if(normal.x similarTo 0.0 && normal.z similarTo 0.0) base.tangent2 else null
		
		override fun mirror(by: Mirror): NormalizedImpl = if(by != Mirror.NONE) {
			NormalizedImpl(base.mirror(by), by.mirror(normal), by.mirror(tangent))
		} else this
		
		override fun rotate(by: Rotation): NormalizedImpl = if(by != Rotation.NONE) {
			NormalizedImpl(base.rotate(by), by.rotate(normal), by.rotate(tangent))
		} else this
		
		override fun rotateKnown(by: Int): Normalized {
			val angle = by.toFloat() / Known.DivisionCount * PI.toFloat()
			return NormalizedImpl(base.rotateKnown(by), normal.yRot(angle), tangent.yRot(angle))
				.optimize()
		}
		
		override fun applyNormal(normal: Vec3): NormalizedImpl = NormalizedImpl(base, normal)
		
		override fun optimize(): Normalized {
			val base = base.optimize()
			if(normal.x similarTo 0.0 && normal.z similarTo 0.0) return base
			if(base === this.base) return NormalizedImpl(base, normal.optimize(), tangent)
			return NormalizedImpl(base, normal.optimize(), tangent.optimize())
		}
		
		override fun unaryMinus(): NormalizedImpl = NormalizedImpl(-base as Flat, normal, -tangent)
		
		override fun write(): CompoundTag = CompoundTag().also { tag ->
			tag.putByte("Type", 0x10)
			tag.put("Base", if(base is Known) base.writeInt() else base.write())
			tag.put("Normal", VecHelper.writeNBT(normal))
		}
		
		override fun toString(): String = "FlexiDirection.NormalizedImpl(base=$base, normal=$normal)"
	}
	
	class Two(override val tangent: Vec3, override val normal: Vec3) : FlexiDirection, Signed {
		init {
			require(tangent.isNormalized()) { "tangent.length != 1" }
			require(normal.isNormalized()) { "normal.length != 1" }
			// require(tangent.dot(normal) similarTo 0.0) { "tangent is not perpendicular to normal" }
		}
		
		
		/*
			Two(tangent=(i,j,k), normal=(a,b,c)) -> NormalizedImpl(base=(x,0,y), normal=(a,b,c))
			* ia + jb + kc = 0, ii+jj+kk=1, aa+bb+cc=1, T=(xc-ya)/d -> b+1 = d
			i = xb + Tc, (i-xb)(1+b) = c(xc-ya),  i + ib - xb - xbb = xcc - yac, x(b + bb + cc) - yac = i + ib
			k = yb - Ta, (k-yb)(1+b) = -a(xc-ya), k + kb - yb - ybb = yaa  -xac, -xac + y(aa + b + bb) = k + kb
			// -j = xa + yc
			x(d - aa) - yac = id,  y = {-id + (d-aa)x} / ac
			-xac + y(d - cc) = kd, -xaacc + {(d-aa)x - id}(d - cc) = kadc
			(-aacc)x + (d-aa)(d-cc)x = { b(b+1)^2 }x = d { kac + i(1+b-cc) } =  b(b+1) { i(b+1)-ja }
			b!=0, b!=-1 -> (b+1)x= i(b+1)-ja, x = i - ja/(b+1)
			b=0 -> i = Tc, k = -Ta
			       1) i = 0 -> kc=0, (xc-ya)c=0; 1-1) c=0 -> a=1, k=y/d, y=d/k
			                                     1-2) k=0 -> j=1, xc=ya, x=at, y=ct
			
		 */
		fun toNormalized(): Normalized {
			val a = normal.x
			val b = normal.y
			val c = normal.z
			return when {
				a similarTo 0.0 && c similarTo 0.0 -> FlatImpl(tangent)
				b similarTo 0.0 -> TODO()
				b similarTo -1.0 -> TODO()
				else -> {
					val d = b + 1
					val x = tangent.x - tangent.y * a / d
					val y = (-tangent.x * d + (d - a * a) * x) / (a * c)
					return NormalizedImpl(base = FlatImpl(Vec3(x, 0.0, y).normalize()), normal, tangent)
				}
			}
		}
		
		override val tangent2: Tangent2?
			get() = null
		
		override fun mirror(by: Mirror): FlexiDirection = Two(by.mirror(tangent), by.mirror(normal))
		override fun rotate(by: Rotation): FlexiDirection = Two(by.rotate(tangent), by.rotate(normal))
		override fun rotateKnown(by: Int): FlexiDirection {
			val angle = by.toFloat() / Known.DivisionCount * PI.toFloat()
			return Two(tangent.yRot(angle), normal.yRot(angle))
		}
		
		override fun optimize(): FlexiDirection {
			val tangent = tangent.optimize()
			val normal = normal.optimize()
			if(tangent !== this.tangent || normal !== this.normal) Two(tangent, normal)
			return this
		}
		
		override fun applyNormal(normal: Vec3): Two = Two(tangent, normal)
		
		override fun unaryMinus(): Two = Two(-tangent, normal)
		
		override fun write(): CompoundTag {
			TODO("Not yet implemented")
		}
		
		override fun toString(): String = "FlexiDirection.Two(tangent=$tangent, normal=$normal)"
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
	get() = tangent2?.let { Mth.atan2(-it.y.toDouble(), it.x.toDouble()) } ?: Mth.atan2(-tangent.z, tangent.x)

fun Vec3.asKnownVec3(): FlexiDirection.KnownVec3? {
	if(this is FlexiDirection.KnownVec3) return this
	val known = FlexiDirection.Known.roundFrom(this).tangent
	return if(known closeTo this) known else null
}

infix fun Vec3.closeToUnsigned(to: Vec3): Boolean = closeTo(to) || closeTo(-to)

fun Vec3.optimize(): Vec3 {
	val xx = x.optimize()
	val yy = y.optimize()
	val zz = z.optimize()
	if(x != xx || y != yy || z != zz) Vec3(xx, yy, zz)
	return this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Double.optimize(): Double {
	if(this < 1e-7 && this > -1e-7) return 0.0
	if(this < 1 + 1e-7 && this > -1 - 1e-7) return 1.0
	return this
}
