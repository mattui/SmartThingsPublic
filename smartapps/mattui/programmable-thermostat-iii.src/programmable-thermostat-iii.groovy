definition(
	name: "Programmable Thermostat III",
	namespace: "mattui",
	author: "Matt Klepac",
	description: "Weekday and Weekend Thermostat",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png")

preferences {
	page(name: "pageOne", title: "Which thermostat(s) should be controlled?", nextPage: "pageTwo", uninstall: true) {
        section("Choose thermostat(s)") {
            input(name: "thermostats", type: "capability.thermostat", required: true, multiple:true)
        }

        section("(Optional) Is active when...", hideable: true, hidden: contact) {
            input(name: "contact", type: "capability.contactSensor", title: "Door or window")
            input(name: "sensorAction", type: "enum", title: "is", options: ["open", "closed"], required: true, defaultValue: "open")
        }
    }
    
    page(name: "weekdayPage")
    
    page(name: "weekendPage")
}
    
def weekdayPage() {
	dynamicPage(name: "weekdayPage", title: "Monday to Friday Schedule", nextPage: "weekendPage") {
        def actions = location.helloHome?.getPhrases()*.label.sort()
        actions.each { action ->
        	section("${action}") {
                input(name: "weekday${action}Heat", type: "number", title: "Heat Temp", required: false)
                input(name: "weekday${action}Cool", type: "number", title: "Cool Temp", required: false)
            }
		}
	}
}

def weekendPage() {
	dynamicPage(name: "weekendPage", title: "Saturday and Sunday Schedule", install: true, uninstall: true) {
        def actions = location.helloHome?.getPhrases()*.label.sort()
        actions.each { action ->
        	section("${action}") {
                input(name: "weekend${action}Heat", type: "number", title: "Heat Temp", required: false)
                input(name: "weekend${action}Cool", type: "number", title: "Cool Temp", required: false)
            }
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize();
}

def updated(settings) {
	unsubscribe()
	unschedule()
    initialize()
}

def initialize() {
	subscribe(location, "mode", temperatureHandler)

	if (contact) {
        subscribe(contact, "contact", temperatureHandler)
	}
    
    evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

def scheduleCallback()
{
	evaluate()
}

def isWeekday() {
	def calendar = Calendar.getInstance()
    def dow = calendar.get(Calendar.DAY_OF_WEEK)
    return ((dow >= Calendar.MONDAY) && (dow <= Calendar.FRIDAY))
}

private evaluate() {
	if (isWeekday()) {
        if (timeOfDayIsBetween(weekdayWakeTime, weekdayLeaveTime, new Date(), location.timeZone)) {
            setTemp(weekdayWakeHeat, weekdayWakeCool)
        } else if (timeOfDayIsBetween(weekdayLeaveTime, weekdayReturnTime, new Date(), location.timeZone)) {
            setTemp(weekdayLeaveHeat, weekdayLeaveCool)
        } else if (timeOfDayIsBetween(weekdayReturnTime, weekdaySleepTime, new Date(), location.timeZone)) {
            setTemp(weekdayReturnHeat, weekdayReturnCool)
        } else {
            setTemp(weekdaySleepHeat, weekdaySleepCool)
        }
    } else {
    	if (timeOfDayIsBetween(weekendWakeTime, weekendLeaveTime, new Date(), location.timezone)) {
        	setTemp(weekendWakeHeat, weekendWakeCool)
        } else if (timeOfDayIsBetween(weekendLeaveTime, weekendReturnTime, new Date(), location.timezone)) {
        	setTemp(weekendLeaveHeat, weekendLeaveCool)
        } else if (timeOfDayIsBetween(weekendReturnTime, weekendSleepTime, new Date(), location.timezone)) {
        	setTemp(weekendReturnHeat, weekendReturnCool)
        } else {
        	setTemp(weekendSleepHeat, weekendSleepCool)
        }
    }
}

def setTemp(heat, cool) {
	if (!contact || contact.currentState("contact").value == sensorAction) {
        thermostats.each { thermostat ->
            def thermostatState = thermostat.currentThermostatMode
            log.debug "Thermostat mode = $thermostatState"
            def thermostatFan = thermostat.currentThermostatFanMode
            log.debug "Thermostat fan = $thermostatFan"
            if (thermostatState == "auto") {
                thermostat.setHeatingSetpoint(heat)
                thermostat.setCoolingSetpoint(cool)
                log.info "Set $thermostat Heat $heat°, Cool $cool°"
            }
            else if (thermostatState == "heat") {
                thermostat.setHeatingSetpoint(heat)
                log.info "Set $thermostat Heat $heat°"
            }
            else {
                thermostat.setCoolingSetpoint(cool)
                log.info "Set $thermostat Cool $cool°"
            }
        }
    }
}