/**
 *  Copyright 2019
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * V0.0.2 12.04.2019
 *
 *  - fixed issue with syncing
 *  - added binary switch status
 *  - setting association group in configure
 *
 * V0.0.3 25.04.2019
 *
 *  - Set operating mode
 *  - Set thermostat operating state (idle,heating)
 *  - Changed colors to the recommended colors from the API
 *  - Fixed issue with setting eco heating set point
*/

metadata {
	definition (name: "heatit Z-Trm2fx", namespace: "JoneLura", author: "JoneLura") {
		capability "Actuator"
		capability "Temperature Measurement"
		capability "Thermostat"
    	capability "Thermostat Mode"
	    capability "Thermostat Heating Setpoint"
    	capability "Thermostat Setpoint"
		capability "Configuration"
		capability "Polling"
		capability "Sensor"
	    capability "Energy Meter"
    	capability "Power Meter"
	    capability "Zw Multichannel"


		command "switchMode"
    	command "energySaveHeat"
	    command "quickSetHeat"
    	command "quickSetecoHeat"
	    command "pressUp"
    	command "pressDown"
        command "ecoPressUp"
    	command "ecoPressDown"

	}

  tiles (scale: 2)
    {

        multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4)
        {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL")
            {
                attributeState("default", label:'${currentValue}°', unit:"C", action:"switchMode", icon:"st.Home.home1")
            }

            tileAttribute("device.thermostatMode", key: "OPERATING_STATE")
            {
                attributeState("off",  action:"switchMode", nextState:"heat")
                attributeState("heat",  action:"switchMode", nextState:"energy", backgroundColor: "#e86d13")
                attributeState("energySaveHeat", action:"switchMode", nextState:"off", backgroundColor: "#79b821")
            }

            tileAttribute("device.heatingSetpoint", key: "VALUE_CONTROL")
            {
                attributeState("VALUE_UP", action: "pressUp")
                attributeState("VALUE_DOWN", action: "pressDown")
            }

			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT")
            {
                attributeState("default", label:'${currentValue}')
            }
        }

        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
            state "power", label:'${currentValue}\n W', action:"refresh"
        }

        standardTile("power-icon", "power.icon", decoration: "flat", width: 2, height: 2) {
    		state "default", label: 'Power',  icon:"st.switches.switch.off"
        }

        valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
            state "energy", label:'${currentValue}\n kWh', action:"refresh"
        }

		standardTile("refresh", "refresh", width:2, height:2, inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
		}

        standardTile("syncPending", "syncPending", decoration: "flat", width: 2, height: 2) {
            state "default", label:'Sync Pending', backgroundColor:"#FF6600", icon: "https://raw.githubusercontent.com/codersaur/SmartThings/master/icons/tile_2x2_cycle.png"
            state "0", label:'Synced', backgroundColor:"#79b821", icon: "https://raw.githubusercontent.com/codersaur/SmartThings/master/icons/tile_2x2_tick.png"
        }

        standardTile("state", "device.thermostatOperatingState", height: 2, width: 2) {
                state("idle", label: "Idle", backgroundColor:"#00A0DC")
                state("heating", label: 'Heating', backgroundColor:"#e86d13")
        }

        standardTile("configure", "configure", width: 2, height: 2, inactiveLable: false, decoration: "flat") {
        	state "default", action: "configure", label: "configure"
        }

        main (["thermostatMulti"])
        details(["thermostatMulti",  "power-icon",
         "power",
         "energy",
        "refresh", "syncPending", "state",
        "configure"
        ])
	}

  preferences {
      parameterMap().each {
          input (
              title: "${it.num}. ${it.title}",
              description: it.descr,
              type: "paragraph",
              element: "paragraph"
          )

          input (
              name: it.key,
              title: null,
              description: "Default: $it.def" ,
              type: it.type,
              options: it.options,
              range: (it.min != null && it.max != null) ? "${it.min}..${it.max}" : null,
              defaultValue: it.def,
              required: false
          )
      }

      input ( name: "logging", title: "Logging", type: "boolean", required: false )
 }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    logger("zwaveEvent(): Switch Binary Report received: ${cmd}","info")
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd) {
    log.debug "multichannelv3.MultiChannelCapabilityReport: ${cmd}"
}


