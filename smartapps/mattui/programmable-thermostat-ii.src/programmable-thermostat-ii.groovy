definition(
	name: "Programmable Thermostat II",
	namespace: "mattui",
	author: "Matt Klepac",
	description: "Weekday and Weekend Thermostat",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png")

preferences {
	section("Choose thermostat (s)") {
		input(name: "thermostats", type: "capability.thermostat", required: true, multiple:true)
	}

	section("Monday to Friday Schedule") {
		input(name: "weekdayWakeTime", type: "time", title: "Wake Time", required: true)
		input(name: "weekdayWakeHeat", type: "number", title: "Wake Heat Temp", required: true)
		input(name: "weekdayWakeCool", type: "number", title: "Wake Cool Temp", required: true)
		input(name: "weekdayLeaveTime", type: "time", title: "Leave Time", required: true)
		input(name: "weekdayLeaveHeat", type: "number", title: "Leave Heat Temp", required: true)
		input(name: "weekdayLeaveCool", type: "number", title: "Leave Cool Temp", required: true)
		input(name: "weekdayReturnTime", type: "time", title: "Return Time", required: true)
		input(name: "weekdayReturnHeat", type: "number", title: "Return Heat Temp", required: true)
		input(name: "weekdayReturnCool", type: "number", title: "Return Cool Temp", required: true)
		input(name: "weekdaySleepTime", type: "time", title: "Sleep Time", required: true)
		input(name: "weekdaySleepHeat", type: "number", title: "Sleep Heat Temp", required: true)
		input(name: "weekdaySleepCool", type: "number", title: "Sleep Cool Temp", required: true)
	}
	section("Saturday and Sunday Schedule") {
		input(name: "weekendWakeTime", type: "time", title: "Wake Time", required: true)
		input(name: "weekendWakeHeat", type: "number", title: "Wake Heat Temp", required: true)
		input(name: "weekendWakeCool", type: "number", title: "Wake Cool Temp", required: true)
		input(name: "weekendLeaveTime", type: "time", title: "Leave Time", required: true)
		input(name: "weekendLeaveHeat", type: "number", title: "Leave Heat Temp", required: true)
		input(name: "weekendLeaveCool", type: "number", title: "Leave Cool Temp", required: true)
		input(name: "weekendReturnTime", type: "time", title: "Return Time", required: true)
		input(name: "weekendReturnHeat", type: "number", title: "Return Heat Temp", required: true)
		input(name: "weekendReturnCool", type: "number", title: "Return Cool Temp", required: true)
		input(name: "weekendSleepTime", type: "time", title: "Sleep Time", required: true)
		input(name: "weekendSleepHeat", type: "number", title: "Sleep Heat Temp", required: true)
		input(name: "weekendSleepCool", type: "number", title: "Sleep Cool Temp", required: true)
	}
    
    section("(Optional) Is active when...", hideable: true, hidden: true) {
		input(name: "contact", type: "capability.contactSensor", title: "Door or window")
        input(name: "sensorAction", type: "enum", title: "is", options: ["open", "closed"], required: true, defaultValue: "open")
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
	schedule(weekdayWakeTime, "evaluate")
	schedule(weekdayLeaveTime, "evaluate")
	schedule(weekdayReturnTime, "evaluate")
	schedule(weekdaySleepTime, "evaluate")
	schedule(weekendWakeTime, "evaluate")
	schedule(weekendLeaveTime, "evaluate")
	schedule(weekendReturnTime, "evaluate")
	schedule(weekendSleepTime, "evaluate")
    
    subscribe(location, changedLocationMode)
	if (contact) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
        subscribe(contact, "contact", temperatureHandler)
	}
    
    evaluate()
}

def isWeekday() {
	def calendar = Calendar.getInstance()
    def dow = calendar.get(Calendar.DAY_OF_WEEK)
    return ((dow >= Calendar.MONDAY) && (dow <= Calendar.FRIDAY))
}

def evaluate() {
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