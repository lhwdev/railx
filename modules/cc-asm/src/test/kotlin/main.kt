import com.lhwdev.minecraft.railx.ccAsm.lua.generateProxyFromApi
import java.io.FileOutputStream

interface Hello {
	fun writeName(name: String)
	
	fun sayHello(age: Int, other: Hello?)
}

fun main() {
	val bytes = generateProxyFromApi(Hello::class)
	FileOutputStream("Hello.class").also {
		it.write(bytes)
		it.flush()
		it.close()
	}
}