def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    log.debug "${device.displayName} - MultiChannelCmdEncap ${cmd}"
    def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
    if (encapsulatedCommand) {
        log.debug "${device.displayName} - Parsed MultiChannelCmdEncap ${encapsulatedCommand}"
        zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
    } else {
        log.warn "Unable to extract MultiChannel command from $cmd"
    }
}


def zwaveEvent(physicalgraph.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
    log.debug "${device.displayName} - ThermostatSetpoinReport received, value: ${cmd.scaledValue} scale: ${cmd.scale}"

   	if (cmd.setpointType == 1){
        def heating = cmd.scaledValue
        sendEvent(name: "heatingSetpoint", value: heating)
    }
    if (cmd.setpointType == 11){
    	def energyHeating = cmd.scaledValue
        sendEvent(name: "ecoHeatingSetpoint", value: energyHeating)
        state.ecoheatingSetpoint = energyHeating
    }
	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
	//map
}


def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd)
{
 //   log.debug "${device.displayName} - MeterReport received, value: ${cmd.scaledMeterValue} scale: ${cmd.scale}"
    switch (cmd.scale) {
        case 0:
            sendEvent([name: "energy", value: cmd.scaledMeterValue, unit: "kWh"])
        //    log.debug "energy $cmd.scaledMeterValue kwh"
            break
        case 2:
            sendEvent([name: "power", value: cmd.scaledMeterValue, unit: "W"])
            state.thermostatOperatingState = "idle"
            if (cmd.scaledMeterValue > 0.5) {
            	state.thermostatOperatingState = "heating"
            }
            sendEvent([name: "thermostatOperatingState", value: state.thermostatOperatingState])
        //    log.debug "power $cmd.scaledMeterValue W"
            break
    }
    multiStatusEvent("${(device.currentValue("power") ?: "0.0")} W | ${(device.currentValue("energy") ?: "0.00")} kWh")
}


def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
	def map = [ sensorType: cmd.sensorType, scale: cmd.scale, value: cmd.scaledSensorValue.toString() ]

    switch (cmd.sensorType) {
        case 1:  // Air Temperature (V1)
            map.name = "temperature"
            map.unit = (cmd.scale == 1) ? "F" : "C"
            break
        default:
            logger("zwaveEvent(): SensorMultilevelReport with unhandled sensorType: ${cmd}","warn")
            map.name = "unknown"
            map.unit = "unknown"
            break
    }

	logger("New multilevel sensor report: Name: ${map.name}, Value: ${map.value}, Unit: ${map.unit}","info")
	if (cmd.sensorValue[1] > 0) {
    	// Only send event where the sensor has a value - need to improve this, maybe sort?
    	log.debug("Temperature is: $cmd.scaledSensorValue °C")
    	sendEvent(name: "temperature", value: cmd.scaledSensorValue)
    }

}

def zwaveEvent(physicalgraph.zwave.commands.thermostatoperatingstatev2.ThermostatOperatingStateReport cmd)
{
	log.debug("operating rep: $cmd")
	def map = [:]
	switch (cmd.operatingState) {
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
            break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
			break
	}

    sendEvent(name: "thermostatOperatingState", value: map.value)

	map.name = "thermostatOperatingState"
	map
}


def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	log.debug("Thermostat Mode Report $cmd")
	def map = [:]
	switch (cmd.mode) {
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
            sendEvent(name: "thermostatMode", value: "off")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
            sendEvent(name: "thermostatMode", value: "heat")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
			map.value = "emergency heat"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
			break
        case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_ENERGY_SAVE_HEAT:
			map.value = "energySaveHeat"
            sendEvent(name: "thermostatMode", value: "energySaveHeat")
			break
	}
	map.name = "thermostatMode"
	map
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	log.debug("support reprt: $cmd")
    def supportedModes = ""
	if(cmd.off) { supportedModes += "off " }
	if(cmd.heat) { supportedModes += "heat " }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergency heat " }
	if(cmd.cool) { supportedModes += "cool " }
	if(cmd.auto) { supportedModes += "auto " }
    if(cmd.energySaveHeat) { supportedModes += "energySaveHeat " }

	state.supportedModes = supportedModes
}

