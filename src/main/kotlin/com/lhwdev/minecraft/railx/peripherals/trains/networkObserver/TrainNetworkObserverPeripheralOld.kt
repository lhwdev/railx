package com.lhwdev.minecraft.railx.peripherals.trains.networkObserver

import com.google.gson.Gson
import com.lhwdev.minecraft.railx.RailX
import com.lhwdev.minecraft.railx.Utils
import com.lhwdev.minecraft.railx.api.peripheral.SmartPeripheral
import com.simibubi.create.AllItems
import com.simibubi.create.Create
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu
import com.simibubi.create.content.logistics.filter.FilterItem
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity
import com.simibubi.create.content.trains.entity.Train
import com.simibubi.create.content.trains.graph.EdgePointType
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation
import com.simibubi.create.content.trains.observer.TrackObserver
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState
import com.simibubi.create.content.trains.signal.SignalBoundary
import com.simibubi.create.content.trains.station.GlobalStation
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import java.util.stream.Collectors

class TrainNetworkObserverPeripheralOld(var parent: TrainNetworkObserverBlockEntity) : SmartPeripheral() {
	val trains: List<Train>
		get() {
			val trainList = LinkedList<Train>()
			val graphLocation = parent.graphLocation!!
			for(train in Create.RAILWAYS.trains.values) {
				if(train.graph.id == graphLocation.graph.id) trainList += train
			}
			return trainList
		}
	val stations: Collection<GlobalStation>
		get() = parent.graphLocation!!.graph.getPoints(EdgePointType.STATION)
	private val signals: Collection<SignalBoundary>
		get() = parent.graphLocation!!.graph.getPoints(EdgePointType.SIGNAL)
	private val observers: Collection<TrackObserver>
		get() = parent.graphLocation!!.graph.getPoints(EdgePointType.OBSERVER)
	
