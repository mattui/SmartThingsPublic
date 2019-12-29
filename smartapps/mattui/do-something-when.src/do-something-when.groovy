definition(
		name: "Do Something When",
		namespace: "mattui",
		author: "Matt Klepac",
		description: "Do something when anything happens in your home.",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	section("When something happens..."){
		input "button", "capability.button", title: "Button Pushed", required: false, multiple: true //tw
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
		input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
		input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
		input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
	}
	section("Only during a certain time"){
		input "starting", "time", title: "Starting", required: false
		input "ending", "time", title: "Ending", required: false
	}
	section("Switches") {
		input "switches_on", "capability.switch", title: "On", multiple: true, required: false
		input "switches_off", "capability.switch", title: "Off", multiple: true, required: false
	}
	section("Locks") {
		input "locks_lock", "capability.lock", title: "Lock", multiple: true, required: false
		input "locks_unlock", "capability.lock", title: "Unlock", multiple: true, required: false
	}
	section("Sonos") {
		input "sonos_play", "capability.musicPlayer", title: "Play", multiple: true, required: false
		input "sonos_stop", "capability.musicPlayer", title: "Stop", multiple: true, required: false
	}
	section("Mode") {
		input "mode", "mode", title: "Set", required: false
	}
	section("Sirens") {
		input "sirens_on","capability.alarm" ,title: "On", multiple: true, required: false
		input "sirens_off", "capability.alarm", title: "Off", multiple: true, required: false
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Minimum time between happenings (optional, defaults to every time)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(button, "button.pushed", eventHandler) //tw
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitch, "windowShade.opening", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(mySwitchOff, "windowShade.closing", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
}

def eventHandler(evt) {
	log.debug "Notify got evt ${evt}"
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			executeHandlers(evt)
		}
	}
	else {
		executeHandlers(evt)
	}
}

private executeHandlers(evt) {
	handleSwitches(evt)
	handleLocks(evt)
	handleMode(evt)
    handleSonos(evt)
	handleSirens(evt)
	sendMessage(evt)
}

private handleSwitches(evt) {
	def turnOn = settings['switches_on']

	if (turnOn != null) {
		turnOn.on()
	}

	def turnOff = settings['switches_off']

	if (turnOff != null) {
		turnOff.off()
	}
}

private handleLocks(evt) {
	def toLock = settings['locks_lock']

	if (toLock != null) {
		toLock.lock()
	}

	def toUnlock = settings['locks_unlock']

	if (toUnlock != null) {
		toUnlock.unlock()
	}
}

private handleMode(evt) {
	def mode = settings['mode']

	if (mode != null) {
		log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

		if (location.mode != mode && location.modes?.find { it.name == mode }) {
			setLocationMode(mode)
		}
	}
}

private handleSonos(evt) {
	def toPlay = settings['sonos_play']
    
    if (toPlay != null) {
    	toPlay.play()
    }
    
    def toStop = settings['sonos_stop']
    
    if (toStop != null) {
    	toStop.stop()
    }
}

private handleSirens(evt) {
	def turnOn = settings['sirens_on']

	if (turnOn != null) {
		turnOn.siren()
	}

	def turnOff = settings['sirens_off']

	if (turnOff != null) {
		turnOff.off()
	}
}

private sendMessage(evt) {
	String msg = messageText
	Map options = [:]

	if (!messageText) {
		msg = defaultText(evt)
		options = [translatable: true, triggerEvent: evt]
	}
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private defaultText(evt) {
	if (evt.name == 'presence') {
		if (evt.value == 'present') {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has arrived at the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has arrived at {{ location.name }}'
			}
		} else {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has left the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has left {{ location.name }}'
			}
		}
	} else {
		'{{ triggerEvent.descriptionText }}'
	}
}

private getIncludeArticle() {
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}