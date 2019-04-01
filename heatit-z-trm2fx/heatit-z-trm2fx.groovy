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
 * V0.0.1 01.04.2019
 *
 *
*/

metadata {
	definition (name: "heatit Z-Trm2fx", namespace: "heatit", author: "JoneLura") {
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
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE")
            {
                attributeState("idle", backgroundColor:"#44b621")
                attributeState("heating", backgroundColor:"#bc2323")
                attributeState("energySaveHeat", backgroundColor:"#ffa81e")
            }
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE")
            {
                attributeState("off", label:'${name}', action:"switchMode", nextState:"heat")
                attributeState("heat", label:'${name}', action:"switchMode", nextState:"energy")
                attributeState("energySaveHeat", label:'${name}', action:"switchMode", nextState:"off")
            }

        }

        valueTile("heatingSetpoint", "device.heatingSetpoint", decoration: "flat", width: 2, height: 2, key: "HEATING_SETPOINT") {
        	state("default", label:'${currentValue}') //, backgroundColor: "#bc2323")
        }

        standardTile("heatingSetpointLabel", "device.heatingSetpoint.label", decoration: "flat", width: 2, height: 1) {
        	state("default", label: "Heat Set Point")
        }

        standardTile("heatingSetPointDown", "device.heatingSetPointDown", decoration: "flat", width: 2, height: 2) {
                state("VALUE_DOWN", action: "pressDown", icon: "st.thermostat.thermostat-down")
        }

        standardTile("heatingSetPointUp", "device.heatingSetPointUp", decoration: "flat", width: 2, height: 2) {
                state("VALUE_UP", action: "pressUp", icon: "st.thermostat.thermostat-up")
        }

        valueTile("ecoHeatingSetpoint", "device.ecoHeatingSetpoint", decoration: "flat", width: 2, height: 2) {
        	state("default", label: '${currentValue}') //, backgroundColor: "#ffa81e")
        }

        standardTile("ecoHeatingSetpointDown", "device.ecoHeatingSetpointDown", decoration: "flat", width: 2, height: 2) {
                state("VALUE_DOWN", action: "ecoPressDown", icon: "st.thermostat.thermostat-down")
        }

        standardTile("ecoHeatingSetpointUp", "device.ecoHeatingSetpointUp", decoration: "flat", width: 2, height: 2) {
                state("VALUE_UP", action: "ecoPressUp", icon: "st.thermostat.thermostat-up")
        }

        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
            state "power", label:'${currentValue}\n W', action:"refresh"
        }

        standardTile("power-icon", "power.icon", decoration: "flat", width: 2, height: 2) {
    		state "default",  icon:"st.switches.switch.off"
        }

        valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
            state "energy", label:'${currentValue}\n kWh', action:"refresh"
        }

		standardTile("refresh", "device.thermostatMode", width:6, height:2, inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
		}

        main "thermostatMulti"
        details(["thermostatMulti", "mode",
        "heatingSetPointUp", "power-icon","ecoHeatingSetpointUp",
        "heatingSetpoint", "power", "ecoHeatingSetpoint",
        "heatingSetPointDown", "energy", "ecoHeatingSetpointDown",
        "refresh"
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
        //    log.debug "power $cmd.scaledMeterValue W"
            break
    }
    multiStatusEvent("${(device.currentValue("power") ?: "0.0")} W | ${(device.currentValue("energy") ?: "0.00")} kWh")
}


def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
    log.debug("Temperature is: $cmd.scaledSensorValue °C")
    sendEvent(name: "temperature", value: cmd.scaledSensorValue)
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
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
			break
	}
	map.name = "thermostatOperatingState"
	map
}


def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	log.debug("Thermostat Mode Report $cmd")
	def map = [:]
	switch (cmd.mode) {
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
            sendEvent(name: "thermostatOperatingState", value: "idle")
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
            sendEvent(name: "thermostatOperatingState", value: "heating")
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
            sendEvent(name: "thermostatOperatingState", value: "energySaveHeat")
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
    log.debug ("${device.displayName} MultiChannelAssociationReport - ${cmd}")//,"info")
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

	setHeatingSetpoint(degrees, 500)
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
	def currTemp = device.latestValue("ecoHeatingSetpoint")
    log.debug(" pressed up currently $currTemp")
    def newTemp = currTemp + 0.5
    log.debug(" pressed up new temp is $newTemp")
	quickSetecoHeat(newTemp)
}

def ecoPressDown(){
	log.debug("pressed Down")
	def currTemp = device.latestValue("ecoHeatingSetpoint")
    def newTemp = currTemp - 0.5
	quickSetecoHeat(newTemp)
}

def quickSetecoHeat(degrees) {

	setecoHeatingSetpoint(degrees, 1000)
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


def poll() {
	def cmds = []
    cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1)
	  cmds << zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 1)
	  cmds << zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 2)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
 //   cmds << zwave.multiChannelAssociationV2.MultiChannelAssociationGroupingsGet()