def zwaveEvent(physicalgraph.zwave.commands.multiinstanceassociationv1.MultiInstanceAssociationReport cmd)
{

        logging("${device.displayName} MultiInstanceAssociationReport - ${cmd}","info")
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd)
{
    logging ("${device.displayName} MultiChannelAssociationReport - ${cmd}","debug")

     def result = []
        if (cmd.nodeId.any { it == zwaveHubNodeId }) {
        	logging ( "$device.displayName is associated in group ${cmd.groupingIdentifier}", "debug")
                result << createEvent(descriptionText: "$device.displayName is associated in group ${cmd.groupingIdentifier}")
        } else if (cmd.groupingIdentifier == 1) {
                // We're not associated properly to group 1, set association
                logging ( "Associating $device.displayName in group ${cmd.groupingIdentifier}", "debug")
                result << createEvent(descriptionText: "Associating $device.displayName in group ${cmd.groupingIdentifier}")
                result << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId))
        } else if (cmd.groupingIdentifier == 2) {
                // We're not associated properly to group 1, set association
                logging ( "Associating $device.displayName in group ${cmd.groupingIdentifier}", "debug")
                result << createEvent(descriptionText: "Associating $device.displayName in group ${cmd.groupingIdentifier}")
                result << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId))
        }
result
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) {
	def result = []
	def cmds = []
	if(!state.endpointInfo) state.endpointInfo = []
	state.endpointInfo[cmd.endPoint - 1] = cmd.format()[6..-1]
	if (cmd.endPoint < getDataValue("endpoints").toInteger()) {
		cmds = zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: cmd.endPoint + 1).format()
	} else {
		log.debug "endpointInfo: ${state.endpointInfo.inspect()}"
	}
	result << createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:"")
	if(cmds) result << response(cmds)
	result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
	state.groups = cmd.supportedGroupings
	if (cmd.supportedGroupings > 1) {
		[response(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier:2, listMode:1))]
	}
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
	def cmds = []

	for (def i = 2; i <= state.groups; i++) {
		cmds << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:i, nodeId:zwaveHubNodeId))
	}
	cmds
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd) {
    logger("zwaveEvent(): Switch Binary Set received: ${cmd}","info")
    sendEvent(name: "switch", value: cmd.switchValue, displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	log.debug "Basic Zwave event received: $cmd.payload"
}

def pressUp(){
	log.debug("pressed Up")
	def currTemp = device.latestValue("heatingSetpoint")
    log.debug(" pressed up currently $currTemp")
    def newTemp = currTemp + 0.5
    log.debug(" pressed up new temp is $newTemp")
	quickSetHeat(newTemp)
}

def pressDown(){
	log.debug("pressed Down")
	def currTemp = device.latestValue("heatingSetpoint")
    def newTemp = currTemp - 0.5
	quickSetHeat(newTemp)
}

def quickSetHeat(degrees) {

	setHeatingSetpoint(degrees, 100)
}

def setHeatingSetpoint(degrees, delay = 30000) {
	setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {
	log.trace "setHeatingSetpoint($degrees, $delay)"
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    //if (locationScale == "C" && deviceScaleString == "F") {
    //	convertedDegrees = celsiusToFahrenheit(degrees)
    //} else if (locationScale == "F" && deviceScaleString == "C") {
    	convertedDegrees = fahrenheitToCelsius(degrees)
    //} else {
    	convertedDegrees = degrees
    //}
    def cmds = []
    cmds << zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees)
    cmds << zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1)
    encapSequence(cmds)
}

def ecoPressUp(){
	log.debug("pressed Up")
	def currTemp = state.ecoheatingSetpoint
    log.debug(" pressed up currently $currTemp")
    def newTemp = currTemp + 0.5
    log.debug(" pressed up new temp is $newTemp")
	quickSetecoHeat(newTemp)
}

def ecoPressDown(){
	log.debug("pressed Down")
	def currTemp = state.ecoheatingSetpoint
    def newTemp = currTemp - 0.5
	quickSetecoHeat(newTemp)
}

def quickSetecoHeat(degrees) {

	setecoHeatingSetpoint(degrees, 100)
}

def setecoHeatingSetpoint(degrees, delay = 30000) {
	setecoHeatingSetpoint(degrees.toDouble(), delay)
}

def setecoHeatingSetpoint(Double degrees, Integer delay = 30000) {
	log.trace "setEcoHeatingSetpoint($degrees, $delay)"
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    //if (locationScale == "C" && deviceScaleString == "F") {
    //	convertedDegrees = celsiusToFahrenheit(degrees)
    //} else if (locationScale == "F" && deviceScaleString == "C") {
    	convertedDegrees = fahrenheitToCelsius(degrees)
    //} else {
    	convertedDegrees = degrees
    //}

    def cmds = []
    cmds << zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 11, scale: deviceScale, precision: p, scaledValue: convertedDegrees)
    cmds << zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 11)
    encapSequence(cmds, delay)

}

