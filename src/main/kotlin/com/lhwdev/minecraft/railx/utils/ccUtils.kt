package com.lhwdev.minecraft.railx.utils

import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import java.util.concurrent.atomic.AtomicInteger


fun waitLoop(filter: String?, block: ((args: Array<*>) -> MethodResult?)): MethodResult =
	MethodResult.pullEvent(filter) { block(it) ?: waitLoop(filter, block) }

private var lastTaskId = AtomicInteger(0)
private const val TaskName = "railx:async_task_done"

fun asyncLuaTask(computer: IComputerAccess): Pair<(result: Array<Any?>) -> Unit, MethodResult> {
	val taskId = lastTaskId.getAndIncrement()
	val onComplete: (result: Any) -> Unit = { computer.queueEvent(TaskName, taskId, it) }
	val result = waitLoop(TaskName) { args ->
		if(args.size == 3) {
			val id = args[1]
			val result = args[2] as? Array<*> ?: return@waitLoop null
			if(id == taskId) {
				MethodResult.of(*result)
			} else null
		} else null
	}
	
	return onComplete to result
}