//    cmds << zwave.configurationV2.configurationGet(parameterNumber: 1)
	cmds << zwave.configurationV2.configurationGet(parameterNumber: 2)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 5)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 6)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9)
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10)
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

def configure()
{
   poll()
}

def modes() {
	["off", "heat", "energySaveHeat"]
}


def switchMode() {

    def currentMode = device.currentState("thermostatMode")?.value
    if (!currentMode)
    {
    	currentMode = "off"
    }
    log.debug "currentMode $currentMode"
 	def cmds = []
   // log.debug("currentMode is $currentMode")
    if (currentMode == "off"){
    	def nextMode = "heat"
        sendEvent(name: "thermostatMode", value: "heat")
        sendEvent(name: "thermostatOperatingState", value: "heating")

    	cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 1)
    	cmds << zwave.thermostatModeV2.thermostatModeGet()
    	encapSequence(cmds, 650)
        poll()
    }
    else if (currentMode == "heat"){
    	def nextMode = "energySaveHeat"
        sendEvent(name: "thermostatMode", value: "energySaveHeat")
        sendEvent(name: "thermostatOperatingState", value: "energySaveHeat")
    	cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 11)
    	cmds << zwave.thermostatModeV2.thermostatModeGet()
    	encapSequence(cmds, 650)
        poll()
    }
    else if (currentMode == "energySaveHeat"){
    	def nextMode = "off"
        sendEvent(name: "thermostatMode", value: "off")
        sendEvent(name: "thermostatOperatingState", value: "idle")
    	cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 11)
    	cmds << zwave.thermostatModeV2.thermostatModeGet()
    	encapSequence(cmds, 650)
        poll()
    }
}

def switchToMode(nextMode) {
	def supportedModes = getDataByName("supportedModes")
	if(supportedModes && !supportedModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
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

def setThermostatMode(String value) {
 	def cmds = []
    cmds << zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value])
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    encapSequence(cmds, standardDelay)
}

def off() {
 	def cmds = []
    cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 0)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    encapSequence(cmds, 650)
	delayBetween([
        sendEvent(name: "thermostatMode", value: "off"),
        sendEvent(name: "thermostatOperatingState", value: "idle"),
        poll()], 650)

}

def heat() {
 	def cmds = []
    cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 1)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    encapSequence(cmds, 650)
    delayBetween([
        sendEvent(name: "thermostatMode", value: "heat"),
        sendEvent(name: "thermostatOperatingState", value: "heating"),
        poll()], 650)
}

def energySaveHeat() {
 	def cmds = []
    cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 11)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    encapSequence(cmds, 650)
    delayBetween([
        sendEvent(name: "thermostatMode", value: "energySaveHeat"),
        sendEvent(name: "thermostatOperatingState", value: "energySaveHeat"),
        poll()], 650)
}

def auto() {
 	def cmds = []
    cmds << zwave.thermostatModeV2.thermostatModeSet(mode: 3)
    cmds << zwave.thermostatModeV2.thermostatModeGet()
    encapSequence(cmds, standardDelay)
}

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
        syncNext()
    }
}

private syncNext() {
    logging("${device.displayName} - Executing syncNext()","info")
    def cmds = []
    for ( param in parameterMap() ) {
        if ( state."$param.key"?.value != null && state."$param.key"?.state in ["notSynced","inProgress"] ) {
            multiStatusEvent("Sync in progress. (param: ${param.num})", true)
            state."$param.key"?.state = "inProgress"
            state."$param.key"?.scale = param.scale
            cmds << response(encap(zwave.configurationV2.configurationSet(configurationValue: intToParam(state."$param.key".value, param.size, param.scale), parameterNumber: param.num, size: param.size)))
            cmds << response(encap(zwave.configurationV2.configurationGet(parameterNumber: param.num)))

            if (param.num == 2)
            {
    			      cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier:3, nodeId:[zwaveHubNodeId])))
                cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier:4, nodeId:[zwaveHubNodeId])))
                cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier:5, nodeId:[zwaveHubNodeId])))
                cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: 3, nodeId:[zwaveHubNodeId])
                cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: 4, nodeId:[zwaveHubNodeId])
                cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: 5, nodeId:[zwaveHubNodeId])

                def sensor = 2 as long // build in sensor
                if (state."$param.key".value == 0 || state."$param.key".value == 5)
                {
                	sensor = 2 // floor sensor
                }
                else if (state."$param.key".value == 3)
                {
                	sensor = 3 // external sensor
                }
                cmds << response(encap(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: sensor, nodeId:[zwaveHubNodeId])))
                cmds << response(encap(zwave.associationV2.associationSet(groupingIdentifier: sensor, nodeId:[zwaveHubNodeId])))

            }
            break
        }
    }
    if (cmds) {
        runIn(10, "syncCheck")
        log.debug "cmds!"
        sendHubCommand(cmds,1000)
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
            logging("$it.key not synced")
            notSynced << it
        }
    }

    log.debug("incorrects: $incorrect")
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
        multiStatusEvent("Sync OK.", true, true)
    }
}

