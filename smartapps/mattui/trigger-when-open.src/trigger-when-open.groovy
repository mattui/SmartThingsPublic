definition(
    name: "Trigger When Open",
    namespace: "mattui",
    author: "Matt Klepac",
    description: "Switches modes when you have left a door or window open longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {
	page(name: "selectOpened", title: "What is being monitored?", nextPage: "selectActions", uninstall: true) {
        section("Monitor this door or window") {
            input "contact", "capability.contactSensor"
        }
        section("And trigger if it's open for more than this many minutes (default 10)") {
            input "openThreshold", "number", description: "Number of minutes", required: false
        }
        section("Delay between triggering (default 10 minutes") {
            input "frequency", "number", title: "Number of minutes", description: "", required: false
        }
    }
    page(name: "selectActions")
    page(name: "pageThree", title: "Name app and configure modes", install: true, uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def selectActions() {
    dynamicPage(name: "selectActions", title: "Select Hello Home Action to Execute", nextPage: "pageThree", install: false, uninstall: false) {
        // get the available actions
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
        // sort them alphabetically
            actions.sort()
            section("Hello Home Actions") {
                log.trace actions
                // use the actions as the options for an enum input
                input "action", "enum", title: "Select an action to execute", options: actions
            }
        }
    }
}

def installed() {
	log.trace "installed()"
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	subscribe(contact, "contact.open", doorOpen)
	subscribe(contact, "contact.closed", doorClosed)
}

def doorOpen(evt)
{
	log.trace "doorOpen($evt.name: $evt.value)"
	def t0 = now()
	def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
	runIn(delay, doorOpenTooLong, [overwrite: false])
	log.debug "scheduled doorOpenTooLong in ${now() - t0} msec"
}

def doorClosed(evt)
{
	log.trace "doorClosed($evt.name: $evt.value)"
}

def doorOpenTooLong() {
	def contactState = contact.currentState("contact")
    def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

	if (contactState.value == "open") {
		def elapsed = now() - contactState.rawDateCreated.time
		def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
		if (elapsed >= threshold) {
			log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling switchModes()"
			switchModes()
            runIn(freq, doorOpenTooLong, [overwrite: false])
		} else {
			log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
		}
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}

void switchModes()
{
	def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
	def msg = "${contact.displayName} has been left open for ${minutes} minutes. Switching to ${targetMode}."
	log.info msg
    location.helloHome.execute(routine)
}