	init {
		// TRAINS
		addMethod("getTrains") { _, _, _ ->
			val trainList = trains
			MethodResult.of(trainList.stream().map { a: Train -> a.id.toString() }.collect(Collectors.toList()))
		}
		addMethod("getTrainName") { _, _, arguments ->
			val b = arguments.getString(0)
			val first = trains.stream().filter { a: Train -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val train = first.get()
			MethodResult.of(Gson().fromJson<Map<*, *>>(Component.Serializer.toJson(train.name), MutableMap::class.java))
		}
		addMethod("getTrainSchedule") { _, _, arguments ->
			val b = arguments.getString(0)
			val first = trains.stream().filter { a -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val train = first.get()
			val map = HashMap<Any, Any>()
			if(train.runtime.schedule == null) return@addMethod MethodResult.of(null)
			var bidx = 1
			for(entry in train.runtime.schedule.entries) {
				val data = HashMap<Any, Any?>()
				data["type"] = entry.instruction.id.toString()
				data["data"] = blowNBT(entry.instruction.data)
				var sidx = 1
				for(conditionLayer in entry.conditions) {
					val conditionL: MutableMap<Any, Any> = HashMap()
					var idx = 1
					for(cond in conditionLayer) {
						val condition = HashMap<Any, Any?>()
						condition["type"] = cond.id.toString()
						condition["data"] = blowNBT(cond.data)
						conditionL[idx] = condition
						idx++
					}
					data[sidx] = conditionL
					sidx++
				}
				map[bidx] = data
				bidx++
			}
			MethodResult.of(map)
		}
		addMethod("getTrainWorldPosition") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = trains.stream().filter { a: Train -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val train = first.get()
			var carr: Optional<DimensionalCarriageEntity> = Optional.empty()
			val a2 = train.carriages[0].getDimensionalIfPresent(Level.NETHER)
			if(a2 != null) carr = Optional.of(a2)
			val a1 = train.carriages[0].getDimensionalIfPresent(Level.OVERWORLD)
			if(a1 != null) carr = Optional.of(a1)
			if(carr.isEmpty) return@addMethod MethodResult.of(null)
			val car = carr.get()
			val ent = car.entity.get()
			MethodResult.of(ent!!.x, ent.y, ent.z, ent.level().dimension().registry().toString())
		}
		addMethod("getTrainSpeed") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = trains.stream().filter { a: Train -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val train = first.get()
			MethodResult.of(train.speed)
		}
		addMethod("getTrainStopped") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = trains.stream().filter { a: Train -> a.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val train = first.get()
			val currentStation = train.getCurrentStation()
			if(currentStation != null) return@addMethod MethodResult.of(
				currentStation.getId().toString(),
				currentStation.name
			)
			MethodResult.of(null)
		}
		// STOPS
		addMethod("getStops") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments? ->
			val stations = stations
			MethodResult.of(stations.stream().map { a: GlobalStation -> a.id.toString() }.collect(Collectors.toList()))
		}
		addMethod("getStopName") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = stations.stream().filter { a: GlobalStation -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val station = first.get()
			MethodResult.of(station.name)
		}
		addMethod("getStopWorldPosition") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = stations.stream().filter { a: GlobalStation -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val station = first.get()
			MethodResult.of(station.blockEntityPos.x, station.blockEntityPos.y, station.blockEntityPos.z)
		}
		addMethod("getStopExpectedTrain") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments ->
			val b = iArguments.getString(0)
			val first = stations.stream().filter { a: GlobalStation -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val station = first.get()
			val prepare = GlobalTrainDisplayData.prepare(station.name, 200)
			val data = HashMap<Int?, Map<String, Any>?>()
			var idx = 1
			for(dep in prepare) {
				val subData: MutableMap<String, Any> = HashMap()
				subData["destination"] = dep.destination // obvious lol
				subData["ticks"] = dep.ticks
				subData["scheduleName"] =
					Gson().fromJson<Map<*, *>>(Component.Serializer.toJson(dep.scheduleTitle), MutableMap::class.java)
				subData["train"] = dep.train.id.toString()
				data[idx] = subData
				idx++
			}
			MethodResult.of(data)
		}
		// SIGNALS
		addMethod("getSignals") { _, _, _ ->
			val stations = signals
			MethodResult.of(stations.stream().map { a: SignalBoundary -> a.id.toString() }.collect(Collectors.toList()))
		}
		addMethod("getSignalWorldPositions") { _, _, arguments ->
			val b = arguments.getString(0)
			val first = signals.stream().filter { a: SignalBoundary -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val signal = first.get()
			val returned: MutableMap<Int, Map<Int, Int>> = HashMap()
			var idx = 1
			// fix shitcode and implications
			for((key) in signal.blockEntities[true]) {
				val pos: MutableMap<Int, Int> = HashMap()
				pos[1] = key.x
				pos[2] = key.y
				pos[3] = key.z
				returned[idx] = pos
				idx++
			}
			for((key) in signal.blockEntities[false]) {
				val pos: MutableMap<Int, Int> = HashMap()
				pos[1] = key.x
				pos[2] = key.y
				pos[3] = key.z
				returned[idx] = pos
				idx++
			}
			MethodResult.of(returned)
		}
		addMethod("getSignalState") { _, _, arguments ->
			val b = arguments.getString(0)
			val toPos = arguments.getBoolean(1)
			val first = signals.stream().filter { a -> a.id.toString() == b }.findFirst()
			if(first.isEmpty) return@addMethod MethodResult.of(null)
			val signal = first.get()
			val stateFor = signal.cachedStates[toPos]
			when(stateFor) {
				SignalState.RED -> MethodResult.of(0)
				SignalState.YELLOW -> MethodResult.of(1)
				SignalState.GREEN -> MethodResult.of(2)
				SignalState.INVALID -> MethodResult.of(-1)
			}
		}
		// OBSERVERS
		addMethod("getObservers") { _, _, _ ->
			val stations = observers
			MethodResult.of(stations.stream().map { a: TrackObserver -> a.id.toString() }.collect(Collectors.toList()))
		}
		addMethod("getObserverWorldPosition") { _, _, arguments ->
			val b = arguments.getString(0)
			val first = observers.firstOrNull { a: TrackObserver -> a.id.toString() == b }
			if(first == null) return@addMethod MethodResult.of(null)
			val obs = first
			MethodResult.of(obs.blockEntityPos.x, obs.blockEntityPos.y, obs.blockEntityPos.z)
		}
		addMethod(
			"getObserverFilter"
		)
		{ _, _, arguments ->
			val b = arguments.getString(0)
			val first = observers.firstOrNull { a -> a.id.toString() == b }
			if(first == null) return@addMethod MethodResult.of(null)
			val obs = first
			MethodResult.of(blowFilter(obs.filter))
		}
		// GRAPH
		addMethod("getGraph") { iComputerAccess: IComputerAccess?, iLuaContext: ILuaContext?, iArguments: IArguments? ->
			val nodes = parent.graphLocation!!.graph.nodes
			val map = LinkedList<Any>()
			val index = 0
			for(location in nodes) {
				val map2: MutableMap<Any, Any> = HashMap()
				map2["x"] = location.x as Number
				map2["y"] = location.y as Number
				map2["z"] = location.z as Number
				map2["dimension"] = location.dimension.location().toString()
				map2["bezier"] = false
				if(location is DiscoveredLocation) {
					val turn = location.turn
					if(turn != null) {
						map2["bezier"] = true
						map2["girder"] = turn.hasGirder
						map2["primary"] = turn.hasGirder
						map2["positions"] = listOf(
							listOf(
								turn.tePositions[true].x,
								turn.tePositions[true].y,
								turn.tePositions[true].z
							),
							listOf(
								turn.tePositions[false].x,
								turn.tePositions[false].y,
								turn.tePositions[false].z
							)
						)
						map2["starts"] = listOf(
							listOf(turn.starts[true].x, turn.starts[true].y, turn.starts[true].z),
							listOf(turn.starts[false].x, turn.starts[false].y, turn.starts[false].z)
						)
						map2["axes"] = listOf(
							listOf(turn.axes[true].x, turn.axes[true].y, turn.axes[true].z),
							listOf(turn.axes[false].x, turn.axes[false].y, turn.axes[false].z)
						)
						map2["normals"] = listOf(
							listOf(turn.normals[true].x, turn.normals[true].y, turn.normals[true].z),
							listOf(turn.normals[false].x, turn.normals[false].y, turn.normals[false].z)
						)
					}
				}
				map.add(map2)
			}
			MethodResult.of(map)
		}
	}
	
	override fun getType(): String {
		return ResourceLocation(RailX.modId, "train_network_observer").toString()
	}
	
	override fun equals(iPeripheral: IPeripheral?): Boolean {
		return false
	}
	
	companion object {
		private fun blowFilter(filter: ItemStack): Map<Any, Any?> {
			val ret: MutableMap<Any, Any?> = HashMap()
			if(filter.`is`(AllItems.FILTER.get())) {
				val filterItems = FilterItem.getFilterItems(filter)
				ret["type"] = "filter"
				ret["whitelist"] = !filter.tag!!.getBoolean("Blacklist")
				ret["respectnbt"] = !filter.tag!!.getBoolean("RespectNBT")
				for(i in 1 until filterItems.slots + 1) {
					ret[i - 1] = blowFilter(filterItems.getStackInSlot(i))
				}
				return ret
			}
			if(filter.`is`(AllItems.ATTRIBUTE_FILTER.get())) {
				ret["type"] = "attribute"
				val whitelistMode = AttributeFilterMenu.WhitelistMode.entries[filter.tag!!.getInt("WhitelistMode")]
				ret["allowmode"] = whitelistMode.toString()
				val tag = filter.tag!!.getList("MatchedAttributes", Tag.TAG_COMPOUND.toInt())
				var idx = 1
				for(inb in tag) {
					val tg = inb as CompoundTag
					ret[idx] = blowNBT(tg)
					idx++
				}
				return ret
			}
			ret["type"] = "item"
			ret["count"] = filter.count
			ret["id"] = ForgeRegistries.ITEMS.getKey(filter.item).toString()
			ret["nbt"] = blowNBT(filter.tag)
			return ret
		}
		
		const val DEBUG_NBT = false
		private fun blowNBT(tag: Tag?): Any? {
			val b = Utils.blowNBT(tag)
			if(DEBUG_NBT) {
				println("======================")
				println("in: " + tag!!.javaClass.getName())
				println("out: " + b!!.javaClass.getName())
			}
			return b
		}
	}
}