private multiStatusEvent(String statusValue, boolean force = false, boolean display = false) {
    if (!device.currentValue("multiStatus")?.contains("Sync") || device.currentValue("multiStatus") == "Sync OK." || force) {
        sendEvent(name: "multiStatus", value: statusValue, descriptionText: statusValue, displayed: display)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {

    def paramKey = parameterMap().find( {it.num == cmd.parameterNumber } ).key
    def value = state."$paramKey".value as double
    def scale = 1 as long
    if (state."$paramKey".scale)
    {
    	scale = state."$paramKey".scale
    }
    def scaledValue = value * scale as long
    logging("${device.displayName} - Parameter ${paramKey} value is ${cmd.scaledConfigurationValue} expected " + scaledValue , "info")
    state."$paramKey".state = (scaledValue == cmd.scaledConfigurationValue) ? "synced" : "incorrect"
    syncNext()
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


private logging(text, type = "debug") {
    if (settings.logging == "true") {
        log."$type" text
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
    if (zwaveInfo.zw.contains("s")) {
        secEncap(cmd)
    } else if (zwaveInfo.cc.contains("56")){
        crcEncap(cmd)
    } else {
        logging("${device.displayName} - no encapsulation supported for command: $cmd","info")
        cmd.format()
    }
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

def initialize() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	unschedule()
	if (getDataValue("manufacturer") != "Honeywell") {
		runEvery5Minutes("poll")  // This is not necessary for Honeywell Z-wave, but could be for other Z-wave thermostats
	}
	pollDevice()
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

    [key: "TemperatureControlCysteresis", num: 4, size: 2, type: "number", def:5, min: 3, max: 30, title: "Hysteresis temp (0.3°..3°) - 3-30",
     descr: "This parameter determines the control hysteresis", scale: 1],

    [key: "FLo", num: 5, size: 2, type: "number", def:50, min: 50, max: 400, title: "Minimum floor temperature(5°..40°)",
     descr: "Minimum floor temperature", scale: 10],

    [key: "FHi", num: 6, size: 2, type: "number", def:280, min: 50, max: 400, title: "Maxmum floor temperature (5°..40°)",
     descr: "Maxmum floor temperature", scale: 10],

    [key: "ALo", num: 7, size: 2, type: "number", def:50, min: 50, max: 400, title: "Minimum air temperature (5°..40°)",
     descr: "Minimum air temperature", scale: 10],

    [key: "AHi", num: 8, size: 2, type: "number", def:400, min: 50, max: 400, title: "Maxmum air temperature (5°..40°)",
     descr: "Maxmum air temperature", scale: 10],

    [key: "CO", num: 9, size: 1, type: "number", def:210, min: 50, max: 400, title: "Heating mode setpoint (CO)",
     descr: "5.0°C – 40.0°C. Default is 210 (21.0°C)", scale: 10],

    [key: "ECO", num: 10, size: 1, type: "number", def:180, min: 50, max: 400, title: "Energy saving mode setpoint (ECO)",
     descr: "5.0°C – 40.0°C. Default is 180 (18.0°C)", scale: 1],

	  [key: "COOL", num: 11, size: 2, type: "number", def:210, min: 50, max: 400, title: "Cooling temperature (5°..40°)",
     descr: "Cooling temperature", scale: 10],

    [key: "FloorSensorCalibration", num: 12, size: 1, type: "number", def:0, min: -40, max: 40, title: "Floor sensor calibration (-4°..4°)",
     descr: "Floor sensor calibration in deg. C (x10)", scale: 10],

 	  [key: "ExtSensorCalibration", num: 13, size: 1, type: "number", def:0, min: -40, max: 40, title: "External sensor calibration (-4°..4°)",
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

    [key: "TempReportHyst", num: 20, size: 1, type: "number", def:1.0, min: 0.1, max: 10.0, title: "Temperature report hysteresis (0.1°..10°)",
     descr: "The temperature report will be sent if there is a difference in temperature value from the previous value reported, defined in this parameter (hysteresis). Temperature reports can be also sent as a result of polling", scale: 10],

    [key: "MeterReportInterval", num: 21, size: 2, type: "number", def:60, min: 0, max: 32767, title: "Meter report interval (seconds)",
     descr: "Time interval between consecutive meter reports. Meter reports can be also sent as a result of polling.", scale: 1],

    [key: "MeterReportDeltaValue", num: 22, size: 1, type: "number", def:10, min: 0, max: 255, title: "Meter report delta value",
     descr: "Delta value in kWh between consecutive meter reports. Meter reports can be also sent as a result of polling.", scale: 1],

]}
