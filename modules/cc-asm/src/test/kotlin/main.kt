import com.lhwdev.minecraft.railx.ccAsm.ComputerApi
import com.lhwdev.minecraft.railx.ccAsm.lua.generateProxyFromApi
import java.io.FileOutputStream


@ComputerApi
interface Hello {
	fun writeName(name: String)
	
	fun sayHello(a: Int?, b: Long, c: Hello? = null, d: Float = 3.0f)
}

fun main() {
	val bytes = generateProxyFromApi(Hello::class)
	FileOutputStream("Hello\$Proxy.class").also {
		it.write(bytes)
		it.flush()
	}
}
