import com.lhwdev.minecraft.railx.ccAsm.ComputerApi
import com.lhwdev.minecraft.railx.ccAsm.lua.ProxyInstanceName
import com.lhwdev.minecraft.railx.ccAsm.lua.generateProxyFromApi
import java.io.FileOutputStream
import java.nio.ByteBuffer


@ComputerApi
interface Hello {
	fun writeName(name: String)
	
	fun sayHello(a: Int?, b: Long, c: Hello? = null, d: Float = 3.0f)
}

class Hi {
	val a = intArrayOf(1,2,3,4,3,7,7,3,5,67,78,56,5,3,35,68,5,34,23)
}


class ByteClassLoader : ClassLoader() {
	private var cache: Pair<String, ByteBuffer>? = null
	
	fun load(name: String, bytes: ByteBuffer): Class<*> {
		if(cache != null) throw IllegalStateException()
		cache = name to bytes
		return try {
			loadClass(name)
		} finally {
			cache = null
		}
	}
	
	fun load(name: String, bytes: ByteArray, offset: Int = 0, size: Int = bytes.size): Class<*> =
		load(name, ByteBuffer.wrap(bytes, offset, size))
	
	override fun findClass(name: String): Class<*> {
		cache?.let { (cacheName, bytes) ->
			if(cacheName == name) {
				return defineClass(name, bytes, null)
			}
		}
		return super.findClass(name)
	}
}


fun main() {
	val loader = ByteClassLoader()
	val generated = generateProxyFromApi(Hello::class)
	val helloProxyClass = loader.load(generated.name, generated.bytes)
	
	val helloProxy = helloProxyClass.getDeclaredField(ProxyInstanceName).get(null)
	println(helloProxy)
	
	fun transferCurrent(name: String) {
		val path = name.replace('.', '/') + ".class"
		FileOutputStream(path).also { out ->
			Hello::class.java.getResourceAsStream(path)!!.let {
				println(name + ": "+ it.transferTo(out))
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
}
