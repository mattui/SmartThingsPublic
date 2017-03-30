definition(
    name: "Dimmer Blocker",
    namespace: "mattui",
    author: "Matt Klepac",
    description: "While in a mode, keep dimmers from going down",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Dimmer to make bright when turned on") {
		input "dimmer", "capability.switchLevel", title: "Which dimmer?"
	input "brightness", "number", title: "Light Level"
	}
}



def installed() {
    	log.debug "Installed with settings: ${settings}"
initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
unsubscribe()
initialize()
}

def initialize() {
    //subscribe(dimmer, "switch.on", onHandler)
    //subscribe(dimmer, "switch.off", onHandler)
    subscribe(dimmer, "switch.setLevel", onHandler)
    //subscribe(dimmer, "switch", onHandler)
}

def onHandler(evt) {
	dimmer.setLevel(brightness)
    log.debug "setting brightness: $brightness"
}