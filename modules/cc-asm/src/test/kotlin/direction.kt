import kotlin.math.max

class Known(val index: Int, val ordinal: Int) {
	companion object {
		private const val DivisionCountBase = 3
		private const val DivisionRepeat = 3
		const val DivisionCount = DivisionCountBase * (1 shl DivisionRepeat)
		private val DivisionsByOrdinal: Array<Known>
		private val DivisionsByIndex: Array<Known>
		
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
					println("entry[$index] ordinal=$ordinal scale=$scale ordOffset=$ordinalOffset gap=$gap")
				}
			}
			
			DivisionsByOrdinal = byOrdinal.requireNoNulls()
			DivisionsByIndex = byIndex.requireNoNulls()
			
			println(DivisionsByOrdinal.joinToString { it.ordinal.toString() })
			println(DivisionsByIndex.joinToString { it.ordinal.toString() })
		}
	}
	
	override fun toString(): String = "FlexiDirection.Known(index=$index,ordinal=$ordinal)"
}


fun main() {
	Known.Companion
}
