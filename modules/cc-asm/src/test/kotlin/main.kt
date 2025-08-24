import com.lhwdev.minecraft.railx.ccAsm.ComputerApi
import com.lhwdev.minecraft.railx.ccAsm.ComputerApiItem
import com.lhwdev.minecraft.railx.ccAsm.InvokeContext
import com.lhwdev.minecraft.railx.ccAsm.ParseContext
import com.lhwdev.minecraft.railx.ccAsm.lua.ProxyInstanceName
import com.lhwdev.minecraft.railx.ccAsm.lua.generateProxyFromApi
import org.squiddev.cobalt.Constants
import org.squiddev.cobalt.LuaTable
import org.squiddev.cobalt.LuaValue
import org.squiddev.cobalt.ValueFactory
import org.squiddev.cobalt.Varargs
import java.io.FileOutputStream
import java.nio.ByteBuffer


@ComputerApi
interface Hello {
	fun writeName(name: String)
	
	fun sayHello(a: Int?, b: Long, c: Hello? = null, d: Float = 3.0f): Int
}

class Hi {
	val a = intArrayOf(1, 2, 3, 4, 3, 7, 7, 3, 5, 67, 78, 56, 5, 3, 35, 68, 5, 34, 23)
}


class ByteClassLoader : ClassLoader() {
	private var cache: Pair<String, ByteBuffer>? = null
	
	fun load(name: String, bytes: ByteBuffer): Class<*> {
		if(findLoadedClass(name) != null) throw IllegalStateException("already loaded $name")
		if(cache != null) throw IllegalStateException()
		cache = name to bytes
		return try {
			loadClass(name)
		} finally {
			if(cache != null) throw IllegalStateException("class $name not loaded")
		}
	}
	
	fun load(name: String, bytes: ByteArray, offset: Int = 0, size: Int = bytes.size): Class<*> =
		load(name, ByteBuffer.wrap(bytes, offset, size))
	
	override fun findClass(name: String): Class<*> {
		cache?.let { (cacheName, bytes) ->
			if(cacheName == name) {
				cache = null
				return defineClass(name, bytes, null)
			}
		}
		return super.findClass(name)
	}
}

fun main() {
	val loader = ByteClassLoader()
	val generated = generateProxyFromApi(Hello::class)
	
	fun transferCurrent(name: String) {
		val path = name.replace('.', '/') + ".class"
		FileOutputStream(path).also { out ->
			Hello::class.java.getResourceAsStream(path)!!.let {
				it.transferTo(out)
				it.close()
			}
			out.close()
		}
	}
	
	transferCurrent("Hello")
	transferCurrent("Hello\$DefaultImpls")
	// transferCurrent("Whoosh")
	
	FileOutputStream("Hello\$Proxy.class").also {
		it.write(generated.bytes)
		it.close()
	}
	
	val helloProxyClass = loader.load(generated.name, generated.bytes)
	val helloProxy = helloProxyClass.getDeclaredField(ProxyInstanceName).get(null) as ComputerApiItem.Object<*>
	println(helloProxy)
	
	val hello = object : Hello {
		override fun writeName(name: String) {
			println("write=$name")
		}
		
		override fun sayHello(a: Int?, b: Long, c: Hello?, d: Float): Int {
			println("sayHello!! $a $b $c $d")
			return 5
		}
	}
	val context = StubInvokeContext(
		// a: Int?, b: Long, c: Hello?, d?: Float
		0,
		3L,
		hello,
	)
	val sayHello = helloProxy.items.filterIsInstance<ComputerApiItem.Function>().find { it.name == "sayHello" }!!
	sayHello.invoke(hello, context)
}


class StubInvokeContext(private val value: List<Any?>) : InvokeContext() {
	constructor(vararg items: Any?) : this(listOf(*items))
	
	private var index = 0
	
	fun next(): Any? = value[index++]
	
	inline fun <reified T> nextAs(): T = next() as T
	
	override fun argumentsCount(count: Int) {
		require(value.size == count)
	}
	
	override fun argumentsCount(minCount: Int, maxCount: Int) {
		require(value.size in minCount..maxCount)
	}
	
	override fun skipCurrent(): Varargs? = null
	
	override fun hasOptional(maybeNull: Boolean): Boolean {
		if(index >= value.size) return false
		if(value[index] == null) return false
		return true
	}
	
	override fun isNull(): Boolean = value[index] == null
	
	override fun boolean(): Boolean = nextAs()
	override fun byte(): Byte = nextAs()
	override fun short(): Short = nextAs()
	override fun int(): Int = nextAs()
	override fun long(): Long = nextAs()
	override fun float(): Float = nextAs()
	override fun double(): Double = nextAs()
	override fun char(): Char = nextAs()
	override fun string(): String = nextAs()
	
	override fun <T : Any> apiInterface(declaration: ComputerApiItem.Object<T>): T =
		declaration.asSelf(next())
	
	override fun list(): ParseList = StubListContext(nextAs())
	
	override fun wrapReturn(value: Any?): LuaValue {
		if(value == null) return Constants.NIL
		return when(value::class.java) {
			Boolean::class.javaObjectType -> ValueFactory.valueOf(value as Boolean)
			Short::class.javaObjectType -> ValueFactory.valueOf((value as Short).toInt())
			Int::class.javaObjectType -> ValueFactory.valueOf(value as Int)
			Long::class.javaObjectType -> ValueFactory.valueOf((value as Long).toInt())
			Float::class.javaObjectType -> ValueFactory.valueOf((value as Float).toDouble())
			Double::class.javaObjectType -> ValueFactory.valueOf(value as Double)
			Char::class.javaObjectType -> ValueFactory.valueOf("${value as Char}")
			
			List::class.java -> LuaTable().also { list ->
				for((index, v) in (value as List<Any?>).withIndex()) list.rawset(index, wrapReturn(v))
			}
			
			else -> error("unknown return value $value")
		}
	}
	
	override fun wrapReturnVoid(): Varargs =
		Constants.NONE
}


class StubListContext(private val value: List<Any?>) : ParseContext.ParseList() {
	private var index = 0
	
	fun next(): Any? = value[index++]
	
	inline fun <reified T> nextAs(): T = next() as T
	
	override fun hasNext(): Boolean = index < value.size
	
	override fun isNull(): Boolean = value[index] == null
	
	override fun boolean(): Boolean = nextAs()
	override fun byte(): Byte = nextAs()
	override fun short(): Short = nextAs()
	override fun int(): Int = nextAs()
	override fun long(): Long = nextAs()
	override fun float(): Float = nextAs()
	override fun double(): Double = nextAs()
	override fun char(): Char = nextAs()
	override fun string(): String = nextAs()
	
	override fun <T : Any> apiInterface(declaration: ComputerApiItem.Object<T>): T =
		declaration.asSelf(next())
	
	override fun list(): ParseList = StubListContext(nextAs())
}

private fun <T : Any> ComputerApiItem.Object<T>.asSelf(value: Any?): T =
	type.cast(value)
