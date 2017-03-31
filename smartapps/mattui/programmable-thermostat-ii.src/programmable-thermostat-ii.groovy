definition(
	name: "Programmable Thermostat II",
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

	page(name: "pageTwo", title: "Monday to Friday Schedule", nextPage: "pageThree") {
    	section("When I Wakeup") {
            input(name: "weekdayWakeTime", type: "time", title: "Wake Time", required: true)
            input(name: "weekdayWakeHeat", type: "number", title: "Wake Heat Temp", required: true)
            input(name: "weekdayWakeCool", type: "number", title: "Wake Cool Temp", required: true)
        }
        
    	section("When I Leave") {
            input(name: "weekdayLeaveTime", type: "time", title: "Leave Time", required: true)
            input(name: "weekdayLeaveHeat", type: "number", title: "Leave Heat Temp", required: true)
            input(name: "weekdayLeaveCool", type: "number", title: "Leave Cool Temp", required: true)
        }
        
    	section("When I Return") {
            input(name: "weekdayReturnTime", type: "time", title: "Return Time", required: true)
            input(name: "weekdayReturnHeat", type: "number", title: "Return Heat Temp", required: true)
            input(name: "weekdayReturnCool", type: "number", title: "Return Cool Temp", required: true)
        }
        
    	section("When I Sleep") {
            input(name: "weekdaySleepTime", type: "time", title: "Sleep Time", required: true)
            input(name: "weekdaySleepHeat", type: "number", title: "Sleep Heat Temp", required: true)
            input(name: "weekdaySleepCool", type: "number", title: "Sleep Cool Temp", required: true)
        }
	}
    
	page(name: "pageThree", title: "Saturday and Sunday Schedule", install: true, uninstall: true) {
    	section("When I Wakeup") {
            input(name: "weekendWakeTime", type: "time", title: "Wake Time", required: true)
            input(name: "weekendWakeHeat", type: "number", title: "Wake Heat Temp", required: true)
            input(name: "weekendWakeCool", type: "number", title: "Wake Cool Temp", required: true)
        }
        
    	section("When I Leave") {
            input(name: "weekendLeaveTime", type: "time", title: "Leave Time", required: true)
            input(name: "weekendLeaveHeat", type: "number", title: "Leave Heat Temp", required: true)
            input(name: "weekendLeaveCool", type: "number", title: "Leave Cool Temp", required: true)
        }
        
    	section("When I Return") {
            input(name: "weekendReturnTime", type: "time", title: "Return Time", required: true)
            input(name: "weekendReturnHeat", type: "number", title: "Return Heat Temp", required: true)
            input(name: "weekendReturnCool", type: "number", title: "Return Cool Temp", required: true)
        }
        
    	section("When I Sleep") {
            input(name: "weekendSleepTime", type: "time", title: "Sleep Time", required: true)
            input(name: "weekendSleepHeat", type: "number", title: "Sleep Heat Temp", required: true)
            input(name: "weekendSleepCool", type: "number", title: "Sleep Cool Temp", required: true)
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
	schedule(weekdayWakeTime, "scheduleCallback")
	schedule(weekdayLeaveTime, "scheduleCallback")
	schedule(weekdayReturnTime, "scheduleCallback")
	schedule(weekdaySleepTime, "scheduleCallback")
	schedule(weekendWakeTime, "scheduleCallback")
	schedule(weekendLeaveTime, "scheduleCallback")
	schedule(weekendReturnTime, "scheduleCallback")
	schedule(weekendSleepTime, "scheduleCallback")
    
    subscribe(location, changedLocationMode)
	if (contact) {
        subscribe(contact, "contact", temperatureHandler)
	}
    
    evaluate()
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
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