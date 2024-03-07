package com.lhwdev.asm.util

import org.objectweb.asm.Type
import java.nio.ByteBuffer


open class BytecodeClassLoader(parent: ClassLoader? = null) : ClassLoader(parent) {
	companion object Default : BytecodeClassLoader()
	
	
	private val cache = HashMap<String, ByteBuffer>()
	
	
	fun createClass(name: String, data: ByteBuffer): Class<*> = try {
		cache[name] = data
		loadClass(name)
	} finally {
		cache -= name
	}
	
	inline fun getOrCreateClass(name: String, block: (name: String) -> ByteBuffer): Class<*> =
		findLoadedClassPublic(name) ?: createClass(name, block(name.replace('.', '/')))
	
	@PublishedApi
	internal fun findLoadedClassPublic(name: String): Class<*>? =
		findLoadedClass(name)
	
	
	@Throws(ClassNotFoundException::class)
	override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
		// First, check if the class has already been loaded
		var c = findLoadedClass(name)
		if(c == null) {
			// If still not found, then invoke findClass in order
			// to find the class.
			val t1 = System.nanoTime()
			c = findClass(name)
			
			// this is the defining class loader; record the stats
			// PerfCounter.getFindClassTime().addElapsedTimeFrom(t1)
			// PerfCounter.getFindClasses().increment()
		}
		if(resolve) {
			resolveClass(c)
		}
		c
	}
	
	override fun findClass(name: String): Class<*> = cache[name]?.let { data ->
		defineClass(name, data, null)
	} ?: super.findClass(name)
}
