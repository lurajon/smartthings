/**
* Fibaro Single Relay (FGS-212)
*
* v.0.0.1
*
*/
metadata {
  definition (name: "Fibaro Single Relay FGS-212", namespace: "JoneLura", author: "Jone Lura", mnmn: "SmartThings", vid:"generic-relay") {
    capability "Switch"
    capability "Button"
    capability "Configuration"
    capability "Health Check"
    capability "Refresh"

    command "reset"

    fingerprint deviceId: "0x1001", inClusters:"0x20, 0x25, 0x27, 0x60, 0x70, 0x72, 0x73, 0x7A, 0x85, 0x86, 0x8E"
  }

  tiles (scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 3, height: 4){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState:"turningOn"
                attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#00a0dc", nextState:"turningOff"
            }
            tileAttribute("device.multiStatus", key:"SECONDARY_CONTROL") {
                attributeState("multiStatus", label:'${currentValue}')
            }
        }



        main(["switch"])
        details(["switch"])
    }

    preferences {
        input (
                title: "Fibaro Single Relay FGS-212 manual",
                description: "Tap to view the manual.",
                image: "http://manuals.fibaro.com/wp-content/uploads/2016/08/switch2_icon.jpg",
                url: "https://manuals.fibaro.com/content/manuals/en/FGS-212/FGS-212-EN-A-v1.1.pdf",
                type: "href",
                element: "href"
        )

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

//UI and tile functions
private getPrefsFor(String name) {
    parameterMap().findAll( {it.key.contains(name)} ).each {
        input (
                name: it.key,
                title: "${it.num}. ${it.title}",
                description: it.descr,
                type: it.type,
                options: it.options,
                range: (it.min != null && it.max != null) ? "${it.min}..${it.max}" : null,
                defaultValue: it.def,
                required: false
        )
    }
}

def on() {
    encap(zwave.basicV1.basicSet(value: 255))
}

def off() {
    encap(zwave.basicV1.basicSet(value: 0))
}

def refresh() {
    def cmds = []
    cmds << zwave.switchBinaryV1.switchBinaryGet()
    encapSequence(cmds,1000)
}

def ping() {
    log.debug "ping()"
    refresh()
}

def installed(){
    log.debug "installed()"
    sendEvent(name: "checkInterval", value: 1920, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    response(refresh())
}

//Configuration and synchronization
def updated() {
    if ( state.lastUpdated && (now() - state.lastUpdated) < 500 ) return
    logging("Executing updated()","info")

    state.lastUpdated = now()
    syncStart()
}

private syncStart() {
    boolean syncNeeded = false
    Integer settingValue = null
    parameterMap().each {
        if(settings."$it.key" != null) {
            settingValue = settings."$it.key" as Integer
            if (state."$it.key" == null) { state."$it.key" = [value: null, state: "synced"] }
            if (state."$it.key".value != settingValue || state."$it.key".state != "synced" ) {
                state."$it.key".value = settingValue
                state."$it.key".state = "notSynced"
                syncNeeded = true
            }
        }
    }
    if ( syncNeeded ) {
        logging("sync needed.", "info")
        syncNext()
    }
}

private syncNext() {
    logging("Executing syncNext()","info")
    def cmds = []
    for ( param in parameterMap() ) {
        if ( state."$param.key"?.value != null && state."$param.key"?.state in ["notSynced","inProgress"] ) {
            multiStatusEvent("Sync in progress. (param: ${param.num})", true)
            state."$param.key"?.state = "inProgress"
            cmds << response(encap(zwave.configurationV2.configurationSet(configurationValue: intToParam(state."$param.key".value, param.size), parameterNumber: param.num, size: param.size)))
            cmds << response(encap(zwave.configurationV2.configurationGet(parameterNumber: param.num)))
            break
        }
    }
    if (cmds) {
        runIn(10, "syncCheck")
        sendHubCommand(cmds,1000)
    } else {
        runIn(1, "syncCheck")
    }
}

def syncCheck() {
    logging("Executing syncCheck()","info")
    def failed = []
    def incorrect = []
    def notSynced = []
    parameterMap().each {
        if (state."$it.key"?.state == "incorrect" ) {
            incorrect << it
        } else if ( state."$it.key"?.state == "failed" ) {
            failed << it
        } else if ( state."$it.key"?.state in ["inProgress","notSynced"] ) {
            notSynced << it
        }
    }

    if (failed) {
        multiStatusEvent("Sync failed! Verify parameter: ${failed[0].num}", true, true)
    } else if (incorrect) {
        multiStatusEvent("Sync mismatch! Verify parameter: ${incorrect[0].num}", true, true)
    } else if (notSynced) {
        multiStatusEvent("Sync incomplete! Open settings and tap Done to try again.", true, true)
    } else {
        if (device.currentValue("multiStatus")?.contains("Sync")) { multiStatusEvent("Sync OK.", true, true) }
    }
}

private multiStatusEvent(String statusValue, boolean force = false, boolean display = false) {
    if (!device.currentValue("multiStatus")?.contains("Sync") || device.currentValue("multiStatus") == "Sync OK." || force) {
        sendEvent(name: "multiStatus", value: statusValue, descriptionText: statusValue, displayed: display)
    }
}

//event handlers related to configuration and sync
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    def paramKey = parameterMap().find( {it.num == cmd.parameterNumber } ).key
    logging("Parameter ${paramKey} value is ${cmd.scaledConfigurationValue} expected " + state."$paramKey".value, "info")
    state."$paramKey".state = (state."$paramKey".value == cmd.scaledConfigurationValue) ? "synced" : "incorrect"
    syncNext()
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
    logging("rejected request!","warn")
    for ( param in parameterMap() ) {
        if ( state."$param.key"?.state == "inProgress" ) {
            state."$param.key"?.state = "failed"
            break
        }
    }
}

//event handlers
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    //ignore
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    logging("SwitchBinaryReport received, value: ${cmd.value} ","info")
    sendEvent([name: "switch", value: (cmd.value == 0 ) ? "off": "on"])
}