def setThermostatMode(String value) {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	])
}

def off() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	])
}

def heat() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	])
}

def energySaveHeat() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 11).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	])
}


def poll() {
	def cmds = []
    cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1)
	cmds << zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 1)
	cmds << zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 2)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    cmds << zwave.thermostatOperatingStateV1.thermostatOperatingStateGet()
	cmds << zwave.configurationV2.configurationGet(parameterNumber: 1)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 2)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 5)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 6)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 11)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 12)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 13)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 14)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 15)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 17)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 18)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 19)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 20)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 21)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 22)

    encapSequence(cmds, 650)
}

def configured() {

    logging("${device.displayName} - Executing configured()","info")

    delayBetween([
    	zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId).format(),
	])
}

def updated() {
	log.debug "Update ${device.displayName} - nodeId: $zwaveHubNodeId"
    if ( state.lastUpdated && (now() - state.lastUpdated) < 500 ) return
    logging("${device.displayName} - Executing updated()","info")

    runIn(3,"syncStart")
    state.lastUpdated = now()

}

def installed() {
	// Configure device

	def cmds = []

   /* def groupingIds = [1,2,3,4]

    groupingIds.each() {
    logger("installed(): Installing Association Group #${it} using Multi-Channel Association commands. New Destinations: ${toHexString([])}","info")
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: it)
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: it, nodeId: []) // Remove All
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: it, nodeId: [])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: it)
   	}*/

   	cmds <<	new physicalgraph.device.HubAction(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())
	sendHubCommand(cmds)
	runIn(3, "initialize")  // Allow configure command to be sent and acknowledged before proceeding
}

def configure()
{
   poll()
}

def modes() {
	["off", "heat", "energySaveHeat"]
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=200) {
    sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}

def updateState(String name, String value) {
	state[name] = value
	device.updateDataValue(name, value)
}

def switchMode() {
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = getDataByName("lastTriedMode") ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedModes")
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	if (supportedModes?.contains(currentMode)) {
		while (!supportedModes.contains(nextMode) && nextMode != "off") {
			nextMode = next(nextMode)
		}
	}
    log.debug "Switching to mode: ${nextMode}"
	switchToMode(nextMode)
}