/*
####################
## Z-Wave Toolkit ##
####################
*/
def parse(String description) {
    def result = []
    logging("Parsing: ${description}")
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
            logging("Parsed: ${cmd}")
            zwaveEvent(cmd)
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
    if (encapsulatedCommand) {
        logging("Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        logging("Unable to extract Secure command from $cmd","warn")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def version = cmdVersions()[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (encapsulatedCommand) {
        logging("Parsed Crc16Encap into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        logging("Unable to extract CRC16 command from $cmd","warn")
    }
}


private logging(text, type = "debug") {
    if (settings.logging == "true" || type == "warn") {
        log."$type" "${device.displayName} - $text"
    }
}

private secEncap(physicalgraph.zwave.Command cmd) {
    logging("encapsulating command using Secure Encapsulation, command: $cmd","info")
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
    logging("encapsulating command using CRC16 Encapsulation, command: $cmd","info")
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
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
        logging("no encapsulation supported for command: $cmd","info")
        cmd.format()
    }
}

private encapSequence(cmds, Integer delay=250) {
    delayBetween(cmds.collect{ encap(it) }, delay)
}

private encapSequence(cmds, Integer delay, Integer ep) {
    delayBetween(cmds.collect{ encap(it, ep) }, delay)
}

private List intToParam(Long value, Integer size = 1) {
    def result = []
    size.times {
        result = result.plus(0, (value & 0xFF) as Short)
        value = (value >> 8)
    }
    return result
}
/*
##########################
## Device Configuration ##
##########################
*/
private Map cmdVersions() {
    [0x25: 1, 0x27: 1,0x20: 1,0x60: 3, 0x70: 2, 0x72: 1, 0x73: 1, 0x7A: 1, 0x85: 2,0x86: 1, 0x8E: 2]
}

private parameterMap() {[
        [key: "allOnAllOff", num: 1, size: 1, type: "enum", options: [
           255 : "ALL ON active, ALL OFF active.",
           0 : "ALL ON is not active ALL OFF is not active",
           1 : "ALL ON is not active ALL OFF active",
           2 : "ALL ON active ALL OFF is not active"
        ], def: "255", title: "Activate / deactivate functions ALL ON / ALL OFF. D",
          desc: ""],
        [key: "autoOffWithOverride", size: 1, num: 3, type: "enum", options: [
          0  :  "manual  override  disabled.  After  single  button  push  the  relay  turns on and automatically turns off after specified time",
          1  :  "manual  override  enabled.  After  single  button  push  the  relay  turns  on  and  automatically  turns  off  after  specified  time.  Another  button push turns the relay off immediately"
        ], def: "0", title: "Auto off relay after specified time, with the possibility  of  manual  override",
          desc: "Auto off relay after specified time, with the possibility  of  manual  override  -  immediate  Off  after  button  push."],
        [key: "autoOff", size: 1, num: 4, type: "Integer", def: 0, title: "Auto off", desc: "(0,1 s – 6553,5 s) Time period for auto off, in miliseconds. 0 - Auto off disabled"],
        [key: "commandsToControl", size: 1, num: 6, type: "enum", options: [
          0 : "commands are sent when device is turned on and off",
          1 : "commands are sent when device is turned off. Enabling device does not send control commands. Double-clicking key sends 'turn on'  command,  dimmers  memorize  the  last  saved  state  (e.g.  50%  brightness)",
          2 : "commands are sent when device is turned off"
        ], def: 0, title: "Sending commands to control", desc: "Sending  commands  to  control    devices  assigned to 1-st association group (key no. 1)."],
        [key: "bistableKey", num: 13, size: 1, type: "enum", options: [
                0: "[On / Off] device changes status on key status change",
                1: "Device status depends on key status: ON when the key is ON, OFF when the key is OFF"
        ], def: "0", title: "Assigns  bistable  key  status  to  the  device  status",
         descr: "Assigns  bistable  key  status  to  the  device  statusl"],
        [key: "switchType", num: 14, size: 1, type: "enum", options: [
                0: "momentary switch",
                1: "toggle switch"
        ], def: "1", title: "Switch type",
         descr: "Parameter defines as what type the device should treat the switch connected to the S1 terminal"],
        [key: "restoreState", num: 16, size: 1, type: "enum", options: [
                0: "power off after power failure",
                1: "restore state"
        ], def: "1", title: "Restore state after power failure",
         descr: "This parameter determines if the device will return to state prior to the power failure after power is restored"],

]}