def switchToMode(nextMode) {
	def supportedModes = getDataByName("supportedModes")
	if(supportedModes && !supportedModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"
	if (nextMode in modes()) {
		updateState("lastTriedMode", nextMode)
		return "$nextMode"()
	} else {
		log.debug("no mode method '$nextMode'")
	}
}

def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

def getModeMap() { [
	"off": 0,
	"heat": 1,
	"energySaveHeat": 11
]}

private getStandardDelay() {
	1000
}

/*
####################
## Z-Wave Toolkit ##
####################
*/
def parse(String description) {
    def result = []
    logging("${device.displayName} - Parsing: ${description}")
    if (description.startsWith("Err 106")) {
        result = createEvent(
                descriptionText: "Failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
                eventType: "ALERT",
                name: "secureInclusion",
                value: "failed",
                displayed: true,
        )
    } else if (description == "updated") {
        return null
    } else {
        def cmd = zwave.parse(description, cmdVersions())
        if (cmd) {
            logging ("${device.displayName} - Parsed: ${cmd}")
            zwaveEvent(cmd)
        }
    }
}

private syncStart() {
    boolean syncNeeded = false
    parameterMap().each {
        if(settings."$it.key" != null) {
            if (state."$it.key" == null)
            {
            	state."$it.key" = [value: null, state: "synced", scale: null]
            }
            if (state."$it.key".value != settings."$it.key" as Integer || state."$it.key".state in ["notSynced","inProgress"]) {
                state."$it.key".value = settings."$it.key" as Integer
                state."$it.key".state = "notSynced"
                syncNeeded = true
            }
        }
    }
    if ( syncNeeded ) {
        logging("${device.displayName} - starting sync.", "info")
        multiStatusEvent("Sync in progress.", true, true)
        sendEvent(name: "syncPending", value: 1, displayed: false)
        syncNext()
    }
}

private syncNext() {
    logging("${device.displayName} - Executing syncNext()","info")
    def cmds = []
    for ( param in parameterMap() ) {
        if ( state."$param.key"?.value != null && state."$param.key"?.state in ["notSynced","inProgress"] ) {
            multiStatusEvent("Sync in progress. (param[key: ${param.key}, number:${param.num}, size: ${param.size}])", true)
            state."$param.key"?.state = "inProgress"
            state."$param.key"?.scale = param.scale
            cmds << zwave.configurationV2.configurationGet(parameterNumber: param.num) // Get current value.
			cmds << response(encap(zwave.configurationV2.configurationSet(configurationValue: intToParam(state."$param.key".value, param.size, param.scale), parameterNumber: param.num, size: param.size)))
            cmds << response(encap(zwave.configurationV2.configurationGet(parameterNumber: param.num))) // Confirm new value

            break
        }
    }
    if (cmds) {
    	sendHubCommand(cmds,1000)
        runIn(10, "syncCheck")
    } else {
        runIn(1, "syncCheck")
    }
}

private syncCheck() {
    logging("${device.displayName} - Executing syncCheck()","info")
    def failed = []
    def incorrect = []
    def notSynced = []
    parameterMap().each {
        if (state."$it.key"?.state == "incorrect" ) {
            incorrect << it
        } else if ( state."$it.key"?.state == "failed" ) {
            failed << it
        } else if ( state."$it.key"?.state in ["inProgress","notSynced"] ) {
            logging("$it.key not synced: state is $state.$it.key.$state")
            notSynced << it
        }
    }

    if (failed) {
        logging("${device.displayName} - Sync failed! Check parameter: ${failed[0].num}","info")
        sendEvent(name: "syncStatus", value: "failed")
        multiStatusEvent("Sync failed! Check parameter: ${failed[0].num}", true, true)
    } else if (incorrect) {
        logging("${device.displayName} - Sync mismatch! Check parameter: ${incorrect[0].num}","info")
        sendEvent(name: "syncStatus", value: "incomplete")
        multiStatusEvent("Sync mismatch! Check parameter: ${incorrect[0].num}", true, true)
    } else if (notSynced) {
        logging("${device.displayName} - Sync incomplete!","info")
        sendEvent(name: "syncStatus", value: "incomplete")
        multiStatusEvent("Sync incomplete! Open settings and tap Done to try again.", true, true)
    } else {
        logging("${device.displayName} - Sync Complete","info")
        sendEvent(name: "syncStatus", value: "synced")
        sendEvent(name: "syncPending", value: 0, displayed: false)
        multiStatusEvent("Sync OK.", true, true)
    }
}

private multiStatusEvent(String statusValue, boolean force = false, boolean display = false) {
    if (!device.currentValue("multiStatus")?.contains("Sync") || device.currentValue("multiStatus") == "Sync OK." || force) {
        sendEvent(name: "multiStatus", value: statusValue, descriptionText: statusValue, displayed: display)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {

   	def param = parameterMap().find( {it.num == cmd.parameterNumber } )

    if (param == null) {
    	syncNext()
    } else {

    	def paramKey = param.key

    	logging("${device.displayName} - Parameter ${paramKey} is to be configured with $cmd")

    	def value = state."$paramKey".value as long
	 	def scale = 1 as long
    	if (state."$paramKey".scale)
    	{
    		scale = state."$paramKey".scale
    	}

    	def scaledValue = value * scale as long

    	if (scaledValue == cmd.scaledConfigurationValue) {
    		state."$paramKey".state = "synced"
    	} else {
    		state."$paramKey".state = "incorrect"

        	// just to help identify the expected value
        	logging("${device.displayName} - Parameter ${paramKey} value is ${cmd.scaledConfigurationValue} expected " + scaledValue + ":" + (scaledValue == cmd.scaledConfigurationValue.toLong()) , "debug")
    	}
    	syncNext()

    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// This will capture any commands not handled by other instances of zwaveEvent
	// and is recommended for development so you can see every command the device sends
	log.debug "Catchall reached for cmd: ${cmd.toString()}}"
	return createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
    if (encapsulatedCommand) {
        logging("${device.displayName} - Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract Secure command from $cmd"
    }
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def version = cmdVersions()[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (encapsulatedCommand) {
        logging("${device.displayName} - Parsed Crc16Encap into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract CRC16 command from $cmd"
    }
}

/**
 *  toHexString()
 *
 *  Convert a list of integers to a list of hex strings.
 **/
private toHexString(input, size = 2, usePrefix = false) {

    def pattern = (usePrefix) ? "0x%0${size}X" : "%0${size}X"

    if (input instanceof Collection) {
        def hex  = []
        input.each { hex.add(String.format(pattern, it)) }
        return hex.toString()
    }
    else {
        return String.format(pattern, input)
    }
}


private logging(text, type = "debug") {
    if (settings.logging == "true") {
        log."$type" text
    }
}

private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

private secEncap(physicalgraph.zwave.Command cmd) {
    logging("${device.displayName} - encapsulating command using Secure Encapsulation, command: $cmd", "info")
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
    logging("${device.displayName} - encapsulating command using CRC16 Encapsulation, command: $cmd","info")
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private multiEncap(physicalgraph.zwave.Command cmd, Integer ep) {
    log.info ("${device.displayName} - encapsulating command using MultiChannel Encapsulation, ep: $ep command: $cmd")//,"info")
    zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:ep).encapsulate(cmd)
}

private encap(physicalgraph.zwave.Command cmd, Integer ep) {
    encap(multiEncap(cmd, ep))
}

private encap(List encapList) {
    encap(encapList[0], encapList[1])
}

private encap(Map encapMap) {
    encap(encapMap.cmd, encapMap.ep)
}

private encap(physicalgraph.zwave.Command cmd) {

	cmd.format()
	/*log.debug "zwaveInfo: $zwaveInfo"
    if (zwaveInfo.zw.contains("s")) {
        secEncap(cmd)
    } else if (zwaveInfo.cc.contains("56")){
        crcEncap(cmd)
    } else {
        logging("${device.displayName} - no encapsulation supported for command: $cmd","info")
        cmd.format()
    }*/
}

private encapSequence(cmds, Integer delay=250) {
    delayBetween(cmds.collect{ encap(it) }, delay)
}

private encapSequence(cmds, Integer delay, Integer ep) {
    delayBetween(cmds.collect{ encap(it, ep) }, delay)
}

private List intToParam(Long value, Integer size = 1, Integer scale = 1) {
	value = value * scale
    def result = []
    size.times {
        result = result.plus(0, (value & 0xFF) as Short)
        value = (value >> 8)
    }
    return result
}

private secure(physicalgraph.zwave.Command cmd) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crc16(physicalgraph.zwave.Command cmd) {
    //zwave.crc16encapV1.crc16Encap().encapsulate(cmd).format()
    "5601${cmd.format()}0000"
}

private commands(commands, delay=200) {
    log.info "inside commands: ${commands}"
    delayBetween(commands.collect{ command(it) }, delay)
}

private setSecured() {
    updateDataValue("secured", "true")
}
private isSecured() {
    getDataValue("secured") == "true"
}


private Map cmdVersions() {
    [0x85: 2, 0x59: 1, 0x8E: 2, 0x86: 3, 0x70: 2, 0x72: 2, 0x5E: 2, 0x5A: 1, 0x73: 1, 0x7A: 4, 0x60: 3, 0x20: 1, 0x6C: 1, 0x31: 5, 0x43: 2, 0x40: 2, 0x98: 1, 0x9F: 1, 0x25: 1]
}



private parameterMap() {[
    [key: "sensorMode", num: 2, size: 1, type: "enum", options: [
        0: "F - Floor temperature mode",
        3: "A2-mode, external room sensor mode",
        4: "A2F-mode, external sensor with floor limitation",
    ], def: "0", title: "Sensor Mode",
     descr: "This parameter determines what kind of sensor is used to regulate the power", scale: 1],
    [key: "floorSensorType", num: 3, size: 1, type: "enum", options: [
    	0: "10k ntc",
        1: "12k ntc",
        2: "15k ntc",
        3: "22k ntc",
        4: "33k ntc",
        5: "47k ntc"
    ], def: "0", title: "Floor Sensor Type",
     descr: "This parameter determines floor sensor type, 10k ntc is default", scale: 1],

    [key: "TemperatureControlCysteresis", num: 4, size: 1, type: "number", def:5, min: 3, max: 30, title: "Hysteresis temp (0.3°..3°) - 3-30",
     descr: "This parameter determines the control hysteresis", scale: 1],

    [key: "FLo", num: 5, size: 1, type: "number", def:5, min: 5, max: 40, title: "Minimum floor temperature(5°..40°)",
     descr: "Minimum floor temperature", scale: 10],

    [key: "FHi", num: 6, size: 1, type: "number", def:28, min: 5, max: 40, title: "Maxmum floor temperature (5°..40°)",
     descr: "Maxmum floor temperature", scale: 10],

    [key: "ALo", num: 7, size: 1, type: "number", def:5, min: 5, max: 40, title: "Minimum air temperature (5°..40°)",
     descr: "Minimum air temperature", scale: 10],

    [key: "AHi", num: 8, size: 1, type: "number", def:40, min: 5, max: 40, title: "Maxmum air temperature (5°..40°)",
     descr: "Maxmum air temperature", scale: 10],

   /* [key: "CO", num: 9, size: 1, type: "number", def:21, min: 5, max: 40, title: "Heating mode setpoint (CO)",
     descr: "5.0°C – 40.0°C. Default is 210 (21.0°C)", scale: 10],



    [key: "ECO", num: 10, size: 1, type: "number", def:18, min: 5, max: 40, title: "Energy saving mode setpoint (ECO)",
     descr: "5.0°C – 40.0°C. Default is 180 (18.0°C)", scale: 10],

    */

	  [key: "COOL", num: 11, size: 1, type: "number", def:21, min: 5, max: 40, title: "Cooling temperature (5°..40°)",
     descr: "Cooling temperature", scale: 10],

    [key: "FloorSensorCalibration", num: 12, size: 1, type: "number", def:0, min: -4, max: 4, title: "Floor sensor calibration (-4°..4°)",
     descr: "Floor sensor calibration in deg. C (x10)", scale: 10],

 	  [key: "ExtSensorCalibration", num: 13, size: 1, type: "number", def:0, min: -4, max: 4, title: "External sensor calibration (-4°..4°)",
     descr: "External sensor calibration in deg. C (x10)", scale: 10],

    [key: "tempDisplay", num: 14, size: 1, type: "enum", options: [
    	0: "Display setpoint temperature (Default)",
      1: "Display measured temperature"
    ], def: "0", title: "Temperatur Display",
     descr: "Selects which temperature is shown in the display", scale: 1],

	  [key: "DimBtnBright", num: 15, size: 1, type: "number", def:50, min: 0, max: 100, title: "Button brightness – dimmed state (%)",
     descr: "Configure the brightness of the buttons, in dimmed state", scale: 1],

    [key: "ActBtnBright", num: 16, size: 1, type: "number", def:100, min: 0, max: 100, title: "Button brightness – active state (%)",
     descr: "Configure the brightness of the buttons, in active state", scale: 1],

    [key: "DimDplyBright", num: 17, size: 1, type: "number", def:50, min: 0, max: 100, title: "Display brightness – dimmed state (%)",
     descr: "Configure the brightness of the display, in dimmed state", scale: 1],

    [key: "ActDplyBright", num: 18, size: 1, type: "number", def:100, min: 0, max: 100, title: "Display brightness – active state (%)",
     descr: "Configure the brightness of the display, in active state", scale: 1],

    [key: "TmpReportIntvl", num: 19, size: 2, type: "number", def:60, min: 0, max: 32767, title: "Temperature report interval (seconds)",
     descr: "Time interval between consecutive temperature reports. Temperature reports can be also sent as a result of polling", scale: 1],

    [key: "TempReportHyst", num: 20, size: 1, type: "number", def:10, min: 01, max: 100, title: "Temperature report hysteresis (0.1°..10°)",
     descr: "The temperature report will be sent if there is a difference in temperature value from the previous value reported, defined in this parameter (hysteresis). Temperature reports can be also sent as a result of polling", scale: 10],

    [key: "MeterReportInterval", num: 21, size: 2, type: "number", def:60, min: 0, max: 32767, title: "Meter report interval (seconds)",
     descr: "Time interval between consecutive meter reports. Meter reports can be also sent as a result of polling.", scale: 1],

    [key: "MeterReportDeltaValue", num: 22, size: 1, type: "number", def:10, min: 0, max: 255, title: "Meter report delta value",
     descr: "Delta value in kWh between consecutive meter reports. Meter reports can be also sent as a result of polling.", scale: 1],

]}
